#!/usr/bin/env python3
"""Writes THIRD-PARTY-LICENSES.md into a staged SV-COMP package.

Usage: third_party_licenses.py <package_dir>

Inputs, all read from the package itself:
- META-INF/swat-third-party.json inside symbolic-executor/lib/symbolic-executor.jar
  (written by the thirdPartyList Gradle task: the Java components shaded into the jar), and the
  same file inside cfg-extraction/cfg-extractor-precheck.jar (cfg-extraction's precheckJar task);
- libs/java-library-path/z3-LICENSE.txt (LICENSE.txt of the bundled Z3 release);
- every libs/python/*.dist-info (bundled Python packages and the license files they ship).

Java license texts come from scripts/svcomp-package/licenses/<SPDX-id>.txt.
Exits non-zero, naming the component, if a bundled component has no determinable
license or no license text.
"""

import json
import re
import sys
import zipfile
from email.parser import Parser
from pathlib import Path

LICENSE_DIR = Path(__file__).resolve().parent / "licenses"
JAR = "symbolic-executor/lib/symbolic-executor.jar"
PRECHECK_JAR = "cfg-extraction/cfg-extractor-precheck.jar"
JAR_LIST = "META-INF/swat-third-party.json"
Z3_LICENSE = "libs/java-library-path/z3-LICENSE.txt"
PYTHON_DIR = "libs/python"

# POM license names -> SPDX ids. A name missing here fails the build (add it after checking).
JAVA_LICENSES = {
    "apache 2.0": "Apache-2.0",
    "apache license, version 2.0": "Apache-2.0",
    "the apache license, version 2.0": "Apache-2.0",
    "the apache software license, version 2.0": "Apache-2.0",
    "gnu lesser general public license, version 2.1": "LGPL-2.1-only",
    "the mit license": "MIT",
    "mit license": "MIT",
}
# Python trove classifiers (after "License :: OSI Approved :: ") -> SPDX ids.
PY_CLASSIFIERS = {
    "MIT License": "MIT",
    "Apache Software License": "Apache-2.0",
    "Mozilla Public License 2.0 (MPL 2.0)": "MPL-2.0",
}
# Packages whose metadata does not name an SPDX license (checked by hand).
PY_LICENSE_OVERRIDES = {
    "witnesscreator": "BSD-2-Clause",  # License field holds the copyright line; text is BSD-2-Clause
}
# Packages that ship no license file in their dist-info: package-relative text to use instead.
PY_TEXT_FALLBACKS = {
    "z3-solver": Z3_LICENSE,  # same project and license as the bundled Z3 release
}
SPDX_ID = re.compile(r"^[A-Za-z0-9.+-]+$")


def fail(msg):
    sys.exit(f"third_party_licenses.py: error: {msg}")


def spdx_of(name):
    key = name.strip()
    if SPDX_ID.match(key) and (LICENSE_DIR / f"{key}.txt").is_file():
        return key
    return JAVA_LICENSES.get(key.lower())


def java_components(pkg, jar_path=JAR):
    jar = pkg / jar_path
    if not jar.is_file():
        fail(f"missing {jar_path}")
    with zipfile.ZipFile(jar) as zf:
        try:
            entries = json.loads(zf.read(JAR_LIST))
        except KeyError:
            fail(f"{jar_path} has no {JAR_LIST}; build it with the thirdPartyList (or precheckJar) Gradle task")
    comps = []
    for e in entries:
        coord = f"{e['group']}:{e['name']}"
        ids = []
        for lic in e["licenses"]:
            spdx = spdx_of(lic["name"])
            if spdx is None:
                fail(f"Java component {coord}: unmapped license {lic['name']!r} (add it to JAVA_LICENSES)")
            ids.append(spdx)
        if not ids:
            fail(f"Java component {coord}: no license in its POM")
        for spdx in ids:
            if not (LICENSE_DIR / f"{spdx}.txt").is_file():
                fail(f"Java component {coord}: no license text {LICENSE_DIR / (spdx + '.txt')}")
        comps.append({"name": coord, "version": e["version"], "license": " OR ".join(ids),
                      "ids": ids, "url": e["url"]})
    return comps


def python_license(name, meta):
    if name in PY_LICENSE_OVERRIDES:
        return PY_LICENSE_OVERRIDES[name]
    if meta.get("License-Expression"):
        return meta["License-Expression"].strip()
    lic = (meta.get("License") or "").strip()
    if lic and SPDX_ID.match(lic):
        return lic
    for c in meta.get_all("Classifier") or []:
        if c.startswith("License :: OSI Approved :: ") and c[27:] in PY_CLASSIFIERS:
            return PY_CLASSIFIERS[c[27:]]
    return PY_CLASSIFIERS.get(lic)  # e.g. z3-solver: "License: MIT License"


def python_url(meta):
    if meta.get("Home-page"):
        return meta["Home-page"].strip()
    urls = {}
    for entry in meta.get_all("Project-URL") or []:
        label, _, url = entry.partition(",")
        urls[label.strip().lower()] = url.strip()
    for label in ("source", "source code", "code", "repository", "homepage", "home"):
        if label in urls:
            return urls[label]
    return next(iter(urls.values()), "")


def python_components(pkg):
    dists = sorted((pkg / PYTHON_DIR).glob("*.dist-info"))
    if not dists:
        fail(f"no *.dist-info in {PYTHON_DIR}")
    comps = []
    for d in dists:
        meta = Parser().parsestr((d / "METADATA").read_text(encoding="utf-8"), headersonly=True)
        name, version = meta["Name"], meta["Version"]
        license_id = python_license(name, meta)
        if not license_id:
            fail(f"Python package {name} {version}: no determinable license")
        files = sorted(p for p in (d / "licenses").rglob("*") if p.is_file()) if (d / "licenses").is_dir() else []
        if not files:
            files = [d / f for f in meta.get_all("License-File") or [] if (d / f).is_file()]
        if not files and name in PY_TEXT_FALLBACKS:
            files = [pkg / PY_TEXT_FALLBACKS[name]]
        if not files or not all(f.is_file() for f in files):
            fail(f"Python package {name} {version}: no license text")
        texts = [(f.name, f.read_text(encoding="utf-8", errors="replace")) for f in files]
        comps.append({"name": name, "version": version, "license": license_id,
                      "url": python_url(meta), "texts": texts})
    return comps


def cell(s):
    return (s or "").replace("|", "\\|")


def table(comps):
    rows = ["| Component | Version | License | Source |", "|---|---|---|---|"]
    rows += [f"| {cell(c['name'])} | {cell(c['version'])} | {cell(c['license'])} | {cell(c['url'])} |"
             for c in comps]
    return "\n".join(rows)


def block(text):
    fence = "```"
    while fence in text:
        fence += "`"
    return f"{fence}text\n{text.rstrip()}\n{fence}"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    pkg = Path(sys.argv[1])
    java = java_components(pkg)
    precheck = java_components(pkg, PRECHECK_JAR)
    z3_license = pkg / Z3_LICENSE
    if not z3_license.is_file():
        fail(f"native Z3: missing license text {Z3_LICENSE}")
    z3_jar = next((c for c in java if c["name"] == "com.microsoft:z3"), None)
    if z3_jar is None:
        fail("native Z3: no com.microsoft:z3 entry in the jar's component list")
    native = [{"name": "Z3 (z3, libz3.so, libz3java.so)", "version": z3_jar["version"],
               "license": "MIT", "url": "https://github.com/Z3Prover/z3"}]
    python = python_components(pkg)

    out = [
        "# Third-Party Licenses",
        "",
        "This file lists the third-party components bundled in this SWAT package and their licenses.",
        "It is generated by `scripts/svcomp-package/third_party_licenses.py` while packaging; do not edit it.",
        "SWAT itself is licensed under the terms in `LICENSE`.",
        "",
        "## Java (shaded into `symbolic-executor/lib/symbolic-executor.jar`)",
        "",
        "JavaSMT is also shipped unshaded as `libs/java-library-path/java-smt-latest.jar`, and the Z3 Java",
        "bindings as `libs/java-library-path/com.microsoft.z3.jar`.",
        "",
        table(java),
        "",
        f"## Java (bundled in `{PRECHECK_JAR}`, the property pre-check from cfg-extraction)",
        "",
        table(precheck),
        "",
        "## Native (`libs/java-library-path/`)",
        "",
        table(native),
        "",
        "## Python (`libs/python/`)",
        "",
        table(python),
        "",
        "## License texts",
        "",
        "### Java",
        "",
    ]
    for spdx in sorted({i for c in java + precheck for i in c["ids"]}):
        users = ", ".join(dict.fromkeys(c["name"] for c in java + precheck if spdx in c["ids"]))
        out += [f"#### {spdx}", "", f"Used by: {users}", "",
                block((LICENSE_DIR / f"{spdx}.txt").read_text(encoding="utf-8")), ""]
    out += ["### Native", "", "#### Z3", "", block(z3_license.read_text(encoding="utf-8")), "",
            "### Python", ""]
    for c in python:
        for fname, text in c["texts"]:
            out += [f"#### {c['name']} {c['version']}: {fname}", "", block(text), ""]
    target = pkg / "THIRD-PARTY-LICENSES.md"
    target.write_text("\n".join(out), encoding="utf-8")
    print(f"Wrote {target}: {len(java) + len(precheck)} Java, {len(native)} native, {len(python)} Python components")


if __name__ == "__main__":
    main()
