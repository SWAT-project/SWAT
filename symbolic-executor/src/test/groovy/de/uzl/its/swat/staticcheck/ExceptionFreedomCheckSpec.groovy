package de.uzl.its.swat.staticcheck

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path

/**
 * Compiles small programs and checks the verdict of the exception-freedom check. The programs
 * that may throw an uncaught RuntimeException must never pass; the others document what the
 * check is able to prove.
 */
class ExceptionFreedomCheckSpec extends Specification {

    static final String VERIFIER = '''
package org.sosy_lab.sv_benchmarks;
public final class Verifier {
  public static void assume(boolean c) { if (!c) Runtime.getRuntime().halt(1); }
  public static boolean nondetBoolean() { return new java.util.Random().nextBoolean(); }
  public static int nondetInt() { return new java.util.Random().nextInt(); }
  public static long nondetLong() { return new java.util.Random().nextLong(); }
  public static double nondetDouble() { return new java.util.Random().nextDouble(); }
  public static String nondetString() { return "" + new java.util.Random().nextInt(); }
  public static <T> T nondetObject(Class<T> t, ObjectFactory<T> f) { return f.createObject(); }
}
'''
    static final String FACTORY = '''
package org.sosy_lab.sv_benchmarks;
public interface ObjectFactory<T> { T createObject(); }
'''

    @TempDir
    Path dir

    ExceptionFreedomCheck.Result check(String main, String release = '8') {
        Path src = dir.resolve('src'), out = dir.resolve('classes')
        Files.createDirectories(src.resolve('org/sosy_lab/sv_benchmarks'))
        Files.createDirectories(out)
        Files.writeString(src.resolve('org/sosy_lab/sv_benchmarks/Verifier.java'), VERIFIER)
        Files.writeString(src.resolve('org/sosy_lab/sv_benchmarks/ObjectFactory.java'), FACTORY)
        Files.writeString(src.resolve('Main.java'), 'import org.sosy_lab.sv_benchmarks.Verifier;\n' + main)
        def files = []
        src.toFile().eachFileRecurse { if (it.name.endsWith('.java')) files << it.path }
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                (['--release', release, '-nowarn', '-d', out.toString()] + files) as String[])
        assert rc == 0
        return ExceptionFreedomCheck.check([out])
    }

    @Unroll
    def "may throw: #name"() {
        expect:
        !check(program, release).exceptionFree

        where:
        name                                  | release | program
        'division by input'                   | '8'     | 'public class Main { public static void main(String[] a) { int x = Verifier.nondetInt(); int y = 10 / x; } }'
        'long division by zero constant'      | '8'     | 'public class Main { public static void main(String[] a) { long x = Verifier.nondetLong(); long y = x / 0L; } }'
        'array index from input'              | '8'     | 'public class Main { public static void main(String[] a) { int[] arr = new int[3]; arr[Verifier.nondetInt()] = 1; } }'
        'array index past the end'            | '8'     | 'public class Main { public static void main(String[] a) { int[] arr = new int[3]; int i = 2; i++; arr[i] = 1; } }'
        'array index in a loop'               | '8'     | 'public class Main { public static void main(String[] a) { int[] arr = new int[3]; for (int i = 0; i < 4; i++) arr[i] = 1; } }'
        'negative array size'                 | '8'     | 'public class Main { public static void main(String[] a) { int[] arr = new int[Verifier.nondetInt()]; } }'
        'field never assigned'                | '8'     | 'class B { void g() {} } class A { B b; } public class Main { public static void main(String[] a) { new A().b.g(); } }'
        'field assigned on one path'          | '8'     | 'class B { void g() {} } class A { B b; A(boolean c) { if (c) b = new B(); } } public class Main { public static void main(String[] a) { new A(Verifier.nondetBoolean()).b.g(); } }'
        'field set to null later'             | '8'     | 'class B { void g() {} } class A { B b = new B(); } public class Main { public static void main(String[] a) { A x = new A(); if (Verifier.nondetBoolean()) x.b = null; x.b.g(); } }'
        'read in overridden method from super constructor' | '8' | 'class B { void g() {} } class P { P() { init(); } void init() {} } class C extends P { B b = new B(); void init() { b.g(); } } public class Main { public static void main(String[] a) { new C(); } }'
        'receiver escapes before assignment'  | '8'     | 'class B { void g() {} } class Reg { static A last = new A(1); } class A { B b; A(int x) { Reg.last = this; int y = 10 / x; b = new B(); } } public class Main { public static void main(String[] a) { try { new A(Verifier.nondetInt()); } catch (Exception e) {} Reg.last.b.g(); } }'
        'static field read before assignment' | '8'     | 'class B { void g() {} } class T { static B peek() { return S.b; } } class S { static B a = T.peek(); static B b = new B(); } public class Main { public static void main(String[] a) { S.a.g(); } }'
        'static field never assigned'         | '8'     | 'class B { void g() {} } class S { static B b; } public class Main { public static void main(String[] a) { S.b.g(); } }'
        'null argument'                       | '8'     | 'class B { void g() {} } public class Main { static void f(B b) { b.g(); } public static void main(String[] a) { f(Verifier.nondetBoolean() ? new B() : null); } }'
        'null return value'                   | '8'     | 'class B { void g() {} } public class Main { static B make() { return Verifier.nondetBoolean() ? new B() : null; } public static void main(String[] a) { make().g(); } }'
        'interface implementation throws'     | '8'     | 'interface I { void f(); } class A implements I { public void f() { throw new IllegalStateException(); } } class C implements I { public void f() {} } public class Main { public static void main(String[] a) { I i = Verifier.nondetBoolean() ? new A() : new C(); i.f(); } }'
        'toString callback through valueOf'   | '8'     | 'class A { public String toString() { throw new IllegalStateException(); } } public class Main { public static void main(String[] a) { String s = String.valueOf(new A()); } }'
        'toString callback in concatenation'  | '8'     | 'class A { public String toString() { throw new IllegalStateException(); } } public class Main { public static void main(String[] a) { String s = "x" + new A(); } }'
        'toString callback through indy'      | '17'    | 'class A { public String toString() { throw new IllegalStateException(); } } public class Main { public static void main(String[] a) { String s = "x" + new A(); } }'
        'rethrow of caught exception'         | '8'     | 'public class Main { public static void main(String[] a) { try { int y = 1 / Verifier.nondetInt(); } catch (ArithmeticException e) { throw e; } } }'
        'finally after a throwing body'       | '8'     | 'public class Main { static int n; public static void main(String[] a) { try { int y = 1 / Verifier.nondetInt(); } finally { n++; } } }'
        'inner handler rethrows'              | '8'     | 'public class Main { public static void main(String[] a) { try { try { int y = 1 / Verifier.nondetInt(); } catch (Exception e) { throw new IllegalStateException(); } } catch (ArithmeticException e) {} } }'
        'static initializer throws'           | '8'     | 'class S { static int x = 10 / Verifier.nondetInt(); } public class Main { public static void main(String[] a) { int y = S.x; } }'
        'array store of wrong type'           | '8'     | 'public class Main { public static void main(String[] a) { Object[] arr = new String[1]; arr[0] = new Object(); } }'
        'failing cast'                        | '8'     | 'public class Main { public static void main(String[] a) { Object o = Verifier.nondetBoolean() ? (Object) "s" : (Object) new Object(); String s = (String) o; } }'
        'unboxing null'                       | '8'     | 'public class Main { public static void main(String[] a) { Integer i = Verifier.nondetBoolean() ? null : 3; int x = i; } }'
        'callback from a caught library call' | '8'     | 'class B { void g() {} } class H { static B b = new B(); } class Cmp implements java.util.Comparator<Object> { public int compare(Object x, Object y) { H.b = (B) x; return 0; } } public class Main { public static void main(String[] a) { try { java.util.Arrays.sort(new Object[] {null, null}, new Cmp()); } catch (Exception e) {} H.b.g(); } }'
        'lambda'                              | '8'     | 'public class Main { public static void main(String[] a) { Runnable r = () -> { int y = 1 / Verifier.nondetInt(); }; r.run(); } }'
        'runtime exception subclass thrown'   | '8'     | 'class E extends RuntimeException {} public class Main { public static void main(String[] a) { if (Verifier.nondetBoolean()) throw new E(); } }'
        'private method of abstract class'    | '17'    | 'public class Main { static abstract class P { private void m() { int z = Verifier.nondetInt(); int q = 1 / z; } void call() { m(); } } static class Q extends P { void m() {} } public static void main(String[] a) { new Q().call(); } }'
        'nondetObject'                        | '8'     | 'class B { void g() {} } public class Main { public static void main(String[] a) { B b = Verifier.nondetObject(B.class, () -> new B()); b.g(); } }'
        'finalizer'                           | '8'     | 'class B { protected void finalize() {} } public class Main { public static void main(String[] a) { new B(); } }'
        'unmodelled library call'             | '8'     | 'public class Main { public static void main(String[] a) { String s = Verifier.nondetString(); char c = s.charAt(3); } }'
        'println of an object'                | '8'     | 'class A { public String toString() { throw new IllegalStateException(); } } public class Main { public static void main(String[] a) { System.out.println(new A()); } }'
        'null string argument'                | '8'     | 'public class Main { public static void main(String[] a) { String s = Verifier.nondetBoolean() ? null : "x"; boolean b = "abc".startsWith(s); } }'
        'main called with null'               | '8'     | 'public class Main { public static void main(String[] a) { if (Verifier.nondetBoolean()) { main(null); } else { int n = a.length; } } }'
        'more specific default method'        | '8'     | 'interface I { default int m() { return 0; } } interface J extends I { default int m() { throw new IllegalStateException(); } } class C implements I, J {} public class Main { public static void main(String[] a) { new C().m(); } }'
        'private method does not implement'   | '17'    | 'interface I { default void m() { throw new IllegalStateException(); } } class B { private void m() {} } class C extends B implements I {} public class Main { public static void main(String[] a) { I x = new C(); x.m(); } }'
        'receiver escapes through a join'     | '8'     | 'interface G { String get(); } class G0 implements G { public String get() { return "ok"; } } class Holder { G g; Holder() { this.g = new G0(); } } abstract class Base implements G { Base() { G o = Verifier.nondetBoolean() ? this : Main.H.g; Main.H.g = o; } } class C extends Base { String f; C(int d) { super(); int z = 10 / d; this.f = "x"; } public String get() { return this.f; } } public class Main { static Holder H = new Holder(); public static void main(String[] a) { try { new C(Verifier.nondetInt()); } catch (RuntimeException e) { } String s = H.g.get(); int n = s.length(); } }'
        'concat overflows on JDK 8'           | '8'     | 'public class Main { public static void main(String[] a) { String s = Verifier.nondetString(); String t = s.concat(s); } }'
        'StringBuilder constructor overflows' | '8'   | 'public class Main { public static void main(String[] a) { StringBuilder sb = new StringBuilder(Verifier.nondetString()); } }'
        'library default method'              | '8'     | 'class It implements java.util.Iterator<Object> { public boolean hasNext() { return false; } public Object next() { return null; } } public class Main { public static void main(String[] a) { if (Verifier.nondetInt() == 42) new It().remove(); } }'
        'library default calls back'          | '17'    | 'class X implements CharSequence { public int length() { throw new IllegalStateException(); } public char charAt(int i) { return 0; } public CharSequence subSequence(int s, int e) { return this; } public String toString() { return ""; } } public class Main { public static void main(String[] a) { if (Verifier.nondetBoolean()) { boolean b = new X().isEmpty(); } } }'
        'library default via app interface'   | '8'     | 'interface J extends java.util.Iterator<Object> {} abstract class Ab implements J { public boolean hasNext() { return false; } } class It extends Ab { public Object next() { return null; } } public class Main { public static void main(String[] a) { J j = new It(); j.remove(); } }'
    }

    @Unroll
    def "proven exception-free: #name"() {
        expect:
        check(program, release).exceptionFree

        where:
        name                                 | release | program
        'arithmetic and an assert'           | '8'     | 'public class Main { public static void main(String[] a) { double x = Verifier.nondetDouble(); Verifier.assume(x > 0); double y = Math.sin(x) * x; assert y < 10; } }'
        'division by a constant'             | '8'     | 'public class Main { public static void main(String[] a) { int x = Verifier.nondetInt(); int d = 4; int y = x / d + x % 3; } }'
        'array with constant indices'        | '8'     | 'public class Main { public static void main(String[] a) { double[] arr = new double[] {1, 2, 3}; arr[2] = arr[0] + Verifier.nondetDouble(); } }'
        'fields assigned in the constructor' | '8'     | 'class B { boolean on; boolean isOn() { return on; } } class A { B b; B c; A() { b = new B(); c = b; } void step() { if (b.isOn()) c.on = false; } } public class Main { public static void main(String[] a) { A x = new A(); while (Verifier.nondetBoolean()) x.step(); } }'
        'enum switch'                        | '8'     | 'enum L { LOW, HIGH } class S { L l = L.LOW; void up() { switch (l) { case LOW: l = L.HIGH; break; default: break; } } } public class Main { public static void main(String[] a) { S s = new S(); if (Verifier.nondetBoolean()) s.up(); assert s.l != L.HIGH; } }'
        'string switch on input'             | '8'     | 'public class Main { public static void main(String[] a) { String s = Verifier.nondetString(); int r; switch (s) { case "a": r = 1; break; default: r = 0; } } }'
        'recursion'                          | '8'     | 'public class Main { static int f(int n) { return n <= 0 ? 0 : 1 + f(n - 1); } public static void main(String[] a) { int r = f(Verifier.nondetInt()); assert r >= 0; } }'
        'caught division'                    | '8'     | 'public class Main { public static void main(String[] a) { try { int y = 1 / Verifier.nondetInt(); } catch (ArithmeticException e) { assert false; } } }'
        'finally after a safe body'          | '8'     | 'public class Main { static int n; public static void main(String[] a) { try { n = Verifier.nondetInt(); } finally { n++; } } }'
        'checked exception'                  | '8'     | 'public class Main { public static void main(String[] a) throws Exception { if (Verifier.nondetBoolean()) throw new Exception(); } }'
        'printing'                           | '8'     | 'public class Main { public static void main(String[] a) { int x = Verifier.nondetInt(); System.out.println("x=" + x); } }'
        'static field from initializer'      | '8'     | 'public class Main { static String s = "abc"; static int[] t = new int[2]; public static void main(String[] a) { t[1] = s.length(); } }'
        'string concatenation with indy'     | '17'    | 'public class Main { public static void main(String[] a) { int x = Verifier.nondetInt(); String s = "x=" + x + Verifier.nondetString(); assert s.length() > 0; } }'
    }
}
