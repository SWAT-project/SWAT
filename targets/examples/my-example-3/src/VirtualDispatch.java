import de.uzl.its.swat.annotations.Symbolic;

public class VirtualDispatch {

    interface Writer {
        void write(String s);
    }

    static class Checker {
        Writer writer() {
            return new Writer() {
                @Override
                public void write(String s) {
                    assert !s.isEmpty();
                }
            };
        }
    }

    public static void main(String[] args) {
        test("test");
    }

    private static void test(@Symbolic String msg) {
        Writer w = new Checker().writer();
        w.write(msg);
    }
}
