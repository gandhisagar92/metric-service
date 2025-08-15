from typing import Any, Dict, List, Tuple
from collections import defaultdict

from ..storage import FileBackedGraphStore
from ..utils.cursors import encode_cursor, decode_cursor
from ..utils.filters import matches_filters
from ..utils.facets import compute_facets
from .query_mapping_service import QueryMappingService


class GraphService:
	def __init__(self, store: FileBackedGraphStore, mapping: QueryMappingService | None = None) -> None:
		self.store = store
		self.mapping = mapping or QueryMappingService(store.data_dir)  # type: ignore

	async def search(self, request: Dict[str, Any]) -> Dict[str, Any]:
		# request contains: referenceDataType, queryBy, inputs, graphOptions
		ref_type = request.get("referenceDataType")
		query_by = request.get("queryBy")
		inputs = request.get("inputs", {})
		options = request.get("graphOptions", {})
		max_neighbors = int(options.get("maxNeighborsPerType", 50))
		include_facets = bool(options.get("includeFacets", True))

		nodes = await self.store.load_nodes()
		edges = await self.store.load_edges()

		# Find initial nodes matching query
		initial_nodes = [n for n in nodes.values() if n.get("type") == ref_type and self._node_matches_query(ref_type, n, query_by, inputs)]
		if not initial_nodes:
			return {"graph": {"nodes": [], "edges": [], "adjacency": {}}, "facets": {}}

		# For simplicity return graph around the first matching node
		root = initial_nodes[0]
		result_nodes: Dict[str, Dict[str, Any]] = {root["id"]: self._shape_node(root, edges)}
		result_edges: Dict[str, Dict[str, Any]] = {}
		adjacency: Dict[str, Any] = {}

		# Build 1-hop neighborhood with degree limit per relation type
		rel_to_neighbors: Dict[str, List[Tuple[str, Dict[str, Any]]]] = defaultdict(list)
		for e in edges.values():
			if e.get("source") == root["id"] or e.get("target") == root["id"]:
				neighbor_id = e.get("target") if e.get("source") == root["id"] else e.get("source")
				rel_to_neighbors[e.get("type")].append((neighbor_id, e))

		for rel_type, pairs in rel_to_neighbors.items():
			counts_total = len(pairs)
			page = pairs[:max_neighbors]
			for neighbor_id, e in page:
				neighbor = nodes.get(neighbor_id)
				if not neighbor:
					continue
				result_nodes[neighbor_id] = self._shape_node(neighbor, edges)
				result_edges[e["id"]] = self._shape_edge(e)
			adjacency[root["id"]] = adjacency.get(root["id"], {"counts": {}, "pageInfo": {}})
			adjacency[root["id"]]["counts"][rel_type] = counts_total
			if counts_total > max_neighbors:
				cursor = encode_cursor({"relation": rel_type, "offset": max_neighbors})
				adjacency[root["id"]]["pageInfo"][rel_type] = {"total": counts_total, "pageSize": max_neighbors, "cursor": cursor}

		facets: Dict[str, Any] = {}
		if include_facets:
			# Compute simple facets for each relation using neighbor nodes
			for rel_type, pairs in rel_to_neighbors.items():
				child_nodes = [nodes[nid] for nid, _ in pairs if nid in nodes]
				if not child_nodes:
					continue
				facet_fields = ["attributes.optionType", "attributes.expirationDate"]
				facets[rel_type] = compute_facets(rel_type, child_nodes, facet_fields)

		return {"graph": {"nodes": list(result_nodes.values()), "edges": list(result_edges.values()), "adjacency": adjacency}, "facets": facets}

	async def expand(self, request: Dict[str, Any]) -> Dict[str, Any]:
		node_id = request.get("nodeId")
		relations = request.get("relations") or []
		filters = request.get("filters") or []
		pagination = request.get("pagination") or {}
		sort = request.get("sort") or {}
		limit = int((pagination.get("limit") or 50))
		cursor = pagination.get("cursor")
		start_offset = 0
		if cursor:
			parsed = decode_cursor(cursor)
			start_offset = int(parsed.get("offset") or 0)

		nodes = await self.store.load_nodes()
		edges = await self.store.load_edges()
		center = nodes.get(node_id)
		if not center:
			return {"nodes": [], "edges": [], "facets": {}, "pageInfo": {}}

		# Collect matching edges
		candidate_edges: List[Dict[str, Any]] = []
		for e in edges.values():
			if e.get("source") == node_id or e.get("target") == node_id:
				if relations and e.get("type") not in relations:
					continue
				candidate_edges.append(e)

		# Apply edge filters (scope=edge)
		edge_filters = [f for f in filters if (f.get("scope") or "node").lower() == "edge"]
		if edge_filters:
			candidate_edges = [e for e in candidate_edges if matches_filters({"type": e.get("type"), "attributes": e.get("attributes", {})}, [self._normalize_field(f) for f in edge_filters])]

		# Derive neighbor nodes and apply node filters
		neighbor_nodes: List[Dict[str, Any]] = []
		for e in candidate_edges:
			neighbor_id = e.get("target") if e.get("source") == node_id else e.get("source")
			n = nodes.get(neighbor_id)
			if not n:
				continue
			neighbor_nodes.append(n)
		node_filters = [f for f in filters if (f.get("scope") or "node").lower() == "node"]
		if node_filters:
			neighbor_nodes = [n for n in neighbor_nodes if matches_filters({"id": n.get("id"), "type": n.get("type"), "label": n.get("label"), "attributes": n.get("attributes", {})}, [self._normalize_field(f) for f in node_filters])]

		# Sorting (only on attributes.* or label)
		field = sort.get("field")
		direction = (sort.get("direction") or "asc").lower()
		if field:
			reverse = direction == "desc"
			neighbor_nodes.sort(key=lambda n: self._get_by_path({"label": n.get("label"), "attributes": n.get("attributes", {})}, field), reverse=reverse)

		# Pagination
		page_nodes = neighbor_nodes[start_offset:start_offset + limit]
		page_edges: List[Dict[str, Any]] = []
		neighbor_ids = {n["id"] for n in page_nodes}
		for e in candidate_edges:
			if e.get("source") in neighbor_ids or e.get("target") in neighbor_ids:
				page_edges.append(e)

		# Compute facets under current filters for the first requested relation, if any
		facets = {}
		if relations:
			facets = compute_facets(relations[0], neighbor_nodes, ["attributes.optionType", "attributes.expirationDate"])

		new_offset = start_offset + len(page_nodes)
		page_info = {}
		for rel in relations or [None]:
			page_info[rel or "ALL"] = {"total": len(neighbor_nodes), "returned": len(page_nodes), "cursor": encode_cursor({"offset": new_offset}) if new_offset < len(neighbor_nodes) else None}

		return {
			"nodes": [self._shape_node(n, edges) for n in page_nodes],
			"edges": [self._shape_edge(e) for e in page_edges],
			"facets": facets,
			"pageInfo": page_info,
		}

	def _node_matches_query(self, ref_type: str, node: Dict[str, Any], query_by: str, inputs: Dict[str, Any]) -> bool:
		attrs = node.get("attributes", {})
		if query_by == "Economics":
			# Economics: check all mapped fields equality
			paths = self.mapping.get_economics_paths(ref_type)
			for key, path in paths.items():
				val = inputs.get(key)
				if val is None:
					continue
				if self._get_by_path({"attributes": attrs}, path) != val:
					return False
			return True
		paths = self.mapping.get_paths(ref_type, query_by)
		if not paths:
			return False
		v = inputs.get("value")
		if v is None:
			return False
		return any(self._get_by_path({"attributes": attrs}, p) == v for p in paths)

	def _shape_node(self, node: Dict[str, Any], edges: Dict[str, Any]) -> Dict[str, Any]:
		degree = 0
		for e in edges.values():
			if e.get("source") == node["id"] or e.get("target") == node["id"]:
				degree += 1
		return {
			"id": node["id"],
			"type": node.get("type"),
			"label": node.get("label"),
			"attributes": node.get("attributes", {}),
			"meta": {"degree": degree},
		}

	def _shape_edge(self, edge: Dict[str, Any]) -> Dict[str, Any]:
		return {
			"id": edge["id"],
			"type": edge.get("type"),
			"label": edge.get("type"),
			"source": edge.get("source"),
			"target": edge.get("target"),
			"attributes": edge.get("attributes", {}),
		}

	def _normalize_field(self, f: Dict[str, Any]) -> Dict[str, Any]:
		return {"field": f.get("field"), "op": f.get("op", "EQ"), "value": f.get("value")}

	def _get_by_path(self, obj: Dict[str, Any], path: str) -> Any:
		cur: Any = obj
		for p in path.split("."):
			if isinstance(cur, dict):
				cur = cur.get(p)
			else:
				return None
		return cur