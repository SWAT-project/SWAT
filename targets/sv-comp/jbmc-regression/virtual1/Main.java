/*
 * Origin of the benchmark:
 *     license: 4-clause BSD (see /java/jbmc-regression/LICENSE)
 *     repo: https://github.com/diffblue/cbmc.git
 *     branch: develop
 *     directory: regression/cbmc-java/virtual1
 * The benchmark was taken from the repo: 24 January 2018
 */
class A {
  public void f() {}
}
;

class B extends A {
  public void f() {
    assert false;
  }
}
;

class Main {
  public static void main(String[] args) {
    A a = new A();
    B b = new B();
    a.f();
  }
}
