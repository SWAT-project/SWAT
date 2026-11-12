#!/usr/bin/env python3
"""Generates java/real-tools-facts/: the real-tools tasks with hand-written facts (an experiment,
see RESULTS.md, "Hand-written facts on real-tools"). Needs the fetched tools (fetch_tools.sh).

The suite is gitignored and NOT part of regular runs: getopt-wrong_false carries a deliberately
wrong fact and comes out a false SAFE. Generate it, run it, delete it:

    scripts/real_tools_facts.py && scripts/build.sh real-tools-facts
    ../scripts/svcomp test run --benchmark-dir swat-benchmarks --suite real-tools-facts ...
    rm -rf java/real-tools-facts

Variants (all ask for --user-facts):
  getopt-harness_*   @Irrelevant on usage, version, doCreate, doList (they only set the exit code)
  getopt-wrong_false a WRONG fact: an irrelevant region around the option loop
  opencsv-region_*   CSVParser: an irrelevant region around the embedded-quote block (it only
                     changes a field's characters, not where fields end)
  opencsv-toarray_*  CSVParser: Swat.summary() copying the token list element by element in
                     place of List.toArray (which SWAT does not model)
  opencsv-both_*     both CSVParser facts
"""
import glob
import os
import shutil

JAVA = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'java')
SRC = os.path.join(JAVA, 'real-tools')
OUT = os.path.join(JAVA, 'real-tools-facts')

REGION = [('''                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    if (!strictQuotes) {''', '''                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    // FACT: only the field's characters depend on this block, not where fields end
                    de.uzl.its.swat.annotations.Swat.beginIrrelevant();
                    if (!strictQuotes) {'''),
          ('''                    }

                    inQuotes = !inQuotes;''', '''                    }
                    de.uzl.its.swat.annotations.Swat.endIrrelevant();

                    inQuotes = !inQuotes;''')]
TOARRAY = [('''        return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);''',
            '''        // FACT: the same as toArray, element by element, which SWAT can track
        if (de.uzl.its.swat.annotations.Swat.summary()) {
            String[] result = new String[tokensOnThisLine.size()];
            for (int k = 0; k < result.length; k++) {
                result[k] = ((java.util.ArrayList<String>) tokensOnThisLine).get(k);
            }
            return result;
        }
        return tokensOnThisLine.toArray(new String[tokensOnThisLine.size()]);''')]
HARNESS = [(f'    static void {m}', f'    @de.uzl.its.swat.annotations.Irrelevant("only sets the exit code")\n    static void {m}')
           for m in ('usage(int why)', 'version()', 'doCreate(String archive, String lvl)', 'doList(String archive)')]
WRONG = [('''        int c;
        while ((c = g.getopt()) != -1) {''', '''        int c;
        // A WRONG fact: the option parsing decides the flags the assertion depends on
        de.uzl.its.swat.annotations.Swat.beginIrrelevant();
        while ((c = g.getopt()) != -1) {'''),
         ('''        int actions = (create''', '''        de.uzl.its.swat.annotations.Swat.endIrrelevant();
        int actions = (create''')]


def edit(path, edits):
    with open(path) as f:
        s = f.read()
    for a, b in edits:
        assert s.count(a) == 1, (path, a[:50])
        s = s.replace(a, b)
    with open(path, 'w') as f:
        f.write(s)


def lib(name, base, edits):
    dst = os.path.join(OUT, name)
    shutil.copytree(os.path.join(SRC, base), dst, ignore=shutil.ignore_patterns('*.class'))
    if edits:
        edit(glob.glob(os.path.join(dst, '**', 'CSVParser.java'), recursive=True)[0], edits)


def task(name, libname, source, edits):
    os.makedirs(os.path.join(OUT, name))
    main = os.path.join(OUT, name, 'Main.java')
    shutil.copy(os.path.join(SRC, source, 'Main.java'), main)
    if edits:
        edit(main, edits)
    verdict = 'true' if name.endswith('_true') else 'false'
    with open(os.path.join(OUT, name + '.yml'), 'w') as f:
        f.write(f"""format_version: '2.0'
input_files:
  - ../common/
  - {libname}/
  - {name}/
properties:
  - property_file: ../properties/valid-assert.prp
    expected_verdict: {verdict}
options:
  language: Java
  swat-user-facts: true
""")


def main():
    if not os.path.isdir(os.path.join(SRC, 'lib-opencsv')):
        raise SystemExit('fetch the tools first: scripts/fetch_tools.sh')
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)
    lib('lib-getopt', 'lib-getopt', None)
    lib('lib-opencsv-region', 'lib-opencsv', REGION)
    lib('lib-opencsv-toarray', 'lib-opencsv', TOARRAY)
    lib('lib-opencsv-both', 'lib-opencsv', REGION + TOARRAY)
    for v in ('true', 'false'):
        task(f'getopt-harness_{v}', 'lib-getopt', f'getopt-minitar_{v}', HARNESS)
        for kind in ('region', 'toarray', 'both'):
            task(f'opencsv-{kind}_{v}', f'lib-opencsv-{kind}', f'opencsv-import_{v}', None)
    task('getopt-wrong_false', 'lib-getopt', 'getopt-minitar_false', WRONG)
    print(f'Generated {OUT}')


if __name__ == '__main__':
    main()
