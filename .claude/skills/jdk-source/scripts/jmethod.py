#!/usr/bin/env python3
"""Cheaply fetch JDK source from the active JDK's lib/src.zip (version-exact, no network).

Usage:
  jmethod.py <class-fqn>                       # index: every method/ctor signature + line no.
  jmethod.py <class-fqn> <method>              # full source of all overloads of <method>
  jmethod.py <class-fqn> <method> --callees    # also list the methods each overload calls
  jmethod.py <class-fqn> --field <name>        # a field declaration (for static-final purity checks)

<class-fqn> is dotted, e.g. java.lang.String. Nested classes: java.lang.Integer (the file is the
top-level class; nested types appear within it). The JDK is resolved from $JAVA_HOME, else from
`java -XshowSettings:properties`. Override the module with --module (default: search all).
"""
import os
import re
import subprocess
import sys
import zipfile


def java_home() -> str:
    jh = os.environ.get("JAVA_HOME")
    if jh and os.path.exists(os.path.join(jh, "lib", "src.zip")):
        return jh
    out = subprocess.run(["java", "-XshowSettings:properties"],
                         capture_output=True, text=True).stderr
    m = re.search(r"java\.home\s*=\s*(.+)", out)
    if not m:
        sys.exit("Could not resolve java.home (set $JAVA_HOME).")
    return m.group(1).strip()


def load_source(fqn: str, module: str | None):
    zp = os.path.join(java_home(), "lib", "src.zip")
    if not os.path.exists(zp):
        sys.exit(f"No src.zip at {zp} — this JDK ships no sources.")
    rel = fqn.replace(".", "/") + ".java"
    with zipfile.ZipFile(zp) as z:
        cands = [n for n in z.namelist() if n.endswith(rel)
                 and (module is None or n.startswith(module + "/"))]
        if not cands:
            sys.exit(f"Not found in src.zip: {rel}"
                     + (f" (module {module})" if module else ""))
        entry = cands[0]
        return entry, z.read(entry).decode("utf-8", "replace").splitlines()


# A modest scanner: yields the source with strings/chars/comments blanked, so brace counting and
# signature detection ignore braces and identifiers inside literals/comments. Good enough for JDK src.
def blank_noncode(src: list[str]) -> list[str]:
    out, in_block = [], False
    for line in src:
        res, i, n = [], 0, len(line)
        while i < n:
            c = line[i]
            if in_block:
                if c == "*" and i + 1 < n and line[i + 1] == "/":
                    in_block = False
                    res.append("  "); i += 2; continue
                res.append(" "); i += 1; continue
            if c == "/" and i + 1 < n and line[i + 1] == "/":
                res.append(" " * (n - i)); break
            if c == "/" and i + 1 < n and line[i + 1] == "*":
                in_block = True
                res.append("  "); i += 2; continue
            if c in "\"'":
                q = c; res.append(" "); i += 1
                while i < n:
                    if line[i] == "\\":
                        res.append("  "); i += 2; continue
                    if line[i] == q:
                        res.append(" "); i += 1; break
                    res.append(" "); i += 1
                continue
            res.append(c); i += 1
        out.append("".join(res))
    return out


SIG = re.compile(r"^\s{1,8}(?:(?:public|private|protected|static|final|native|synchronized|"
                 r"abstract|default|strictfp)\s+)+[\w$.<>\[\]?,\s]*?\b(\w+)\s*\(")


def signatures(code: list[str]):
    """(line_index, method_name) for lines that look like a method/ctor declaration."""
    for i, line in enumerate(code):
        m = SIG.search(line)
        if m and "=" not in line.split("(")[0]:
            yield i, m.group(1)


def brace_extent(code: list[str], start: int):
    """From a signature at line `start`, return (end_index) of the matching close brace, or None
    for abstract/interface methods that end in ';' before any '{'."""
    depth, seen = 0, False
    for i in range(start, len(code)):
        for c in code[i]:
            if c == "{":
                depth += 1; seen = True
            elif c == "}":
                depth -= 1
                if seen and depth == 0:
                    return i
        if not seen and ";" in code[i]:
            return None  # no body (abstract/native-decl)
    return len(code) - 1


def main():
    args = [a for a in sys.argv[1:]]
    module = None
    if "--module" in args:
        k = args.index("--module"); module = args[k + 1]; del args[k:k + 2]
    callees = "--callees" in args
    if callees:
        args.remove("--callees")
    field = None
    if "--field" in args:
        k = args.index("--field"); field = args[k + 1]; del args[k:k + 2]
    if not args:
        sys.exit(__doc__)
    fqn = args[0]
    method = args[1] if len(args) > 1 else None

    entry, src = load_source(fqn, module)
    code = blank_noncode(src)
    print(f"// {entry}  ({len(src)} lines)")

    if field:
        for i, line in enumerate(code):
            if re.search(r"\b" + re.escape(field) + r"\b", line) and ("=" in line or ";" in line) \
                    and SIG.search(line) is None:
                print(f"{i+1}: {src[i].rstrip()}")
        return

    if method is None:
        print(f"// method/ctor index for {fqn}:")
        for i, name in signatures(code):
            print(f"{i+1}: {src[i].strip()}")
        return

    hits = [i for i, name in signatures(code) if name == method]
    if not hits:
        sys.exit(f"No declaration of {method}(...) found in {fqn}.")
    for start in hits:
        end = brace_extent(code, start)
        end = start if end is None else end
        print(f"\n// ---- {fqn}.{method}  (lines {start+1}-{end+1}) ----")
        print("\n".join(src[start:end + 1]))
        if callees:
            body = "\n".join(code[start:end + 1])
            names = sorted(set(re.findall(r"\b([a-zA-Z_]\w*)\s*\(", body))
                           - {method, "if", "for", "while", "switch", "catch", "return", "new"})
            print(f"// callees: {', '.join(names)}")


if __name__ == "__main__":
    main()
