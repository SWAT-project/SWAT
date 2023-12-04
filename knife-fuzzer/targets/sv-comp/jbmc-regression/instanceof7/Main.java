/*
 * Origin of the benchmark:
 *     license: 4-clause BSD (see /java/jbmc-regression/LICENSE)
 *     repo: https://github.com/diffblue/cbmc.git
 *     branch: develop
 *     directory: regression/cbmc-java/instanceof7
 * The benchmark was taken from the repo: 24 January 2018
 */
class Main {
  public static void main(String[] args) {
    A[] as = {new A(), new B()};
    assert (!(as[0] instanceof B));
    assert (as[1] instanceof B);
  }
}
;

class A {}

class B extends A {}
