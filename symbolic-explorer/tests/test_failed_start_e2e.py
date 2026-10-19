"""
End-to-end reproducer: an SV-COMP run whose target JVM fails before main (a VerifyError from
inconsistently compiled classes, or a missing main class) is UNKNOWN, while a non-symbolic program
that runs to completion stays SAFE.

It runs SymbolicExplorer.py with the executor jar (build it with `../gradlew copyJar` in
symbolic-executor/) and the Z3 libraries in libs/java-library-path, and is skipped if either or
javac is missing. Run it with the harness venv, from symbolic-explorer/:
    ../targets/sv-comp/scripts/.venv/bin/python3 -m unittest tests.test_failed_start_e2e -v
"""

import json
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

EXPLORER = Path(__file__).resolve().parent.parent
ROOT = EXPLORER.parent
AGENT = ROOT / 'symbolic-executor/lib/symbolic-executor.jar'
Z3_DIR = ROOT / 'libs/java-library-path'
Z3_JAR = Z3_DIR / 'com.microsoft.z3.jar'
CONFIG = ROOT / 'targets/sv-comp/sv-comp.cfg'


def free_port():
    with socket.socket() as s:
        s.bind(('localhost', 0))
        return s.getsockname()[1]


def write(path: Path, text: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


@unittest.skipUnless(AGENT.exists() and Z3_JAR.exists() and shutil.which('javac') and shutil.which('java'),
                     'needs the executor jar, the Z3 libraries, java and javac')
class FailedStartTest(unittest.TestCase):

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)

    def javac(self, out: Path, *sources: Path):
        subprocess.run(['javac', '-d', str(out)] + [str(s) for s in sources], check=True)

    def verdict(self, target_dir: Path) -> dict:
        logdir = self.tmp / 'run'
        logdir.mkdir()
        cmd = [sys.executable, '-u', str(EXPLORER / 'SymbolicExplorer.py'), '-prp', 'valid-assert.prp',
               '--agent', str(AGENT), '--config', str(CONFIG), '-z3', str(Z3_DIR), '--logdir', str(logdir),
               '--mode', 'sv-comp', '--port', str(free_port()), '--target', 'Main',
               '--classpath', str(target_dir), str(Z3_JAR)]
        # The explorer ends itself with SIGTERM once the verdict is written
        subprocess.run(cmd, cwd=logdir, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
        return json.loads((logdir / 'stats.json').read_text())

    def test_verify_error_is_unknown(self):
        # B extends A when Main is compiled, but not at run time: verifying Main fails before main
        src = self.tmp / 'src'
        write(src / 'A.java', 'public class A { public int f() { return 1; } }')
        write(src / 'B.java', 'public class B extends A { }')
        write(src / 'Main.java', 'public class Main { public static void main(String[] args) { A a = new B(); assert a.f() == 1; } }')
        write(src / 'v2/B.java', 'public class B { }')
        out = self.tmp / 'classes'
        self.javac(out, src / 'A.java', src / 'B.java', src / 'Main.java')
        self.javac(out, src / 'v2/B.java')
        stats = self.verdict(out)
        self.assertEqual(stats['verdict'], 'UNKNOWN')
        self.assertIn('no_trace', [e['kind'] for e in stats['execution_errors']])

    def test_missing_main_class_is_unknown(self):
        empty = self.tmp / 'empty'
        empty.mkdir()
        stats = self.verdict(empty)
        self.assertEqual(stats['verdict'], 'UNKNOWN')

    def test_non_symbolic_program_is_safe(self):
        src = self.tmp / 'src'
        write(src / 'Main.java', 'public class Main { public static void main(String[] args) { int x = 3; assert x == 3; } }')
        out = self.tmp / 'classes'
        self.javac(out, src / 'Main.java')
        stats = self.verdict(out)
        self.assertEqual(stats['verdict'], 'SAFE')
        self.assertEqual(stats['execution_errors'], [])


if __name__ == '__main__':
    unittest.main()
