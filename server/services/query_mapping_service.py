import os
import json
from typing import Any, Dict, List


class QueryMappingService:
	def __init__(self, data_dir: str) -> None:
		self.mapping_path = os.path.join(data_dir, "query_mapping.json")
		self._cache: Dict[str, Any] = {}

	def _load(self) -> Dict[str, Any]:
		if self._cache:
			return self._cache
		try:
			with open(self.mapping_path, "r", encoding="utf-8") as f:
				self._cache = json.load(f)
		except FileNotFoundError:
			self._cache = {}
		return self._cache

	def get_paths(self, reference_data_type: str, query_by: str) -> List[str]:
		data = self._load()
		return (data.get(reference_data_type, {}).get(query_by) or [])

	def get_economics_paths(self, reference_data_type: str) -> Dict[str, str]:
		data = self._load()
		return (data.get(reference_data_type, {}).get("EconomicsFields") or {})