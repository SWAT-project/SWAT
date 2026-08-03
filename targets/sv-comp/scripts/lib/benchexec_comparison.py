"""Compare BenchExec result XML files."""

from __future__ import annotations

import bz2
import json
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True, order=True)
class BenchmarkKey:
    """Stable identity of one benchmark task in one property."""

    task: str
    property: str
    expected_verdict: str


@dataclass(frozen=True)
class BenchmarkOutcome:
    """Outcome reported by BenchExec for one task."""

    category: str
    status: str
    cputime: str
    walltime: str
    source: str
    display_name: str
    runset: str

    @property
    def label(self) -> str:
        if self.category and self.status:
            return f"{self.category}/{self.status}"
        return self.category or self.status or "missing"


def collect_result_files(paths: Iterable[str | Path]) -> list[Path]:
    """Collect BenchExec XML result files from files or directories."""

    result_files: list[Path] = []
    for raw_path in paths:
        path = Path(raw_path)
        if path.is_dir():
            result_files.extend(path.rglob("*.xml"))
            result_files.extend(path.rglob("*.xml.bz2"))
        elif path.is_file() and (path.name.endswith(".xml") or path.name.endswith(".xml.bz2")):
            result_files.append(path)
        else:
            raise FileNotFoundError(f"No BenchExec XML result found at {path}")

    unique_files = sorted(set(result_files))
    if not unique_files:
        raise FileNotFoundError("No BenchExec XML result files found")
    return unique_files


def load_benchexec_results(paths: Iterable[str | Path]) -> dict[BenchmarkKey, BenchmarkOutcome]:
    """Load task outcomes from BenchExec result XML files."""

    results: dict[BenchmarkKey, BenchmarkOutcome] = {}
    for result_file in collect_result_files(paths):
        root = _parse_xml(result_file)
        display_name = root.attrib.get("displayName", "")
        runset = root.attrib.get("name", "")

        for run in root.findall("run"):
            columns = {column.attrib.get("title", ""): column.attrib.get("value", "") for column in run.findall("column")}
            key = BenchmarkKey(
                task=_normalize_task_name(run.attrib.get("name", "")),
                property=_extract_property(run),
                expected_verdict=run.attrib.get("expectedVerdict", ""),
            )
            if key in results:
                raise ValueError(f"Duplicate benchmark result for {key}")

            results[key] = BenchmarkOutcome(
                category=columns.get("category", ""),
                status=columns.get("status", ""),
                cputime=columns.get("cputime", ""),
                walltime=columns.get("walltime", ""),
                source=str(result_file),
                display_name=display_name,
                runset=runset,
            )

    return results


def compare_benchexec_results(
    reference_paths: Iterable[str | Path],
    current_paths: Iterable[str | Path],
    *,
    include_status_changes: bool = True,
) -> dict:
    """Compare two BenchExec result sets and return a serializable diff."""

    reference = load_benchexec_results(reference_paths)
    current = load_benchexec_results(current_paths)

    changes = []
    for key in sorted(set(reference) | set(current)):
        old = reference.get(key)
        new = current.get(key)

        if old is None:
            transition_changed = True
        elif new is None:
            transition_changed = True
        elif include_status_changes:
            transition_changed = (old.category, old.status) != (new.category, new.status)
        else:
            transition_changed = old.category != new.category

        if transition_changed:
            changes.append(_change_to_dict(key, old, new))

    return {
        "reference": _summarize(reference),
        "current": _summarize(current),
        "changes": changes,
        "transition_counts": dict(Counter(change["transition"] for change in changes)),
    }


def format_benchexec_diff(diff: dict, *, markdown: bool = False) -> str:
    """Format a BenchExec comparison for humans."""

    reference = diff["reference"]
    current = diff["current"]
    changes = diff["changes"]
    gained = sum(1 for change in changes if change["from_category"] != "correct" and change["to_category"] == "correct")
    lost = sum(1 for change in changes if change["from_category"] == "correct" and change["to_category"] != "correct")

    if markdown:
        lines = [
            f"- Reference: {reference['correct']}/{reference['total']} correct",
            f"- Current: {current['correct']}/{current['total']} correct",
            f"- Outcome changes: {len(changes)} ({gained} gained correct, {lost} lost correct)",
            "",
        ]
        for change in changes:
            lines.append(
                f"- `{change['property']}` `{change['task']}`: "
                f"`{change['from']}` -> `{change['to']}`"
            )
        return "\n".join(lines)

    lines = [
        f"Reference: {reference['correct']}/{reference['total']} correct",
        f"Current:   {current['correct']}/{current['total']} correct",
        f"Changes:   {len(changes)} ({gained} gained correct, {lost} lost correct)",
        "",
    ]
    for change in changes:
        lines.append(
            f"{change['property']:22} {change['task']:80} "
            f"{change['from']} -> {change['to']}"
        )
    return "\n".join(lines)


def diff_to_json(diff: dict) -> str:
    """Serialize a BenchExec comparison as stable JSON."""

    return json.dumps(diff, indent=2, sort_keys=True)


def _parse_xml(path: Path) -> ET.Element:
    if path.name.endswith(".bz2"):
        with bz2.open(path, "rb") as xml_file:
            return ET.fromstring(xml_file.read())

    return ET.parse(path).getroot()


def _normalize_task_name(raw_name: str) -> str:
    marker = "sv-benchmarks/java/"
    if marker in raw_name:
        return raw_name.split(marker, 1)[1]
    return raw_name


def _extract_property(run: ET.Element) -> str:
    prop = run.attrib.get("properties", "")
    if prop:
        return prop

    property_file = run.attrib.get("propertyFile", "")
    if property_file:
        return Path(property_file).stem

    return ""


def _summarize(results: dict[BenchmarkKey, BenchmarkOutcome]) -> dict:
    categories = Counter(outcome.category or "missing" for outcome in results.values())
    return {
        "total": len(results),
        "correct": categories.get("correct", 0),
        "categories": dict(sorted(categories.items())),
        "display_names": sorted({outcome.display_name for outcome in results.values() if outcome.display_name}),
        "runsets": sorted({outcome.runset for outcome in results.values() if outcome.runset}),
    }


def _change_to_dict(
    key: BenchmarkKey,
    old: BenchmarkOutcome | None,
    new: BenchmarkOutcome | None,
) -> dict:
    from_category = old.category if old else "missing"
    to_category = new.category if new else "missing"
    from_label = old.label if old else "missing"
    to_label = new.label if new else "missing"

    return {
        "task": key.task,
        "property": key.property,
        "expected_verdict": key.expected_verdict,
        "from": from_label,
        "to": to_label,
        "from_category": from_category,
        "to_category": to_category,
        "transition": f"{from_category}->{to_category}",
        "reference_source": old.source if old else "",
        "current_source": new.source if new else "",
    }
