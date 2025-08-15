import re
from typing import Any, Dict, List, Tuple


_STRING_TYPES = {"string"}
_NUMERIC_TYPES = {"number", "integer"}
_BOOL_TYPES = {"boolean"}
_DATE_TYPES = {"date", "datetime"}
_ENUM_TYPES = {"enum"}

_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_DATETIME_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z?$")


def _type_check(value: Any, expected: str) -> bool:
    if expected in _STRING_TYPES:
        return isinstance(value, str)
    if expected == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    if expected == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    if expected in _BOOL_TYPES:
        return isinstance(value, bool)
    if expected == "date":
        return isinstance(value, str) and bool(_DATE_RE.match(value))
    if expected == "datetime":
        return isinstance(value, str) and bool(_DATETIME_RE.match(value))
    if expected in _ENUM_TYPES:
        # Enum is validated separately against options
        return True
    # Unknown type: accept
    return True


def validate_attributes(attrs: Dict[str, Any], schema_attrs: Dict[str, Dict[str, Any]]) -> Tuple[bool, List[Dict[str, Any]]]:
    errors: List[Dict[str, Any]] = []
    # Required fields
    for key, spec in schema_attrs.items():
        if spec.get("required") and key not in attrs:
            errors.append({"field": key, "code": "required", "message": "Field is required"})
    # Validate provided fields
    for key, value in attrs.items():
        spec = schema_attrs.get(key, {})
        expected_type = spec.get("type")
        if expected_type and not _type_check(value, expected_type):
            errors.append({"field": key, "code": "type", "message": f"Expected {expected_type}"})
            continue
        # String constraints
        if expected_type in _STRING_TYPES and isinstance(value, str):
            min_len = spec.get("minLength")
            max_len = spec.get("maxLength")
            pattern = spec.get("pattern")
            if isinstance(min_len, int) and len(value) < min_len:
                errors.append({"field": key, "code": "minLength", "message": f"minLength {min_len}"})
            if isinstance(max_len, int) and len(value) > max_len:
                errors.append({"field": key, "code": "maxLength", "message": f"maxLength {max_len}"})
            if isinstance(pattern, str) and not re.match(pattern, value):
                errors.append({"field": key, "code": "pattern", "message": "pattern mismatch"})
        # Number constraints
        if expected_type in _NUMERIC_TYPES and isinstance(value, (int, float)):
            min_val = spec.get("min")
            max_val = spec.get("max")
            if min_val is not None and value < min_val:
                errors.append({"field": key, "code": "min", "message": f"min {min_val}"})
            if max_val is not None and value > max_val:
                errors.append({"field": key, "code": "max", "message": f"max {max_val}"})
        # Enum options
        if expected_type in _ENUM_TYPES:
            options = spec.get("options") or spec.get("enum")
            if isinstance(options, list) and value not in options:
                errors.append({"field": key, "code": "enum", "message": f"must be one of {options}"})
    return (len(errors) == 0, errors)