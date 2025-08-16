from typing import Any, Dict, List


def _get_by_path(obj: Dict[str, Any], path: str) -> Any:
	parts = path.split(".")
	cur: Any = obj
	for p in parts:
		if isinstance(cur, dict) and p in cur:
			cur = cur[p]
		else:
			return None
	return cur


def matches_filters(item: Dict[str, Any], filters: List[Dict[str, Any]]) -> bool:
	for f in filters:
		field = f.get("field")
		op = (f.get("op") or "EQ").upper()
		value = f.get("value")
		actual = _get_by_path(item, field) if field else None
		if op == "EQ":
			if actual != value:
				return False
		elif op == "IN":
			if not isinstance(value, list) or actual not in value:
				return False
		elif op == "NE":
			if actual == value:
				return False
		elif op == "CONTAINS":
			if not isinstance(actual, str) or not isinstance(value, str) or value.lower() not in actual.lower():
				return False
		# Extend with GT/LT etc. when needed
	return True