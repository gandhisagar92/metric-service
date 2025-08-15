import json
import tornado.web
from typing import Any

from ..services.graph_service import GraphService
from ..providers.payload_providers import PayloadProviderRegistry


class GraphSearchHandler(tornado.web.RequestHandler):
	def initialize(self, graph_service: GraphService) -> None:
		self.graph_service = graph_service

	def set_default_headers(self) -> None:
		self.set_header("Access-Control-Allow-Origin", "*")
		self.set_header("Access-Control-Allow-Methods", "POST,OPTIONS")
		self.set_header("Access-Control-Allow-Headers", "Content-Type")

	def options(self) -> None:
		self.set_status(204)
		self.finish()

	async def post(self) -> None:
		try:
			body = json.loads(self.request.body.decode("utf-8"))
		except Exception:
			self.set_status(400)
			self.write({"error": "Invalid JSON"})
			return
		result = await self.graph_service.search(body)
		self.set_header("Content-Type", "application/json; charset=utf-8")
		self.write(result)


class GraphExpandHandler(tornado.web.RequestHandler):
	def initialize(self, graph_service: GraphService) -> None:
		self.graph_service = graph_service

	def set_default_headers(self) -> None:
		self.set_header("Access-Control-Allow-Origin", "*")
		self.set_header("Access-Control-Allow-Methods", "POST,OPTIONS")
		self.set_header("Access-Control-Allow-Headers", "Content-Type")

	def options(self) -> None:
		self.set_status(204)
		self.finish()

	async def post(self) -> None:
		try:
			body = json.loads(self.request.body.decode("utf-8"))
		except Exception:
			self.set_status(400)
			self.write({"error": "Invalid JSON"})
			return
		result = await self.graph_service.expand(body)
		self.set_header("Content-Type", "application/json; charset=utf-8")
		self.write(result)


class NodePayloadHandler(tornado.web.RequestHandler):
	def initialize(self, payload_registry: PayloadProviderRegistry) -> None:
		self.payload_registry = payload_registry
		self.store = self.application.settings["store"]

	def set_default_headers(self) -> None:
		self.set_header("Access-Control-Allow-Origin", "*")
		self.set_header("Access-Control-Allow-Methods", "GET,OPTIONS")
		self.set_header("Access-Control-Allow-Headers", "Content-Type")

	def options(self, node_id: str) -> None:
		self.set_status(204)
		self.finish()

	async def get(self, node_id: str) -> None:
		node = await self.store.get_node(node_id)
		if not node:
			self.set_status(404)
			self.write({"error": "node not found"})
			return
		payload = await self.payload_registry.get_payload(node_id, node.get("type"))
		self.set_header("Content-Type", "application/json; charset=utf-8")
		self.write(payload)