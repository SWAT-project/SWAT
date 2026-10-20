# SWAT SV-COMP Package

This archive contains a runnable SWAT package for SV-COMP style Java verification tasks.

Important entry points:

- `run-swat.sh`: verifier entry point used by BenchExec (`run-swat.sh --version`
  or `-v` prints the version).
- `compile-target.sh`: helper used by `run_swat.py` to compile Java benchmark sources.
- `smoketest.sh`: runs SWAT on the bundled smoke-test target.
- `sv-comp.cfg`: SWAT configuration for SV-COMP mode.
- `THIRD-PARTY-LICENSES.md`: bundled third-party components and their licenses.

Requirements (SV-COMP 2027 environment, Ubuntu 26.04): `python3` (3.14) and
Java 17 (`openjdk-17-jdk-headless`, which also provides `javac`).

All Python dependencies are bundled in `libs/python/` and are used with the
system `python3`; no virtual environment is needed. They are built for the
Python version recorded in `libs/python/PYTHON_VERSION`, and `run-swat.sh`
refuses to start with a different one. Set `SWAT_PYTHON` to use another
interpreter of that version. The full tool log of the last run is in `out.log`;
stdout is truncated if it would exceed SV-COMP's output limit.

Example:

```sh
./run-swat.sh ../../sv-benchmarks/java/properties/valid-assert.prp smoketest/common smoketest
```
