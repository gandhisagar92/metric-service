import asyncio
import json
import os
import tempfile
from typing import Any, Dict, Optional


class FileBackedGraphStore:
    def __init__(self, data_dir: str) -> None:
        self.data_dir = data_dir
        os.makedirs(self.data_dir, exist_ok=True)
        self.schema_path = os.path.join(self.data_dir, "schema.json")
        self.nodes_path = os.path.join(self.data_dir, "nodes.json")
        self.edges_path = os.path.join(self.data_dir, "edges.json")
        # In-process locks for basic safety
        self._locks: Dict[str, asyncio.Lock] = {
            self.schema_path: asyncio.Lock(),
            self.nodes_path: asyncio.Lock(),
            self.edges_path: asyncio.Lock(),
        }

    # ---------------- Private helpers -----------------
    async def _read_json(self, path: str, default: Any) -> Any:
        async with self._locks[path]:
            if not os.path.exists(path):
                return default
            return await asyncio.to_thread(self._read_json_sync, path, default)

    def _read_json_sync(self, path: str, default: Any) -> Any:
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
        except FileNotFoundError:
            return default
        except json.JSONDecodeError:
            # Corrupt file fallback
            return default

    async def _write_json(self, path: str, data: Any) -> None:
        async with self._locks[path]:
            await asyncio.to_thread(self._write_json_sync, path, data)

    def _write_json_sync(self, path: str, data: Any) -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fd, tmp_path = tempfile.mkstemp(prefix=".tmp_", dir=os.path.dirname(path))
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            os.replace(tmp_path, path)
        finally:
            try:
                if os.path.exists(tmp_path):
                    os.remove(tmp_path)
            except Exception:
                pass

    # ---------------- Schema -----------------
    async def load_schema(self) -> Dict[str, Any]:
        data = await self._read_json(self.schema_path, {})
        if not isinstance(data, dict):
            data = {}
        data.setdefault("nodeTypes", {})
        data.setdefault("relationshipTypes", {})
        return data

    async def save_schema(self, schema: Dict[str, Any]) -> None:
        to_save = {
            "nodeTypes": schema.get("nodeTypes", {}),
            "relationshipTypes": schema.get("relationshipTypes", {}),
        }
        await self._write_json(self.schema_path, to_save)

    # ---------------- Nodes -----------------
    async def load_nodes(self) -> Dict[str, Dict[str, Any]]:
        data = await self._read_json(self.nodes_path, {})
        return data if isinstance(data, dict) else {}

    async def save_nodes(self, nodes: Dict[str, Dict[str, Any]]) -> None:
        await self._write_json(self.nodes_path, nodes)

    async def get_node(self, node_id: str) -> Optional[Dict[str, Any]]:
        nodes = await self.load_nodes()
        return nodes.get(node_id)

    async def create_node(self, node: Dict[str, Any]) -> Dict[str, Any]:
        nodes = await self.load_nodes()
        nodes[node["id"]] = node
        await self.save_nodes(nodes)
        return node

    async def update_node(self, node_id: str, node: Dict[str, Any]) -> Dict[str, Any]:
        nodes = await self.load_nodes()
        nodes[node_id] = node
        await self.save_nodes(nodes)
        return node

    async def delete_node(self, node_id: str, cascade: bool = False) -> Dict[str, Any]:
        nodes = await self.load_nodes()
        if node_id not in nodes:
            return {"ok": False, "notFound": True}
        edges = await self.load_edges()
        incident = [e_id for e_id, e in edges.items() if e.get("source") == node_id or e.get("target") == node_id]
        if incident and not cascade:
            return {"ok": False, "conflict": True, "incidentEdges": len(incident)}
        # Delete node
        del nodes[node_id]
        await self.save_nodes(nodes)
        deleted_edges = 0
        if incident:
            for e_id in incident:
                del edges[e_id]
            deleted_edges = len(incident)
            await self.save_edges(edges)
        return {"ok": True, "deletedIncidentEdges": deleted_edges}

    # ---------------- Edges -----------------
    async def load_edges(self) -> Dict[str, Dict[str, Any]]:
        data = await self._read_json(self.edges_path, {})
        return data if isinstance(data, dict) else {}

    async def save_edges(self, edges: Dict[str, Dict[str, Any]]) -> None:
        await self._write_json(self.edges_path, edges)

    async def get_edge(self, edge_id: str) -> Optional[Dict[str, Any]]:
        edges = await self.load_edges()
        return edges.get(edge_id)

    async def create_edge(self, edge: Dict[str, Any]) -> Dict[str, Any]:
        edges = await self.load_edges()
        edges[edge["id"]] = edge
        await self.save_edges(edges)
        return edge

    async def update_edge(self, edge_id: str, edge: Dict[str, Any]) -> Dict[str, Any]:
        edges = await self.load_edges()
        edges[edge_id] = edge
        await self.save_edges(edges)
        return edge

    async def delete_edge(self, edge_id: str) -> bool:
        edges = await self.load_edges()
        if edge_id not in edges:
            return False
        del edges[edge_id]
        await self.save_edges(edges)
        return True