import au.com.bytecode.opencsv.CSVParser;
import java.io.IOException;
import org.sosy_lab.sv_benchmarks.Verifier;

// A CSV import step on opencsv 2.3's CSVParser (unmodified). One nondet input line. Malformed
// lines (unterminated quote) and records without exactly three fields are rejected; the assert
// checks a property of well-formed three-field records.
public class Main {
    static int rejected;

    public static void main(String[] args) {
        String line = Verifier.nondetString();
        CSVParser parser = new CSVParser();
        String[] fields;
        try {
            fields = parser.parseLine(line);
        } catch (IOException e) {
            reject(line);
            return;
        }
        if (fields.length != 3) {
            reject(line);
            return;
        }
        if (@COND@) assert false;
    }

    static void reject(String line) {
        if (line.length() > 10) rejected += 2;
        else rejected++;
    }
}
