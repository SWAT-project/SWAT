import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STRawGroupDir;

import witness.WitnessAssumption;
import witness.WitnessEdge;
import witness.WitnessNode;

public class App {

    public static void main(String[] args) {


        String path = args[1];
        System.out.println("source path: " + path);

        List<WitnessAssumption> witness = new LinkedList<>();
        if(args[0].trim().length() == 0) {
            System.out.println("No witness record, assuming default assertion violation");
            (new App()).saveDefaultviolationWitness(path);
        } else {
            String param = new String(Base64.getDecoder().decode(args[0]));
            String[] lines = param.split("\n");
            for (String line : lines) {
                line = line.substring(line.indexOf("[WITNESS]") + 10).trim();
                // decode line base64
                byte[] decodedBytes = Base64.getDecoder().decode(line);
                line = new String(decodedBytes);

                String[] parts = line.split("@@@");
                if (parts.length != 4) {
                    System.out.println("Received line:" + line);
                    throw new RuntimeException("Invalid witness line: " + line);
                }

                String value = parts[0].trim();
                int lineNumber = Integer.parseInt(parts[1].trim()); // account for 0-based line numbers
                String className = parts[2].trim();
                String description = parts[3].trim();

                // Escape XML special characters in scope to avoid XML parse errors
                // (e.g., <init> contains angle brackets that break XML parsing)
                String scope = "L" + className + ";." + description;
                scope = scope.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                witness.add(new WitnessAssumption(value, className + ".java", scope, lineNumber));
            }
            (new App()).checkAndSaveWitness(path, witness);
            }
        }



    private void saveDefaultviolationWitness(String path){
        STGroup group = new STRawGroupDir("witnesses", '$','$');
        ST st = group.getInstanceOf("default_violation");
        String result = st.render();
        try {
            System.out.println("Writing default violation witness to file: " + path + "/" + "witness.graphml");
            Files.write(Paths.get(path + "/" + "witness.graphml"), result.getBytes());
        } catch (IOException e) {
            System.err.println("Error writing witness to file: " + e.getMessage());
        }
    }
    private void checkAndSaveWitness(String path, List<WitnessAssumption> witness) {


        List<WitnessNode> nodes = new ArrayList<>();
        List<WitnessEdge> edges = new ArrayList<>();

        int nodeId = 0;
        WitnessNode initNode = new WitnessNode(nodeId++);
        nodes.add(initNode);
        initNode.addData("entry", "true");
        WitnessNode curNode = initNode;

        for (WitnessAssumption wa : witness) {
            String loc = getLineOfCode(path, wa.getClazz(), wa.getLine());
            String assumption = computeAssumption(loc, wa);

            WitnessNode prevNode = curNode;
            curNode = new WitnessNode(nodeId++);
            nodes.add(curNode);
            WitnessEdge edge = new WitnessEdge(prevNode, wa, assumption, curNode);
            edges.add(edge);
        }

        curNode.addData("violation", "true");

        STGroup group = new STRawGroupDir("witnesses", '$','$');
        ST st = group.getInstanceOf("witness");
        st.add("nodes", nodes);
        st.add("edges", edges);
        String result = st.render();
        try {
            System.out.println("Writing violation witness to file: " + path + "/" + "witness.graphml");
            Files.write(Paths.get(path + "/" + "witness.graphml"), result.getBytes());
        } catch (IOException e) {
            System.out.println("Error writing witness to file: " + e.getMessage());
        }
    }

    private String getLineOfCode(String path, String filename, int line) {
        String absolutePath = path + "/" + filename;
        try {
            // Open absolutePath as a stream
            InputStream is = Files.newInputStream(Paths.get(absolutePath));
            if (is == null) {
                System.out.println("Could not find file: " + absolutePath);
                return null;
            }
            BufferedReader res = new BufferedReader(new InputStreamReader(is));
            String loc = "";
            for (int i = 0; i<line; i++) {
                loc = res.readLine();
            }
            System.out.println("Line of code: " + loc);
            return loc;
        } catch (IOException e) {
            System.out.println("Exception reading file: " + e.getMessage());
            return null;
        }
    }

    private String computeAssumption(String lineOfCode, WitnessAssumption wa) {
        if (lineOfCode == null) {
            return "true";
        }

        // Check if this is a string type by looking for nondetString() in the line
        boolean isStringType = lineOfCode.contains("nondetString()");

        int idx = lineOfCode.indexOf("Verifier");
        if (idx < 0) {
            return "true";
        }
        lineOfCode = lineOfCode.substring(0, idx).trim();
        idx = lineOfCode.lastIndexOf("=");
        if (idx < 0) {
            // No assignment found - this is likely a return statement or direct call
            // Use the witness value directly (e.g., "true", "false", or a literal value)
            return wa.getValue();
        }
        // Check if this is a comparison (==) not an assignment (=)
        // If the character before '=' is also '=', this is a comparison, not an assignment
        if (idx > 0 && lineOfCode.charAt(idx - 1) == '=') {
            // This is a comparison like "if (pos == ..." - no variable assignment
            return wa.getValue();
        }
        lineOfCode = lineOfCode.substring(0, idx).trim();
        String[] parts = lineOfCode.split(" ");
        String id = parts[parts.length-1].trim();

        // For string types, always use .equals() format with properly quoted value
        if (isStringType) {
            String value = wa.getValue();
            // Add quotes around the value if not already present
            if (!value.startsWith("\"")) {
                // Escape any quotes and backslashes within the string value
                value = value.replace("\\", "\\\\").replace("\"", "\\\"");
                value = "\"" + value + "\"";
            }
            return id + ".equals(" + value + ")";
        }

        // For primitive types, use assignment format
        return id + " = " + wa.getValue();
    }

} 