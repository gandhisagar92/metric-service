from collections import Counter, defaultdict
from typing import Any, Dict, Iterable, List


def compute_facets(relation: str, nodes: Iterable[Dict[str, Any]], facet_fields: List[str]) -> Dict[str, List[Dict[str, Any]]]:
	result: Dict[str, List[Dict[str, Any]]] = {}
	for field in facet_fields:
		counter: Counter = Counter()
		for n in nodes:
			val = _get_by_path(n, field)
			if val is None:
				continue
			counter[val] += 1
		result[field.split(".")[-1]] = [{"value": k, "count": v} for k, v in counter.most_common()]
	return result


def _get_by_path(obj: Dict[str, Any], path: str) -> Any:
	parts = path.split(".")
	cur: Any = obj
	for p in parts:
		if isinstance(cur, dict) and p in cur:
			cur = cur[p]
		else:
			return None
	return cur