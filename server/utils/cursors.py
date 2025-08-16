import base64
import json
from typing import Any, Dict


def encode_cursor(obj: Dict[str, Any]) -> str:
	payload = json.dumps(obj, separators=(",", ":")).encode("utf-8")
	return base64.urlsafe_b64encode(payload).decode("ascii")


def decode_cursor(cursor: str) -> Dict[str, Any]:
	try:
		data = base64.urlsafe_b64decode(cursor.encode("ascii"))
		return json.loads(data.decode("utf-8"))
	except Exception:
		return {}