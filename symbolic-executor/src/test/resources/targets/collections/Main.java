import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * Target for the agent test of the JDK container models. The first argument picks a scenario;
 * each one ends in a branch on the symbolic string, after the string went through a container.
 */
public class Main {
  static void branch(Object o) {
    if (((String) o).startsWith("x")) {
      System.out.println("x");
    }
  }

  public static void main(String[] args) {
    String s = Verifier.nondetString();
    switch (args[0]) {
      case "list": {
        LinkedList<String> l = new LinkedList<>();
        l.addLast(s);
        l.addFirst("a");
        branch(l.getLast());
        break;
      }
      case "iterator": {
        List<String> l = new ArrayList<>();
        l.add(s);
        List<String> copy = new ArrayList<>(l);
        for (Iterator<String> it = copy.iterator(); it.hasNext(); ) {
          branch(it.next());
        }
        break;
      }
      case "map": {
        Map<String, Object> m = new HashMap<>();
        m.put("k", s);
        for (Map.Entry<String, Object> e : m.entrySet()) {
          if (e.getKey().equals("k")) branch(e.getValue());
        }
        branch(m.get("k"));
        break;
      }
      case "array": {
        List<String> l = new ArrayList<>();
        l.add(s);
        Object[] a = l.toArray();
        branch(Arrays.asList(a).get(0));
        break;
      }
      case "contains": {
        List<String> l = new ArrayList<>();
        l.add(s);
        if (l.contains("x")) System.out.println("x");
        break;
      }
      case "symbolicKey": {
        Map<String, String> m = new HashMap<>();
        m.put(s, "v");
        branch(m.get("k") == null ? "y" : "x");
        break;
      }
      case "heldByUnmodelled": {
        List<String> l = new ArrayList<>();
        l.add(s);
        branch(String.join(",", l));
        break;
      }
      case "arraycopy": {
        int[] a = new int[4];
        int[] b = new int[4];
        a[1] = s.length();
        System.arraycopy(a, 0, b, 0, 4);
        if (b[1] > 2) System.out.println("long");
        break;
      }
      default:
        break;
    }
  }
}
