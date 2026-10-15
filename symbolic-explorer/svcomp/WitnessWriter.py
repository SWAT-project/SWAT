"""
Writes the SV-COMP violation witness of a testcase from the trace of the violating execution.

The executor ships the witness data in the trace (``nondetReturns``: every value returned by a
``Verifier.nondet*()`` call with the location of the call; ``violation``: the throwable that escaped
the entry method with its stack frames). The Database keeps the data of the latest trace; when the
final verdict is VIOLATION the driver hands it to :func:`write_violation_witness`, which builds the
witness files with the ``witnesscreator`` package (``witness.yml`` in format 2.3 and the legacy
``witness.graphml``).

Witness generation must never break or delay the verdict: everything here is caught and logged.
"""

import time
from pathlib import Path
from typing import Dict, List, Optional

import log
from timing.TimingManager import TimingManager

logger = log.get_logger()

# The witness formats written per violation; YAML (2.3) is what SV-COMP 2027 requires, GraphML is
# kept for validators that still read it (e.g. wit4java).
WITNESS_FORMATS = ("yaml", "graphml")
PRODUCER_NAME = "SWAT"


def write_violation_witness(witness_data: Optional[dict], input_files: List[str], property_file: str,
                            out_dir: str, producer_version: str,
                            base_dir: Optional[str] = None) -> Dict[str, Path]:
    """
    Write the violation witness files for the trace data of the violating execution.

    Args:
        witness_data: ``{'nondet_returns': [...], 'violation': {...} | None}`` as stored by the
            Database for the latest trace, or None if no trace of the violating execution arrived.
        input_files: The task's input files/directories exactly as given to the verifier.
        property_file: Path to the property file of the task.
        out_dir: Directory the witness files are written to (created if needed).
        producer_version: The SWAT version recorded in the witness metadata.
        base_dir: Directory relative input paths are resolved against (default: the working directory).

    Returns:
        The written files per format (``{'yaml': Path, 'graphml': Path}``), empty if nothing was written.
        Never raises.
    """
    start = time.perf_counter()
    try:
        return _write(witness_data, input_files, property_file, out_dir, producer_version, base_dir)
    except Exception:  # noqa: BLE001 - the verdict must not depend on the witness
        logger.exception('[WITNESS] Writing the violation witness failed')
        return {}
    finally:
        TimingManager.instance().record_witness_generation_time(time.perf_counter() - start)


def _write(witness_data: Optional[dict], input_files: List[str], property_file: str, out_dir: str,
           producer_version: str, base_dir: Optional[str]) -> Dict[str, Path]:
    try:
        from witnesscreator import Frame, Location, NondetReturn, Task, Violation, write_witnesses
    except ImportError as e:
        logger.error(f'[WITNESS] The witnesscreator package is not available, no witness is written: {e}')
        return {}

    if witness_data is None:
        logger.warning('[WITNESS] No trace of the violating execution arrived; the witness has no waypoints')
        witness_data = {}

    values = []
    for nr in witness_data.get('nondet_returns') or []:
        values.append(NondetReturn(
            function=nr['function'],
            type=nr['type'],
            value=nr['value'],
            location=Location(class_name=nr['className'], source_file=nr.get('sourceFile') or None,
                              line=nr['line'], call_index=nr.get('callIndex', 0)),
            bits=nr.get('bits'),
        ))

    violation = None
    v = witness_data.get('violation')
    if v is not None:
        violation = Violation(
            throwable=v['throwable'],
            frames=[Frame(class_name=f['className'], source_file=f.get('sourceFile') or None, line=f['line'])
                    for f in v.get('frames') or []],
        )
    else:
        logger.warning('[WITNESS] No uncaught throwable recorded; the witness has no target waypoint')

    task = Task(input_files=list(input_files), property_file=property_file,
                producer_name=PRODUCER_NAME, producer_version=producer_version, base_dir=base_dir)

    logger.info(f'[WITNESS] Writing witness ({len(values)} nondet values, '
                f'target: {violation.throwable if violation else "none"}) to {out_dir}')
    paths = write_witnesses(task, values, violation, out_dir=out_dir, formats=WITNESS_FORMATS)
    for fmt, path in paths.items():
        logger.info(f'[WITNESS] Written {fmt}: {path}')
    missing = [fmt for fmt in WITNESS_FORMATS if fmt not in paths]
    if missing:
        logger.error(f'[WITNESS] Not written: {missing}')
    return dict(paths)
