import os
import json
from typing import Any, Dict, Protocol

from ..storage import FileBackedGraphStore


class PayloadProvider(Protocol):
	async def get_payload(self, node_id: str) -> Dict[str, Any]:
		...


class FilePayloadProvider:
	def __init__(self, store: FileBackedGraphStore) -> None:
		self.store = store

	async def get_payload(self, node_id: str) -> Dict[str, Any]:
		node = await self.store.get_node(node_id)
		if not node:
			return {}
		# For mock, attributes are already hydrated
		return {
			"id": node["id"],
			"type": node["type"],
			"displayLabel": node.get("label", node["id"]),
			"attributes": node.get("attributes", {}),
			"audit": {"asOf": None, "sourceSystem": "MockFile"}
		}


class PayloadProviderRegistry:
	def __init__(self, store: FileBackedGraphStore, data_dir: str) -> None:
		self.store = store
		self.providers: Dict[str, PayloadProvider] = {}
		self.mapping_path = os.path.join(data_dir, "providers.json")
		self._load_default()

	def _load_default(self) -> None:
		# Always have file provider
		self.providers["file"] = FilePayloadProvider(self.store)  # type: ignore

	async def get_provider_name_for_type(self, node_type: str) -> str:
		try:
			with open(self.mapping_path, "r", encoding="utf-8") as f:
				cfg = json.load(f)
		except FileNotFoundError:
			cfg = {"providers": {}}
		prov = (cfg.get("providers") or {}).get(node_type) or cfg.get("defaultProvider") or "file"
		return prov

	async def get_payload(self, node_id: str, node_type: str) -> Dict[str, Any]:
		provider_name = await self.get_provider_name_for_type(node_type)
		provider = self.providers.get(provider_name)
		if provider is None:
			# Fallback to file provider
			provider = self.providers["file"]
		return await provider.get_payload(node_id)