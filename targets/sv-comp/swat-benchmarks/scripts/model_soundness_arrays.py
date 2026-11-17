#!/usr/bin/env python3
"""Writes the model-soundness tasks for the Arrays.equals/hashCode, StringBuilder.equals and Logger
models (java/model-soundness/arrays-*, sb-equals-*, logger-*). SAFE/VIOLATION pairs: a wrong or
too-eager model turns a VIOLATION task into a false SAFE; the `_true` tasks are SAFE only if the
model is exact, except those marked "must stay UNKNOWN", which no exact model can prove.

    ./model_soundness_arrays.py      (then ./build.sh model-soundness; ./check_labels.py model-soundness)
"""
from pathlib import Path

SUITE = Path(__file__).resolve().parent.parent / 'java' / 'model-soundness'

YML = """format_version: '2.0'
input_files:
  - ../common/
  - {name}/
properties:
  - property_file: ../properties/valid-assert.prp
    expected_verdict: {verdict}
options:
  language: Java
"""


def task(name, comment, inputs, imports, body, extra=''):
    safe = name.endswith('_true')
    head = ('// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): ' if safe
            else '// Expected verdict: VIOLATION. Violating inputs (nondet call order): ') + inputs
    lines = [head] + [f'// {c}' for c in comment.strip().splitlines()]
    src = '\n'.join(lines) + '\n'
    src += ''.join(f'import {i};\n' for i in ['org.sosy_lab.sv_benchmarks.Verifier'] + imports)
    src += '\npublic class Main {\n' + extra + '    public static void main(String[] args) {\n'
    src += ''.join(f'        {l}\n' for l in body.strip().splitlines())
    src += '    }\n}\n'
    d = SUITE / name
    d.mkdir(parents=True, exist_ok=True)
    (d / 'Main.java').write_text(src)
    (SUITE / f'{name}.yml').write_text(YML.format(name=name, verdict='true' if safe else 'false'))


def main():
    A = ['java.util.Arrays']
    # ---- Arrays.equals
    for v, b1 in (('false', '5'), ('true', '6')):
        task(f'arrays-equals-int_{v}',
             'Arrays.equals(int[], int[]): equal iff every element is. The SAFE variant differs in a\n'
             'concrete element (5 vs 6), so no x, y make the arrays equal.',
             'int:3 int:7', A, f"""
int x = Verifier.nondetInt();
int y = Verifier.nondetInt();
int[] a = {{x, 5, 7}};
int[] b = {{3, {b1}, y}};
if (Arrays.equals(a, b)) assert false;""")
    for v, b1 in (('false', '255'), ('true', '254')):
        task(f'arrays-equals-byte_{v}',
             'Arrays.equals(byte[], byte[]) with sign-extended elements: (byte) 255 == -1. The SAFE\n'
             'variant has (byte) 254 == -2 against -1.',
             'byte:-128', A, f"""
byte x = Verifier.nondetByte();
byte[] a = {{x, (byte) -1}};
byte[] b = {{(byte) -128, (byte) {b1}}};
if (Arrays.equals(a, b)) assert false;""")
    task('arrays-equals-mixed_false',
         'Arrays.equals for char[], long[] and boolean[]; the long must be 2^32 (its upper half).',
         'char:Z long:4294967296 boolean:true', A, """
char c = Verifier.nondetChar();
long v = Verifier.nondetLong();
boolean z = Verifier.nondetBoolean();
if (Arrays.equals(new char[] {c}, new char[] {'Z'}) && Arrays.equals(new long[] {v}, new long[] {4294967296L})
        && Arrays.equals(new boolean[] {z}, new boolean[] {true})) assert false;""")
    task('arrays-equals-mixed_true',
         'As arrays-equals-mixed_false, but the low half of the long must also be 1: long equality is\n'
         '64-bit, so v == 2^32 has (int) v == 0 and the assert is unreachable.',
         'char:Z long:4294967296 boolean:true', A, """
char c = Verifier.nondetChar();
long v = Verifier.nondetLong();
boolean z = Verifier.nondetBoolean();
if (Arrays.equals(new char[] {c}, new char[] {'Z'}) && Arrays.equals(new long[] {v}, new long[] {4294967296L})
        && Arrays.equals(new boolean[] {z}, new boolean[] {true}) && (int) v == 1) assert false;""")
    for v, box in (('false', 'Integer.valueOf(5)'), ('true', 'Long.valueOf(5)')):
        task(f'arrays-equals-obj_{v}',
             'Arrays.equals(Object[], Object[]) over String, Integer and null elements. In the SAFE\n'
             'variant the second element is a Long: Integer.equals(Long) is false for every value.',
             'string:ab int:5', A, f"""
String s = Verifier.nondetString();
int x = Verifier.nondetInt();
Object[] a = {{s, Integer.valueOf(x), null}};
Object[] b = {{"ab", {box}, null}};
if (Arrays.equals(a, b)) assert false;""")
    task('arrays-equals-symlen_false',
         'Arrays.equals with an array of symbolic length (not modelled: stays a context loss).',
         'int:2', A, """
int n = Verifier.nondetInt();
if (n < 0 || n > 3) return;
int[] a = new int[n];
int[] b = new int[2];
if (Arrays.equals(a, b)) assert false;""")
    task('arrays-equals-symlen_true',
         'Must stay UNKNOWN: Arrays.equals with an array of symbolic length is not modelled. (SAFE:\n'
         'equal arrays have equal lengths, so n == 2.)',
         'int:2', A, """
int n = Verifier.nondetInt();
if (n < 0 || n > 3) return;
int[] a = new int[n];
int[] b = new int[2];
if (Arrays.equals(a, b) && n != 2) assert false;""")
    # ---- Arrays.hashCode
    task('arrays-hash-overflow_false',
         'Arrays.hashCode(int[]) = 31 * (31 + x) + 1 wraps around: x = 69273666 > 0 gives a\n'
         'negative hash (an unbounded-integer model would say it cannot).',
         'int:69273666', A, """
int x = Verifier.nondetInt();
int h = Arrays.hashCode(new int[] {x, 1});
if (x > 0 && h < 0) assert false;""")
    task('arrays-hash-overflow_true',
         'For 0 < x < 69273635, 962 + 31 * x <= 2147483616 does not overflow, so the hash stays\n'
         'positive.',
         'int:69273666', A, """
int x = Verifier.nondetInt();
int h = Arrays.hashCode(new int[] {x, 1});
if (x > 0 && x < 69273635 && h < 0) assert false;""")
    task('arrays-hash-long_false',
         'Arrays.hashCode(long[]): Long.hashCode(v) = (int) (v ^ (v >>> 32)); v = 2^32 hashes to 1,\n'
         'so h == 32 although (int) v == 0 (a model using (int) v would say SAFE).',
         'long:4294967296', A, """
long v = Verifier.nondetLong();
int h = Arrays.hashCode(new long[] {v});
if (h == 32 && (int) v == 0) assert false;""")
    task('arrays-hash-long_true',
         'v with (int) v == 0 and v >>> 32 == 1 is 2^32, whose hash is 31 + 1 = 32, not 31.',
         'long:4294967296', A, """
long v = Verifier.nondetLong();
int h = Arrays.hashCode(new long[] {v});
if (h == 31 && (int) v == 0 && (v >>> 32) == 1) assert false;""")
    task('arrays-hash-boolean_false',
         'Arrays.hashCode(boolean[]): Boolean.hashCode is 1231 for true, 1237 for false.',
         'boolean:true', A, """
boolean z = Verifier.nondetBoolean();
int h = Arrays.hashCode(new boolean[] {z});
if (h == 1262) assert false;""")
    task('arrays-hash-boolean_true',
         'The hash of a one-element boolean[] is 31 + 1231 or 31 + 1237, nothing else.',
         'boolean:true', A, """
boolean z = Verifier.nondetBoolean();
int h = Arrays.hashCode(new boolean[] {z});
if (h != 1262 && h != 1268) assert false;""")
    O = ['java.util.Objects']
    task('arrays-hash-boxed_false',
         'Objects.hash(Integer, Boolean, null, Long) is Arrays.hashCode(Object[]): 2255468 for\n'
         'x = 5, z = true, v = 2^32.',
         'int:5 boolean:true long:4294967296', O, """
int x = Verifier.nondetInt();
boolean z = Verifier.nondetBoolean();
long v = Verifier.nondetLong();
if (Objects.hash(x, z, null, v) == 2255468) assert false;""")
    task('arrays-hash-boxed_true',
         'With z = false, x = 5 and v = 2^32 fixed the hash is 2261234 (1237 instead of 1231),\n'
         'never 2255468.',
         'int:5 boolean:true long:4294967296', O, """
int x = Verifier.nondetInt();
boolean z = Verifier.nondetBoolean();
long v = Verifier.nondetLong();
if (!z && x == 5 && v == 4294967296L && Objects.hash(x, z, null, v) == 2255468) assert false;""")
    task('arrays-hash-string_false',
         'Arrays.hashCode(Object[]) with a String element (String.hashCode is not modelled: stays a\n'
         'context loss). {"a"} hashes to 31 + 97.',
         'string:a', A, """
String s = Verifier.nondetString();
if (Arrays.hashCode(new Object[] {s}) == 128) assert false;""")
    task('arrays-hash-string_true',
         'Must stay UNKNOWN (String.hashCode is not modelled). SAFE: a one-char string hashing to\n'
         '97 is "a".',
         'string:a', A, """
String s = Verifier.nondetString();
if (Arrays.hashCode(new Object[] {s}) == 128 && s.length() == 1 && !s.equals("a")) assert false;""")
    # ---- StringBuilder/StringBuffer.equals is identity
    task('sb-equals-string_true',
         'StringBuilder does not override equals: a builder never equals a String, whatever its\n'
         'chars (jbmc-regression/StringBuilderConstructors02).',
         'string:ab', [], """
String s = Verifier.nondetString();
StringBuilder sb = new StringBuilder(s);
if (sb.equals(s)) assert false;""")
    task('sb-equals-string_false',
         'Sanity pair: the builder holds the same chars as the String.',
         'string:ab', [], """
String s = Verifier.nondetString();
StringBuilder sb = new StringBuilder(s);
if (!sb.equals(s) && sb.toString().equals("ab")) assert false;""")
    task('sb-equals-identity_false',
         'StringBuilder.equals is identity: the same builder equals itself (f true), another one with\n'
         'the same chars does not.',
         'string:ab boolean:true', [], """
String s = Verifier.nondetString();
boolean f = Verifier.nondetBoolean();
StringBuilder sb = new StringBuilder(s);
StringBuilder other = f ? sb : new StringBuilder(s);
if (sb.equals(other) && s.length() == 2) assert false;""")
    task('sb-equals-identity_true',
         'StringBuffer.equals is identity: a different buffer with the same chars is never equal.',
         'string:ab boolean:true', [], """
String s = Verifier.nondetString();
boolean f = Verifier.nondetBoolean();
StringBuffer sb = new StringBuffer(s);
StringBuffer other = new StringBuffer(s);
if (f && sb.equals(other) && s.length() == 2) assert false;""")
    # ---- Logger sinks with application code or readable output behind them
    L = ['java.util.logging.Handler', 'java.util.logging.LogRecord', 'java.util.logging.Logger']
    task('logger-handler_false',
         'A custom Handler (application code, run by Logger.warning) asserts on the message: an\n'
         'ignored or context-free Logger call must not hide it.',
         'string:boom', L, """
Logger logger = Logger.getLogger("swat.model-soundness");
logger.setUseParentHandlers(false);
logger.addHandler(new Handler() {
    public void publish(LogRecord r) {
        if (r.getMessage().equals("boom")) assert false;
    }
    public void flush() {}
    public void close() {}
});
String s = Verifier.nondetString();
logger.warning(s);""")
    task('logger-stream_false',
         'A JDK StreamHandler writes the message into a buffer that the program reads back: the\n'
         'Logger call stores the symbolic message.',
         'string:boom', ['java.io.ByteArrayOutputStream', 'java.util.logging.Logger',
                         'java.util.logging.SimpleFormatter', 'java.util.logging.StreamHandler'], """
Logger logger = Logger.getLogger("swat.model-soundness.stream");
logger.setUseParentHandlers(false);
ByteArrayOutputStream out = new ByteArrayOutputStream();
StreamHandler handler = new StreamHandler(out, new SimpleFormatter());
logger.addHandler(handler);
String s = Verifier.nondetString();
logger.warning(s);
handler.flush();
if (out.toString().contains("boom")) assert false;""")


if __name__ == '__main__':
    main()
