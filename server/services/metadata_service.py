import os
import json
from typing import Any, Dict

from ..storage import FileBackedGraphStore


class MetadataService:
	def __init__(self, data_dir: str) -> None:
		self.data_dir = data_dir
		self.metadata_path = os.path.join(self.data_dir, "metadata.json")

	async def get_metadata(self) -> Dict[str, Any]:
		if not os.path.exists(self.metadata_path):
			return {"version": "1.0", "referenceDataTypes": {}}
		# Read with store thread helper not needed; small file
		with open(self.metadata_path, "r", encoding="utf-8") as f:
			data = json.load(f)
		return data if isinstance(data, dict) else {"version": "1.0", "referenceDataTypes": {}}