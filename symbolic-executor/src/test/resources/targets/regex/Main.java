import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * Target for the agent test of the java.util.regex models. The first argument picks a scenario;
 * each one branches on a regex match of the symbolic string.
 */
public class Main {
  private static final Pattern AID = Pattern.compile("[0-9A-Fa-f]{10,32}\\*?\\#?");

  public static void main(String[] args) {
    String s = Verifier.nondetString();
    boolean r;
    switch (args[0]) {
      case "matches":
        r = AID.matcher(s).matches();
        break;
      case "patternMatches":
        r = Pattern.matches("[0-9]+", s);
        break;
      case "twice": {
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        r = m.matches() && m.matches();
        break;
      }
      case "find":
        r = Pattern.compile("[0-9]+").matcher(s).find();
        break;
      case "reset": {
        Matcher m = Pattern.compile("[0-9]+").matcher(s);
        m.reset("12");
        r = m.matches();
        break;
      }
      case "untranslatable":
        r = Pattern.compile("a.c").matcher(s).matches();
        break;
      case "flags":
        r = Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE).matcher(s).matches();
        break;
      default:
        throw new IllegalArgumentException(args[0]);
    }
    if (r) {
      System.out.println("match");
    } else {
      System.out.println("no match");
    }
  }
}
