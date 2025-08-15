import json
import os
import re
import uuid
from typing import Any, Dict, List, Optional, Tuple

import tornado.ioloop
import tornado.web

from .storage import FileBackedGraphStore
from .validators import validate_attributes


def json_response(handler: tornado.web.RequestHandler, payload: Any, status: int = 200) -> None:
    handler.set_status(status)
    handler.set_header("Content-Type", "application/json; charset=utf-8")
    handler.write(json.dumps(payload, ensure_ascii=False))


class BaseHandler(tornado.web.RequestHandler):
    def set_default_headers(self) -> None:
        # Development CORS (can be restricted later)
        self.set_header("Access-Control-Allow-Origin", "*")
        self.set_header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS")
        self.set_header("Access-Control-Allow-Headers", "Content-Type, If-None-Match")

    def options(self, *args: Any, **kwargs: Any) -> None:  # CORS preflight
        self.set_status(204)
        self.finish()

    def prepare(self) -> None:
        if self.request.headers.get("Content-Type", "").startswith("application/json") and self.request.body:
            try:
                self.json_body = json.loads(self.request.body.decode("utf-8"))
            except Exception:
                self.json_body = None
        else:
            self.json_body = None

    @property
    def store(self) -> FileBackedGraphStore:
        return self.application.settings["store"]


# ---------------------- Schema Handlers ----------------------
class SchemaHandler(BaseHandler):
    async def get(self) -> None:
        schema = await self.store.load_schema()
        json_response(self, schema)

    async def put(self) -> None:
        if not isinstance(self.json_body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        # Basic shape validation
        node_types = self.json_body.get("nodeTypes", {})
        rel_types = self.json_body.get("relationshipTypes", {})
        if not isinstance(node_types, dict) or not isinstance(rel_types, dict):
            json_response(self, {"error": "schema must include nodeTypes and relationshipTypes maps"}, 400)
            return
        await self.store.save_schema(self.json_body)
        json_response(self, {"status": "ok"})


class NodeTypesCollectionHandler(BaseHandler):
    async def get(self) -> None:
        schema = await self.store.load_schema()
        json_response(self, {"nodeTypes": schema.get("nodeTypes", {})})

    async def post(self) -> None:
        if not isinstance(self.json_body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        name = self.json_body.get("name")
        attrs = self.json_body.get("attributes", {})
        label = self.json_body.get("label", name)
        if not name or not isinstance(attrs, dict):
            json_response(self, {"error": "name and attributes are required"}, 400)
            return
        schema = await self.store.load_schema()
        node_types = schema.setdefault("nodeTypes", {})
        node_types[name] = {"label": label, "attributes": attrs}
        await self.store.save_schema(schema)
        json_response(self, {"status": "upserted", "nodeType": node_types[name]}, 201)


class NodeTypeItemHandler(BaseHandler):
    async def get(self, name: str) -> None:
        schema = await self.store.load_schema()
        node_types = schema.get("nodeTypes", {})
        if name not in node_types:
            json_response(self, {"error": "nodeType not found"}, 404)
            return
        json_response(self, node_types[name])

    async def put(self, name: str) -> None:
        if not isinstance(self.json_body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        schema = await self.store.load_schema()
        node_types = schema.setdefault("nodeTypes", {})
        if name not in node_types:
            json_response(self, {"error": "nodeType not found"}, 404)
            return
        node_types[name] = self.json_body
        await self.store.save_schema(schema)
        json_response(self, {"status": "updated", "nodeType": node_types[name]})

    async def delete(self, name: str) -> None:
        cascade = self.get_query_argument("cascade", default="false").lower() == "true"
        schema = await self.store.load_schema()
        node_types = schema.get("nodeTypes", {})
        if name not in node_types:
            json_response(self, {"error": "nodeType not found"}, 404)
            return
        # Prevent deletion if nodes exist
        nodes = await self.store.load_nodes()
        existing = [n for n in nodes.values() if n.get("type") == name]
        if existing and not cascade:
            json_response(self, {"error": "nodeType in use by existing nodes", "count": len(existing)}, 409)
            return
        if existing and cascade:
            # Delete all nodes of this type and incident edges
            for node_id in list(nodes.keys()):
                if nodes[node_id].get("type") == name:
                    await self.store.delete_node(node_id, cascade=True)
        del node_types[name]
        await self.store.save_schema(schema)
        json_response(self, {"status": "deleted"})


class RelationshipTypesCollectionHandler(BaseHandler):
    async def get(self) -> None:
        schema = await self.store.load_schema()
        json_response(self, {"relationshipTypes": schema.get("relationshipTypes", {})})

    async def post(self) -> None:
        if not isinstance(self.json_body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        name = self.json_body.get("name")
        attrs = self.json_body.get("attributes", {})
        label = self.json_body.get("label", name)
        source_types = self.json_body.get("sourceTypes", [])
        target_types = self.json_body.get("targetTypes", [])
        if not name or not isinstance(attrs, dict) or not source_types or not target_types:
            json_response(self, {"error": "name, attributes, sourceTypes, targetTypes are required"}, 400)
            return
        schema = await self.store.load_schema()
        rel_types = schema.setdefault("relationshipTypes", {})
        rel_types[name] = {"label": label, "attributes": attrs, "sourceTypes": source_types, "targetTypes": target_types}
        await self.store.save_schema(schema)
        json_response(self, {"status": "upserted", "relationshipType": rel_types[name]}, 201)


class RelationshipTypeItemHandler(BaseHandler):
    async def get(self, name: str) -> None:
        schema = await self.store.load_schema()
        rel_types = schema.get("relationshipTypes", {})
        if name not in rel_types:
            json_response(self, {"error": "relationshipType not found"}, 404)
            return
        json_response(self, rel_types[name])

    async def put(self, name: str) -> None:
        if not isinstance(self.json_body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        schema = await self.store.load_schema()
        rel_types = schema.setdefault("relationshipTypes", {})
        if name not in rel_types:
            json_response(self, {"error": "relationshipType not found"}, 404)
            return
        rel_types[name] = self.json_body
        await self.store.save_schema(schema)
        json_response(self, {"status": "updated", "relationshipType": rel_types[name]})

    async def delete(self, name: str) -> None:
        cascade = self.get_query_argument("cascade", default="false").lower() == "true"
        schema = await self.store.load_schema()
        rel_types = schema.get("relationshipTypes", {})
        if name not in rel_types:
            json_response(self, {"error": "relationshipType not found"}, 404)
            return
        # Prevent deletion if edges exist
        edges = await self.store.load_edges()
        existing = [e for e in edges.values() if e.get("type") == name]
        if existing and not cascade:
            json_response(self, {"error": "relationshipType in use by existing edges", "count": len(existing)}, 409)
            return
        if existing and cascade:
            for edge_id in list(edges.keys()):
                if edges[edge_id].get("type") == name:
                    await self.store.delete_edge(edge_id)
        del rel_types[name]
        await self.store.save_schema(schema)
        json_response(self, {"status": "deleted"})


# ---------------------- Node Handlers ----------------------
class NodesCollectionHandler(BaseHandler):
    async def get(self) -> None:
        node_type = self.get_query_argument("type", default=None)
        label_contains = self.get_query_argument("labelContains", default=None)
        limit = int(self.get_query_argument("limit", default="50"))
        offset = int(self.get_query_argument("offset", default="0"))
        nodes = await self.store.load_nodes()
        values = list(nodes.values())
        if node_type:
            values = [n for n in values if n.get("type") == node_type]
        if label_contains:
            values = [n for n in values if label_contains.lower() in (n.get("label") or "").lower()]
        total = len(values)
        page = values[offset: offset + limit]
        json_response(self, {"total": total, "items": page})

    async def post(self) -> None:
        body = self.json_body
        if not isinstance(body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        node_id = body.get("id") or str(uuid.uuid4())
        node_type = body.get("type")
        label = body.get("label", node_id)
        attributes = body.get("attributes", {})
        if not node_type:
            json_response(self, {"error": "type is required"}, 400)
            return
        schema = await self.store.load_schema()
        node_type_def = schema.get("nodeTypes", {}).get(node_type)
        if not node_type_def:
            json_response(self, {"error": f"Unknown node type: {node_type}"}, 400)
            return
        ok, errors = validate_attributes(attributes, node_type_def.get("attributes", {}))
        if not ok:
            json_response(self, {"error": "Attribute validation failed", "details": errors}, 400)
            return
        node = {"id": node_id, "type": node_type, "label": label, "attributes": attributes}
        created = await self.store.create_node(node)
        json_response(self, created, 201)


class NodeItemHandler(BaseHandler):
    async def get(self, node_id: str) -> None:
        node = await self.store.get_node(node_id)
        if not node:
            json_response(self, {"error": "node not found"}, 404)
            return
        json_response(self, node)

    async def put(self, node_id: str) -> None:
        body = self.json_body
        if not isinstance(body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        node = await self.store.get_node(node_id)
        if not node:
            json_response(self, {"error": "node not found"}, 404)
            return
        # Update fields
        new_label = body.get("label", node.get("label"))
        new_attributes = body.get("attributes", node.get("attributes", {}))
        # Validate
        schema = await self.store.load_schema()
        node_type_def = schema.get("nodeTypes", {}).get(node.get("type"), {})
        ok, errors = validate_attributes(new_attributes, node_type_def.get("attributes", {}))
        if not ok:
            json_response(self, {"error": "Attribute validation failed", "details": errors}, 400)
            return
        node["label"] = new_label
        node["attributes"] = new_attributes
        updated = await self.store.update_node(node_id, node)
        json_response(self, updated)

    async def delete(self, node_id: str) -> None:
        cascade = self.get_query_argument("cascade", default="false").lower() == "true"
        result = await self.store.delete_node(node_id, cascade=cascade)
        if not result.get("ok"):
            if result.get("notFound"):
                json_response(self, {"error": "node not found"}, 404)
                return
            if result.get("conflict"):
                json_response(self, {"error": "node has incident relationships", "incidentEdges": result.get("incidentEdges", 0)}, 409)
                return
            json_response(self, {"error": "delete failed"}, 400)
            return
        json_response(self, {"status": "deleted", "deletedIncidentEdges": result.get("deletedIncidentEdges", 0)})


# ---------------------- Relationship Handlers ----------------------
class RelationshipsCollectionHandler(BaseHandler):
    async def get(self) -> None:
        rel_type = self.get_query_argument("type", default=None)
        source_id = self.get_query_argument("sourceId", default=None)
        target_id = self.get_query_argument("targetId", default=None)
        limit = int(self.get_query_argument("limit", default="50"))
        offset = int(self.get_query_argument("offset", default="0"))
        edges = await self.store.load_edges()
        values = list(edges.values())
        if rel_type:
            values = [e for e in values if e.get("type") == rel_type]
        if source_id:
            values = [e for e in values if e.get("source") == source_id]
        if target_id:
            values = [e for e in values if e.get("target") == target_id]
        total = len(values)
        page = values[offset: offset + limit]
        json_response(self, {"total": total, "items": page})

    async def post(self) -> None:
        body = self.json_body
        if not isinstance(body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        edge_id = body.get("id") or str(uuid.uuid4())
        rel_type = body.get("type")
        source = body.get("source")
        target = body.get("target")
        attributes = body.get("attributes", {})
        if not rel_type or not source or not target:
            json_response(self, {"error": "type, source, target are required"}, 400)
            return
        # Validate types and existence
        schema = await self.store.load_schema()
        rel_type_def = schema.get("relationshipTypes", {}).get(rel_type)
        if not rel_type_def:
            json_response(self, {"error": f"Unknown relationship type: {rel_type}"}, 400)
            return
        source_node = await self.store.get_node(source)
        target_node = await self.store.get_node(target)
        if not source_node or not target_node:
            json_response(self, {"error": "source or target node not found"}, 400)
            return
        if source_node.get("type") not in rel_type_def.get("sourceTypes", []):
            json_response(self, {"error": f"source type {source_node.get('type')} not allowed for {rel_type}"}, 400)
            return
        if target_node.get("type") not in rel_type_def.get("targetTypes", []):
            json_response(self, {"error": f"target type {target_node.get('type')} not allowed for {rel_type}"}, 400)
            return
        ok, errors = validate_attributes(attributes, rel_type_def.get("attributes", {}))
        if not ok:
            json_response(self, {"error": "Attribute validation failed", "details": errors}, 400)
            return
        edge = {"id": edge_id, "type": rel_type, "source": source, "target": target, "attributes": attributes}
        created = await self.store.create_edge(edge)
        json_response(self, created, 201)


class RelationshipItemHandler(BaseHandler):
    async def get(self, edge_id: str) -> None:
        edge = await self.store.get_edge(edge_id)
        if not edge:
            json_response(self, {"error": "relationship not found"}, 404)
            return
        json_response(self, edge)

    async def put(self, edge_id: str) -> None:
        body = self.json_body
        if not isinstance(body, dict):
            json_response(self, {"error": "Invalid JSON body"}, 400)
            return
        edge = await self.store.get_edge(edge_id)
        if not edge:
            json_response(self, {"error": "relationship not found"}, 404)
            return
        # Update fields
        new_type = body.get("type", edge.get("type"))
        new_source = body.get("source", edge.get("source"))
        new_target = body.get("target", edge.get("target"))
        new_attributes = body.get("attributes", edge.get("attributes", {}))
        # Validate against schema and node existence
        schema = await self.store.load_schema()
        rel_type_def = schema.get("relationshipTypes", {}).get(new_type)
        if not rel_type_def:
            json_response(self, {"error": f"Unknown relationship type: {new_type}"}, 400)
            return
        source_node = await self.store.get_node(new_source)
        target_node = await self.store.get_node(new_target)
        if not source_node or not target_node:
            json_response(self, {"error": "source or target node not found"}, 400)
            return
        if source_node.get("type") not in rel_type_def.get("sourceTypes", []):
            json_response(self, {"error": f"source type {source_node.get('type')} not allowed for {new_type}"}, 400)
            return
        if target_node.get("type") not in rel_type_def.get("targetTypes", []):
            json_response(self, {"error": f"target type {target_node.get('type')} not allowed for {new_type}"}, 400)
            return
        ok, errors = validate_attributes(new_attributes, rel_type_def.get("attributes", {}))
        if not ok:
            json_response(self, {"error": "Attribute validation failed", "details": errors}, 400)
            return
        edge["type"] = new_type
        edge["source"] = new_source
        edge["target"] = new_target
        edge["attributes"] = new_attributes
        updated = await self.store.update_edge(edge_id, edge)
        json_response(self, updated)

    async def delete(self, edge_id: str) -> None:
        existed = await self.store.delete_edge(edge_id)
        if not existed:
            json_response(self, {"error": "relationship not found"}, 404)
            return
        json_response(self, {"status": "deleted"})


def make_app(data_dir: str = "/workspace/data") -> tornado.web.Application:
    os.makedirs(data_dir, exist_ok=True)
    store = FileBackedGraphStore(data_dir)
    return tornado.web.Application([
        (r"/schema", SchemaHandler),
        (r"/schema/node-types", NodeTypesCollectionHandler),
        (r"/schema/node-types/([\w:-]+)", NodeTypeItemHandler),
        (r"/schema/relationship-types", RelationshipTypesCollectionHandler),
        (r"/schema/relationship-types/([\w:-]+)", RelationshipTypeItemHandler),
        (r"/nodes", NodesCollectionHandler),
        (r"/nodes/([\w\-\.:]+)", NodeItemHandler),
        (r"/relationships", RelationshipsCollectionHandler),
        (r"/relationships/([\w\-\.:]+)", RelationshipItemHandler),
    ], debug=True, store=store)


if __name__ == "__main__":
    app = make_app()
    port = int(os.environ.get("PORT", "8080"))
    app.listen(port)
    print(f"Tornado server running on http://0.0.0.0:{port}")
    tornado.ioloop.IOLoop.current().start()