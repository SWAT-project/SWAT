import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STRawGroupDir;
import witness.WitnessAssumption;
import witness.WitnessEdge;
import witness.WitnessNode;

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

public class App {

    public static void main(String[] args) {

        // print cwd
        System.out.println("cwd: " + System.getProperty("user.dir"));

        List<WitnessAssumption> witness = new LinkedList<>();

        String param = new String(Base64.getDecoder().decode(args[0]));
        String[] lines = param.split("\n");
        for(String line: lines) {
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

            witness.add(new WitnessAssumption(value, className + ".java", "L" + className + ";." + description, lineNumber));
        }



        (new App()).checkAndSaveWitness(witness, "java.lang.AssertionError");
    }


    private void checkAndSaveWitness(List<WitnessAssumption> witness, String stderr) {

        if (!stderr.contains("java.lang.AssertionError") && !stderr.contains("error encountered")) {
            System.out.println("No witness applicable");
            return;
        }

        List<WitnessNode> nodes = new ArrayList<>();
        List<WitnessEdge> edges = new ArrayList<>();

        int nodeId = 0;
        WitnessNode initNode = new WitnessNode(nodeId++);
        nodes.add(initNode);
        initNode.addData("entry", "true");
        WitnessNode curNode = initNode;

        for (WitnessAssumption wa : witness) {
            String loc = getLineOfCode(wa.getClazz(), wa.getLine());
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
            Files.write(Paths.get("witness.graphml"), result.getBytes());
        } catch (IOException e) {
            System.err.println("Error writing witness to file: " + e.getMessage());
        }
    }

    private String getLineOfCode(String filename, int line) {
        String absolutePath = System.getProperty("user.dir") + "/" + filename;
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
            return null;
        }
    }

    private String computeAssumption(String lineOfCode, WitnessAssumption wa) {
        if (lineOfCode == null) {
            return "true";
        }
        int idx = lineOfCode.indexOf("Verifier");
        if (idx < 0) {
            return "true";
        }
        lineOfCode = lineOfCode.substring(0, idx).trim();
        idx = lineOfCode.lastIndexOf("=");
        if (idx < 0) {
            return "true";
        }
        lineOfCode = lineOfCode.substring(0, idx).trim();
        String[] parts = lineOfCode.split(" ");
        String id = parts[parts.length-1].trim();
        if (wa.getValue().contains("\"") && !wa.getValue().contains("parse")) {
            // its a string constant
            return "" + id + ".equals(" + wa.getValue() + ")";
        }
        return "" + id +" = " + wa.getValue();
    }

} 