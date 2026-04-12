package de.uzl.its.targets.finance_engine.util;

import java.util.ArrayList;
import java.util.List;

public class CsvParser {
    public static List<List<String>> parse(String content) {
        List<List<String>> out = new ArrayList<List<String>>();
        int i = 0;
        int n = content.length();
        StringBuilder field = new StringBuilder();
        List<String> row = new ArrayList<String>();
        boolean inQuotes = false;
        while (i < n) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2; continue;
                    } else {
                        inQuotes = false; i++; continue;
                    }
                } else {
                    field.append(c); i++; continue;
                }
            } else {
                if (c == '"') { inQuotes = true; i++; continue; }
                else if (c == ',') { row.add(field.toString()); field.setLength(0); i++; continue; }
                else if (c == '\r') { i++; continue; }
                else if (c == '\n') { row.add(field.toString()); field.setLength(0); out.add(row); row = new ArrayList<String>(); i++; continue; }
                else { field.append(c); i++; continue; }
            }
        }
        row.add(field.toString());
        out.add(row);
        return out;
    }
}
