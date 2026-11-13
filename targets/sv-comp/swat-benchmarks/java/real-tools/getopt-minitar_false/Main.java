// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:-xv string:-f string:x
import gnu.getopt.Getopt;
import org.sosy_lab.sv_benchmarks.Verifier;

// A small tar-like CLI on GNU getopt (java-getopt 1.0.13, unmodified). Three nondet argv
// strings. Help, version, usage errors and the create action leave without reaching the assert;
// only extracting with a verbose flag and the archive name "x" does.
public class Main {
    static int verbose;
    static boolean create, extract, list;
    static String file;
    static String level;

    public static void main(String[] args) {
        String[] argv = new String[3];
        for (int i = 0; i < argv.length; i++) {
            argv[i] = Verifier.nondetString();
        }
        Getopt g = new Getopt("minitar", argv, ":hVvcxtf:z::");
        g.setOpterr(false);
        int c;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h': usage(0); return;
                case 'V': version(); return;
                case 'v': verbose++; break;
                case 'c': create = true; break;
                case 'x': extract = true; break;
                case 't': list = true; break;
                case 'f': file = g.getOptarg(); break;
                case 'z': level = g.getOptarg(); break;
                case ':': usage(2); return;   // missing argument
                default: usage(1); return;    // '?': unknown option
            }
        }
        int actions = (create ? 1 : 0) + (extract ? 1 : 0) + (list ? 1 : 0);
        if (actions != 1) {
            usage(3);
            return;
        }
        if (file == null) {
            usage(4);
            return;
        }
        if (create) {
            doCreate(file, level);
        } else if (list) {
            doList(file);
        } else {
            doExtract(file, g.getOptind(), argv.length);
        }
    }

    static int code;

    static void usage(int why) {
        if (why == 0) code = 0;
        else if (why == 2) code = 64;
        else code = 65 + why;
    }

    static void version() {
        code = verbose > 0 ? 1 : 0;
    }

    static void doCreate(String archive, String lvl) {
        int l = 6;
        if (lvl != null && lvl.length() == 1) {
            char d = lvl.charAt(0);
            if (d >= '0' && d <= '9') l = d - '0';
        }
        if (archive.length() > 3) l++;
        code = l;
    }

    static void doList(String archive) {
        if (archive.startsWith("-")) code = 1;
        else code = archive.length();
    }

    static void doExtract(String archive, int optind, int argc) {
        if (verbose > 0 && archive.equals("x")) {
            if (optind == argc) assert false;
        }
        code = optind < argc ? 1 : 0;
    }
}
