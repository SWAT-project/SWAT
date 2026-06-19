# SWAT SV-COMP Package

This archive contains a runnable SWAT package for SV-COMP style Java verification tasks.

Important entry points:

- `run-swat.sh`: verifier entry point used by BenchExec.
- `compile-target.sh`: helper used by `run_swat.py` to compile Java benchmark sources.
- `smoketest.sh`: runs SWAT on two bundled smoke-test targets.
- `sv-comp.cfg`: SWAT configuration for SV-COMP mode.

The package expects Java 17 and the bundled `.venv_ubuntu_24_04_1__x86_64`
Python environment. `run-swat.sh` intentionally uses only that environment.

Example:

```sh
./run-swat.sh ../../sv-benchmarks/java/properties/valid-assert.prp smoketest/common smoketest
```
