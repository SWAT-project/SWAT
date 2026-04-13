<p align="center">
  <img src="https://swat-project.github.io/docs/assets/images/logo.png" alt="SWAT Logo" width="300">
</p>

# SWAT

SWAT is a dynamic symbolic execution engine for Java applications. It uses on-the-fly bytecode instrumentation to facilitate symbolic tracking, enabling automated test generation and program analysis without requiring source code modifications.


## Getting Started

The [examples](targets/examples/) directory contains ready-to-run targets that demonstrate SWAT's core features:

- [**basic-1**](targets/examples/basic-1/) — Introduction to SWAT's symbolic execution capabilities with simple Java programs.
- [**spring-1**](targets/examples/spring-1/) — Symbolic execution of Spring Boot controller parameters.

Each example includes its own README with setup and usage instructions.


## Documentation

Documentation is available at [swat-project.github.io/docs](https://swat-project.github.io/docs/).

`JavaDoc`for the [**symbolic-executor**](symbolic-executor/src/main/java/de/uzl/its/swat) is availale online at [swat-project.github.io/docs/javadoc](https://swat-project.github.io/docs/javadoc)

## SV-COMP

SWAT has participated in the [International Competition on Software Verification (SV-COMP)](https://sv-comp.sosy-lab.org/) since 2024:

| Year | Results | Artifact | 
|------|--------|--------|
| 2024 | [SV-Comp](https://sv-comp.sosy-lab.org/2024/results/results-verified/) | [Zenodo](https://zenodo.org/records/10206092)/ [GitHub](https://github.com/SWAT-project/SWAT/tree/SV-COMP-Submission-2024) |
| 2025 | [SV-Comp](https://sv-comp.sosy-lab.org/2025/results/results-verified/) | [Zenodo](https://zenodo.org/records/14214662)/ [GitHub](https://github.com/SWAT-project/SWAT/tree/SV-COMP-Submission-2025) |
| 2026 | [SV-Comp](https://sv-comp.sosy-lab.org/2026/results/results-verified/) | [Zenodo](https://zenodo.org/records/17748741)/ [GitHub](https://github.com/SWAT-project/SWAT/tree/SV-COMP-Submission-2026) |


## Publications
SV-Comp 2026 Competition Paper: 
```
@inproceedings{SWAT-SVCOMP26,
  author    = {N.~Loose and F.~Sieck and F.~Mächtle and T.~Eisenbarth},
  title     = {{SWAT:} Improvements to the Symbolic Executor (Competition Contribution)},
  booktitle = {Proc.\ TACAS~(2)},
  year      = {2026},
  publisher = {Springer},
  series    = {LNCS~16506},
  doi       = {10.1007/978-3-032-22749-2_35},
}
```
SV-Comp 2024 Competition Paper: 
```
@inproceedings{SWAT-SVCOMP24,
  author       = {N.~Loose and F.~Mächtle and F.~Sieck and T.~Eisenbarth}
  title        = {{SWAT:} Modular Dynamic Symbolic Execution for Java Applications using Dynamic Instrumentation (Competition Contribution)},
  booktitle    = {Proc.\ TACAS~(3)},
  series       = {LNCS},
  publisher    = {Springer},
  year         = {2024},
  url          = {https://doi.org/10.1007/978-3-031-57256-2_28}
}
```