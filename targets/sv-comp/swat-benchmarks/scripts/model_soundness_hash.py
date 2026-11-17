#!/usr/bin/env python3
"""Writes the model-soundness tasks for the exact model of hash containers with symbolic keys
(java/model-soundness/hash-*, hashset-*, emptymap-*, list-equals*): HashMap, LinkedHashMap, ConcurrentHashMap,
HashSet, LinkedHashSet with
String and boxed integral keys, lookups as equality chains over the stored keys.

SAFE/VIOLATION pairs: a wrong or too-eager model turns a VIOLATION task into a false SAFE. A `_true`
task is SAFE only if the model is exact; those marked "must stay UNKNOWN" use something the model
must not cover (iteration order of a plain hash container, keys with their own equals, Double keys,
changes through a view or by library code), where SAFE on the `_false` twin would be a false SAFE.

    ./model_soundness_hash.py      (then ./build.sh model-soundness; ./check_labels.py model-soundness)
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


def task(name, comment, inputs, body, extra=''):
    safe = name.endswith('_true')
    head = ('// Expected verdict: SAFE. Paired VIOLATION inputs (must not fail here): ' if safe
            else '// Expected verdict: VIOLATION. Violating inputs (nondet call order): ') + inputs
    lines = [head] + [f'// {c}' for c in comment.strip().splitlines()]
    src = '\n'.join(lines) + '\n'
    src += 'import org.sosy_lab.sv_benchmarks.Verifier;\nimport java.util.*;\n'
    src += '\npublic class Main {\n' + extra + '    public static void main(String[] args) {\n'
    src += ''.join(f'        {l}\n' for l in body.strip().splitlines())
    src += '    }\n}\n'
    d = SUITE / name
    d.mkdir(parents=True, exist_ok=True)
    (d / 'Main.java').write_text(src)
    (SUITE / f'{name}.yml').write_text(YML.format(name=name, verdict='true' if safe else 'false'))


def pair(name, comment, inputs, body, false_cond, true_cond, extra=''):
    """A VIOLATION and a SAFE task that differ only in the condition before `assert false`."""
    for verdict, cond in (('false', false_cond), ('true', true_cond)):
        task(f'{name}_{verdict}', comment, inputs, body.strip() + f'\nif ({cond}) assert false;', extra)


def main():
    pair('hash-collide',
         'Two symbolic keys that may or may not be equal: put(a), put(b) leaves one entry iff\n'
         'a.equals(b), and then get(a) is the second value. SAFE: with one entry, get(a) is 2.',
         'string:k string:k', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put(a, 1);
m.put(b, 2);""",
         'a.equals("k") && m.size() == 1 && m.get(a) == 2',
         'a.equals("k") && m.size() == 1 && m.get(a) != 2')
    pair('hash-overwrite',
         'put with a symbolic key overwrites the entry of an equal concrete key; get of the concrete\n'
         'key then returns the new value. SAFE: only the key "key" overwrites it.',
         'string:key', """
String k = Verifier.nondetString();
Map<String, String> m = new HashMap<>();
m.put("key", "old");
String before = m.put(k, "new");""",
         'm.get("key").equals("new") && before != null',
         'm.get("key").equals("new") && !k.equals("key")')
    pair('hash-remove-size',
         'remove with a symbolic Integer key, then size and containsKey. SAFE: only x == 3 removes\n'
         'the entry of 3.',
         'int:3', """
int x = Verifier.nondetInt();
Map<Integer, String> m = new HashMap<>();
m.put(1, "a");
m.put(2, "b");
m.put(3, "c");
String removed = m.remove(x);""",
         'm.size() == 2 && !m.containsKey(3) && removed.equals("c")',
         'm.size() == 2 && !m.containsKey(3) && x != 3')
    pair('hash-contains-missing',
         'containsKey of a symbolic key the map does not hold: the path must say it differs from\n'
         'every stored key. SAFE: a one-char string in [a, b] is a key.',
         'string:c', """
String s = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("a", 1);
m.put("b", 2);""",
         "!m.containsKey(s) && s.length() == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'c'",
         "!m.containsKey(s) && s.length() == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'b'")
    for v, box, cond in (('false', 'Integer', 'm.size() == 1'), ('true', 'Long', 'm.size() == 1 || !"int".equals(m.get(5))')):
        task(f'hash-int-long_{v}',
             'Integer and Long keys: an Integer never equals a Long of the same value. The VIOLATION\n'
             'variant puts Integer.valueOf(x), which replaces the entry of 5 for x == 5; the SAFE one\n'
             'puts Long.valueOf(x), which is always a second entry.',
             'int:5', f"""
int x = Verifier.nondetInt();
Map<Object, String> m = new HashMap<>();
m.put(Integer.valueOf(5), "int");
m.put({box}.valueOf(x), "other");
if ({cond}) assert false;""")
    pair('hash-null-key',
         'The null key next to a symbolic String key: the String "null" is not the null key, and a\n'
         'nondet String is never null. SAFE: the null entry is never replaced.',
         'string:null', """
String s = Verifier.nondetString();
Map<String, String> m = new HashMap<>();
m.put(null, "n");
m.put(s, "s");""",
         'm.size() == 2 && m.get(null).equals("n") && m.containsKey("null")',
         'm.size() != 2 || !m.get(null).equals("n")')
    pair('hash-iterate',
         'Iteration of a HashMap with a symbolic key: the order depends on the key\'s hash code\n'
         '("b" comes before "c" but after "a" and ""), which no branch on the path says. Must stay\n'
         'UNKNOWN: an order taken as fixed makes the VIOLATION task a false SAFE.',
         'string:c', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("b", 1);
m.put(a, 2);
String first = m.keySet().iterator().next();""",
         'first.equals("b") && a.equals("c")',
         'first.equals("b") && a.equals("a")')
    pair('hash-iterate-values',
         'As hash-iterate, over values(): their order is the keys\' hash order too. Must stay UNKNOWN.',
         'string:c', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("b", 1);
m.put(a, 2);
int first = m.values().iterator().next();""",
         'first == 1 && a.equals("c")',
         'first == 1 && a.equals("a")')
    pair('hash-iterate-order',
         'The first value of a HashMap is the symbolic key\'s only if that key hashes before "b": true\n'
         'for "" and "p" (table index 0), false for "c" (index 3 after 2). No branch says so: a search\n'
         'that takes the order of one run as fixed sees first == 1 for every one-char key after \'b\'\n'
         'and reports the VIOLATION task SAFE. Must stay UNKNOWN (SAFE variant: \'c\' to \'o\' all\n'
         'come after "b").',
         'string:p', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("b", 1);
m.put(a, 2);
int first = m.values().iterator().next();""",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b'",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p'")
    pair('hashset-iterate-copy',
         'A HashSet with a symbolic element copied into a list: the list\'s order is the hash order.\n'
         'Must stay UNKNOWN.',
         'string:c', """
String a = Verifier.nondetString();
Set<String> s = new HashSet<>();
s.add("b");
s.add(a);
List<String> l = new ArrayList<>(s);""",
         'l.get(0).equals("b") && a.equals("c")',
         'l.get(0).equals("b") && a.equals("a")')
    pair('hash-linked-iterate',
         'Iteration of a LinkedHashMap with a symbolic key: insertion order, whatever the keys are.\n'
         'SAFE: "b" was put first, and putting it again does not move it.',
         'string:a', """
String a = Verifier.nondetString();
Map<String, Integer> m = new LinkedHashMap<>();
m.put("b", 1);
m.put(a, 2);
Iterator<String> it = m.keySet().iterator();
String first = it.next();""",
         'it.hasNext() && it.next().equals("a")',
         '!first.equals("b")')
    key = """    static final class Key {
        final int v;
        Key(int v) { this.v = v; }
        @Override public boolean equals(Object o) { return o instanceof Key && ((Key) o).v % 10 == v % 10; }
        @Override public int hashCode() { return v % 10; }
    }
"""
    pair('hash-user-key',
         'A key class with its own equals and hashCode (equal modulo 10): not a key the model\n'
         'compares. Must stay UNKNOWN (SAFE would need the hash code to be tracked into the map).',
         'int:13', """
int x = Verifier.nondetInt();
Map<Key, String> m = new HashMap<>();
m.put(new Key(3), "three");""",
         'x != 3 && m.containsKey(new Key(x))',
         'x >= 0 && x < 10 && x != 3 && m.containsKey(new Key(x))', extra=key)
    pair('hash-user-key-mixed',
         'A map with a symbolic String key and a key with its own equals: the model gives up at the\n'
         'second, and the map is not exact any more. Must stay UNKNOWN.',
         'string:s int:13', """
String s = Verifier.nondetString();
int x = Verifier.nondetInt();
Map<Object, String> m = new HashMap<>();
m.put(s, "string");
m.put(new Key(3), "three");
m.remove(new Key(x));""",
         's.equals("s") && x != 3 && m.size() == 1',
         's.equals("s") && x >= 0 && x < 10 && x != 3 && m.size() == 1', extra=key)
    pair('hash-double-key',
         'Double keys: Double.equals compares bits, so -0.0 is not the key 0.0 although -0.0 == 0.0.\n'
         'Not modelled. Must stay UNKNOWN.',
         'double:-0.0', """
double d = Verifier.nondetDouble();
Map<Double, Integer> m = new HashMap<>();
m.put(0.0, 1);""",
         '!m.containsKey(d) && d == 0.0',
         'm.containsKey(d) && d != 0.0')
    pair('hash-view-remove',
         'A key removed through the keySet view, which is not modelled: the map\'s exact entries are\n'
         'not known afterwards. Must stay UNKNOWN.',
         'string:k', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("k", 1);
m.keySet().remove(a);""",
         'm.isEmpty()',
         'm.isEmpty() && !a.equals("k")')
    pair('hash-view-remove-key',
         'As hash-view-remove, with the symbolic key in the map and a concrete one removed through\n'
         'the view. Must stay UNKNOWN.',
         'string:k', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put(a, 1);
m.keySet().remove("k");""",
         'm.isEmpty()',
         'm.isEmpty() && !a.equals("k")')
    pair('hash-iterator-remove',
         'An entry removed through an iterator: the map\'s exact entries are not known afterwards.\n'
         'Must stay UNKNOWN.',
         'string:k', """
String a = Verifier.nondetString();
Map<String, Integer> m = new LinkedHashMap<>();
m.put("k", 1);
m.put(a, 2);
Iterator<String> it = m.keySet().iterator();
it.next();
it.remove();""",
         'm.isEmpty()',
         'm.isEmpty() && !a.equals("k")')
    pair('hash-escape',
         'A set changed by library code that is not modelled (Collections.addAll): its exact\n'
         'elements are not known afterwards. Must stay UNKNOWN.',
         'string:p', """
String a = Verifier.nondetString();
Set<String> s = new HashSet<>();
s.add(a);
Collections.addAll(s, "p", "q");""",
         's.size() == 2',
         's.size() == 2 && !a.equals("p") && !a.equals("q")')
    for v, old in (('false', 'null'), ('true', '"old"')):
        task(f'hash-putifabsent_{v}',
             'putIfAbsent: a key mapped to null counts as absent and gets the new value (VIOLATION\n'
             'variant, a == "k"); a key with a value keeps it (SAFE variant).',
             'string:k', f"""
String a = Verifier.nondetString();
Map<String, String> m = new HashMap<>();
m.put("k", {old});
m.put("j", "J");
m.putIfAbsent(a, "new");
if ("new".equals(m.get("k"))) assert false;""")
        task(f'hash-getordefault_{v}',
             'getOrDefault: a key mapped to null gives null, not the default (VIOLATION variant,\n'
             'a == "k"); in the SAFE variant no value is null.',
             'string:k', f"""
String a = Verifier.nondetString();
Map<String, String> m = new HashMap<>();
m.put("k", {old});
String v = m.getOrDefault(a, "default");
if (v == null) assert false;""")
    pair('hash-copy',
         'Copy constructor and putAll with symbolic Integer keys: the copies hold the same entries,\n'
         'and putAll replaces the value of an equal key. SAFE: only y == 7 puts 2 under 7 (x == 7\n'
         'with y == x is excluded).',
         'int:1 int:7', """
int x = Verifier.nondetInt();
int y = Verifier.nondetInt();
Map<Integer, Integer> m = new HashMap<>();
m.put(x, 1);
m.put(y, 2);
Map<Integer, Integer> c = new HashMap<>(m);
Map<Integer, Integer> d = new HashMap<>();
d.put(7, 0);
d.putAll(c);""",
         'd.size() == 2 && d.get(7) == 2 && x != 7',
         'd.size() == 2 && d.get(7) == 2 && x != 7 && y != 7')
    pair('hash-clear',
         'clear, then a new entry: the keys from before are gone. SAFE: only "k" is a key.',
         'string:k', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put(a, 1);
m.clear();
m.put("k", 2);""",
         'm.containsKey(a) && m.size() == 1',
         'm.containsKey(a) && !a.equals("k")')
    pair('hash-nested',
         'A map of maps with symbolic keys: get returns the very map that was put, so what is put\n'
         'into it shows through the outer map. SAFE: only q == "id" makes "id" a key.',
         'string:p string:id', """
String p = Verifier.nondetString();
String q = Verifier.nondetString();
Map<String, Map<String, String>> outer = new HashMap<>();
outer.put(p, new HashMap<String, String>());
outer.get(p).put(q, "v");
Map<String, String> inner = outer.get(p);""",
         'inner.containsKey("id") && p.equals("p")',
         'inner.containsKey("id") && !q.equals("id")')
    pair('hash-char-key',
         'Character keys (boxed chars compare by value). SAFE: only \'b\' maps to 2.',
         'char:b', """
char c = Verifier.nondetChar();
Map<Character, Integer> m = new HashMap<>();
m.put('a', 1);
m.put('b', 2);
Integer v = m.get(c);""",
         'v != null && v == 2',
         "v != null && v == 2 && c != 'b'")
    pair('hash-char-literal',
         'A Character key from a char constant, looked up with a symbolic char that is not 0. The\n'
         'shadow of the boxed constant must be \'a\': with the default \'\\0\' (as before the\n'
         'constructor models took int-typed constants) a model that compares it puts c == 0 on the\n'
         'path instead of c == \'a\', and the VIOLATION task becomes a false SAFE. SAFE: only \'a\' is\n'
         'a key. The all-zero run ends early.',
         'char:a', """
char c = Verifier.nondetChar();
if (c == 0) return;
Map<Character, Integer> m = new HashMap<>();
m.put('a', 1);""",
         'm.containsKey(c)',
         "m.containsKey(c) && c != 'a'")
    pair('hash-short-literal',
         'As hash-char-literal, with a Short key from a constant. SAFE: only 7 is a key. The\n'
         'all-zero run ends early.',
         'short:7', """
short x = Verifier.nondetShort();
if (x == 0) return;
Map<Short, Integer> m = new HashMap<>();
m.put((short) 7, 1);""",
         'm.containsKey(x)',
         'm.containsKey(x) && x != 7')
    pair('hash-unknown-shadow',
         'A Short key built by a constructor SWAT does not model (from a String): its shadow keeps\n'
         'the default 0, so it must not be compared (comparing it puts x == 0 on the path instead of\n'
         'x == 7, and the VIOLATION task becomes a false SAFE). Must stay UNKNOWN. The all-zero run\n'
         'ends early.',
         'short:7', """
short x = Verifier.nondetShort();
if (x == 0) return;
Map<Short, Integer> m = new HashMap<>();
m.put(new Short("7"), 1);""",
         'm.containsKey(x)',
         'm.containsKey(x) && x != 7')
    pair('hash-boolean-key',
         'Boolean and Byte keys from constants, looked up with symbolic values. SAFE: only true and\n'
         '(byte) -3 are keys.',
         'boolean:true byte:-3', """
boolean b = Verifier.nondetBoolean();
byte y = Verifier.nondetByte();
Map<Object, Integer> m = new HashMap<>();
m.put(true, 1);
m.put((byte) -3, 2);""",
         'm.containsKey(b) && m.containsKey(y)',
         'm.size() != 2 || (m.containsKey(b) && !b) || (m.containsKey(y) && y != -3)')
    pair('hash-new-string',
         'A String key built by a constructor SWAT may not model (from bytes): its shadow must not\n'
         'be compared unless it is the string. SAFE or UNKNOWN on the SAFE task. The all-zero run ends\n'
         'early.',
         'string:hi', """
String s = Verifier.nondetString();
if (s.isEmpty()) return;
Map<String, Integer> m = new HashMap<>();
m.put(new String(new byte[] {104, 105}), 1);""",
         'm.containsKey(s)',
         'm.containsKey(s) && !s.equals("hi")')
    pair('hash-string-vs-int',
         'A String key and an Integer key are never equal ("5" is not 5). SAFE: the map always has\n'
         'two entries.',
         'string:5 int:5', """
String s = Verifier.nondetString();
int x = Verifier.nondetInt();
Map<Object, String> m = new HashMap<>();
m.put(s, "string");
m.put(x, "int");""",
         'm.size() == 2 && s.equals("5") && x == 5',
         'm.size() != 2')
    pair('emptymap-get',
         'Lookups in Collections.emptyMap() and emptySet() with a symbolic key: null and false for\n'
         'every key. SAFE: nothing is found.',
         'string:k', """
String s = Verifier.nondetString();
Map<String, String> m = Collections.emptyMap();
Set<String> e = Collections.emptySet();""",
         'm.get(s) == null && !e.contains(s) && s.equals("k")',
         'm.get(s) != null || m.containsKey(s) || e.contains(s)')
    pair('hash-concurrent',
         'A ConcurrentHashMap with symbolic String keys: put, putIfAbsent, get, getOrDefault and\n'
         'remove by key as in a HashMap. SAFE: "v1" stays only under a (putIfAbsent of an equal b\n'
         'changes nothing, and remove(b) then removes it).',
         'string:k string:k', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
Map<String, String> m = new java.util.concurrent.ConcurrentHashMap<>();
m.put(a, "v1");
String old = m.putIfAbsent(b, "v2");
String got = m.getOrDefault(b, "none");
m.remove(b);""",
         'old != null && got.equals("v1") && m.isEmpty() && a.equals("k")',
         'old != null && (!got.equals("v1") || !m.isEmpty() || !a.equals(b))')
    pair('hash-concurrent-iterate',
         'As hash-iterate-order, for a ConcurrentHashMap: its iteration order is the hash order too.\n'
         'Must stay UNKNOWN.',
         'string:p', """
String a = Verifier.nondetString();
Map<String, Integer> m = new java.util.concurrent.ConcurrentHashMap<>();
m.put("b", 1);
m.put(a, 2);
int first = m.values().iterator().next();""",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b'",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p'")
    pair('hash-linked-copy-order',
         'A LinkedHashMap copied from a HashMap with a symbolic key iterates in the HashMap\'s hash\n'
         'order, not in the order the keys were put (see hash-iterate-order). Must stay UNKNOWN.',
         'string:p', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put("b", 1);
m.put(a, 2);
Map<String, Integer> l = new LinkedHashMap<>(m);
int first = l.values().iterator().next();""",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b'",
         "first == 2 && a.length() == 1 && a.charAt(0) > 'b' && a.charAt(0) < 'p'")
    pair('hashset-aslist',
         'A HashSet built from Arrays.asList, a source the model does not copy from: the set is not\n'
         'modelled. Must stay UNKNOWN.',
         'string:z', """
String a = Verifier.nondetString();
Set<String> s = new HashSet<>(Arrays.asList(a, "z"));""",
         's.size() == 1',
         's.size() == 1 && !a.equals("z")')
    pair('hashset-keyset-copy',
         'addAll of a map\'s key set (with a symbolic key) into a set. SAFE: two elements are left\n'
         'only if a is "j" or "k".',
         'string:j', """
String a = Verifier.nondetString();
Map<String, Integer> m = new HashMap<>();
m.put(a, 1);
m.put("k", 2);
Set<String> s = new HashSet<>();
s.add("j");
s.addAll(m.keySet());""",
         's.size() == 2 && a.equals("j")',
         's.size() == 2 && !a.equals("j") && !a.equals("k")')
    pair('list-equals',
         'ArrayList.equals of two lists with symbolic String elements: equal iff the elements are,\n'
         'pair by pair. SAFE: only a == "k" and b == "m" make them equal.',
         'string:k string:m', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
List<String> x = new ArrayList<>();
x.add(a);
x.add("m");
List<String> y = new ArrayList<>();
y.add("k");
y.add(b);""",
         'x.equals(y) && a.length() == 1',
         'x.equals(y) && (!a.equals("k") || !b.equals("m"))')
    pair('list-equals-size',
         'Objects.equals of two lists of different sizes: never equal, whatever the elements are.\n'
         'SAFE: the lists differ.',
         'string:q string:q', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
List<String> x = new ArrayList<>();
x.add(a);
List<String> y = new ArrayList<>();
y.add(a);
y.add(b);""",
         '!Objects.equals(x, y) && a.equals("q") && x.equals(x)',
         'Objects.equals(x, y) || !x.equals(x)')
    pair('list-equals-changed',
         'equals of a list that library code SWAT does not model has changed (Collections.reverse):\n'
         'its elements are not known in order any more, and comparing the recorded ones puts\n'
         'a == "k" on the path instead of a == "z". Must stay UNKNOWN.',
         'string:z', """
String a = Verifier.nondetString();
List<String> x = new ArrayList<>();
x.add(a);
x.add("k");
Collections.reverse(x);
List<String> y = new ArrayList<>();
y.add("k");
y.add("z");""",
         'x.equals(y)',
         'x.equals(y) && !a.equals("z")')
    pair('hashset-add',
         'HashSet.add of a symbolic element returns whether it was new; contains of another symbolic\n'
         'one. SAFE: if a was not new, the set is {"x"} and holds b only for b == "x".',
         'string:x string:x', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
Set<String> s = new HashSet<>();
s.add("x");
boolean fresh = s.add(a);""",
         '!fresh && s.size() == 1 && s.contains(b) && b.length() == 1',
         '!fresh && s.contains(b) && !b.equals("x")')
    pair('hashset-remove',
         'HashSet.remove after adding a symbolic Integer. SAFE: one element is left only if x was\n'
         '1 or 2.',
         'int:2', """
int x = Verifier.nondetInt();
Set<Integer> s = new HashSet<>();
s.add(1);
s.add(2);
s.add(x);
boolean removed = s.remove(2);""",
         'removed && s.size() == 1 && x != 1',
         'removed && s.size() == 1 && x != 1 && x != 2')
    pair('hashset-copy',
         'A HashSet built from a list with symbolic elements (and addAll of another set): equal\n'
         'elements collapse. SAFE: one element is left only if a is "z" too.',
         'string:z string:z', """
String a = Verifier.nondetString();
String b = Verifier.nondetString();
List<String> l = new ArrayList<>();
l.add(a);
l.add(b);
l.add("z");
Set<String> s = new HashSet<>(l);
Set<String> t = new LinkedHashSet<>();
t.add("z");
boolean changed = t.addAll(s);""",
         '!changed && s.size() == 1 && t.size() == 1 && a.length() == 1',
         '!changed && !a.equals("z")')
    pair('hashset-list-changed',
         'A HashSet built from a list that a call SWAT does not model changed (remove(Object)): the\n'
         'list\'s elements are not known exactly, so neither are the set\'s. Must stay UNKNOWN.',
         'string:z', """
String a = Verifier.nondetString();
List<String> l = new ArrayList<>();
l.add("y");
l.add("z");
l.remove(a);
Set<String> s = new HashSet<>(l);""",
         's.size() == 1 && s.contains("y")',
         's.size() == 1 && s.contains("y") && !a.equals("z")')
    pair('hashset-linked-order',
         'A LinkedHashSet iterates in insertion order, also with symbolic elements. SAFE: "b" stays\n'
         'first.',
         'string:a', """
String a = Verifier.nondetString();
Set<String> s = new LinkedHashSet<>();
s.add("b");
s.add(a);
Iterator<String> it = s.iterator();
String first = it.next();""",
         'it.hasNext() && it.next().equals("a")',
         '!first.equals("b")')


if __name__ == '__main__':
    main()
