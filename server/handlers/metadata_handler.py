import tornado.web
from typing import Any

from ..services.metadata_service import MetadataService


class MetadataHandler(tornado.web.RequestHandler):
	def initialize(self, metadata_service: MetadataService) -> None:
		self.metadata_service = metadata_service

	def set_default_headers(self) -> None:
		self.set_header("Access-Control-Allow-Origin", "*")
		self.set_header("Access-Control-Allow-Methods", "GET,OPTIONS")
		self.set_header("Access-Control-Allow-Headers", "Content-Type")

	def options(self) -> None:
		self.set_status(204)
		self.finish()

	async def get(self) -> None:
		data = await self.metadata_service.get_metadata()
		self.set_header("Content-Type", "application/json; charset=utf-8")
		self.write(data)