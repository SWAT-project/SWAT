package de.uzl.its.swat.symbolicRegression;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

import java.util.*;

public class FormulaParser {


    // Method to tokenize the input formula
    public List<String> tokenize(String formula) {
        List<String> tokens = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < formula.length(); i++) {
            if (formula.charAt(i) == '(' || formula.charAt(i) == ')' || formula.charAt(i) == ',') {
                if (start < i) {
                    tokens.add(formula.substring(start, i).trim());
                }
                tokens.add(String.valueOf(formula.charAt(i)));
                start = i + 1;
            }
        }
        if (start < formula.length()) {
            tokens.add(formula.substring(start).trim());
        }
        return tokens;
    }

    Value astToSmt(Node node, FormulaManager fmgr, Value input) {
        System.out.println("Node: " + node.getType());

        // If the node has children, recursively call this method on the children
        // For nodes with children, you need to recursively process them
        for (Node child : node.children) {
            if (child.getOutput() == null) {
                astToSmt(child, fmgr, input);
            }
        }


        Value a, b, c;
        FloatingPointFormula aFormula, bFormula, cFormula;
        FloatingPointFormulaManager fpmgr = fmgr.getFloatingPointFormulaManager();
        IntegerFormulaManager imgr = fmgr.getIntegerFormulaManager();
        switch (node.getType()) {
            case "real_compare_as_int":
                // Handle real_compare_as_int
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                FloatValue aFloat = a.asFloatValue();
                FloatValue bFloat = b.asFloatValue();

                aFormula = fpmgr
                        .round(aFloat.formula, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
                bFormula = fpmgr
                        .round(bFloat.formula, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);

                boolean concrete = Math.round(aFloat.concrete) == Math.round(bFloat.concrete);
                BooleanValue result = new BooleanValue(input.context, concrete, fpmgr.equalWithFPSemantics(aFormula, bFormula));
                node.setOutput(result);

                break;
            case "real_sub":
                // Handle real_sub
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asFloatValue().FSUB(b.asFloatValue()));
                break;
            case "real_mul":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asFloatValue().FMUL(b.asFloatValue()));

                break;
            case "real_abs":
                if (node.getChildren().size() != 1) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                aFormula = fpmgr.abs((FloatingPointFormula) a.formula);
                node.setOutput(new FloatValue(input.context, Math.abs(a.asFloatValue().concrete), aFormula));
                break;
            case "real_less_than":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();

                node.setOutput(
                        new BooleanValue(
                                input.context,
                                a.asFloatValue().concrete < b.asFloatValue().concrete,
                                fpmgr.lessThan(a.asFloatValue().formula, b.asFloatValue().formula))
                );
                break;
            case "real_div":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asFloatValue().FDIV(b.asFloatValue()));
                break;
            case "real_add":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asFloatValue().FADD(b.asFloatValue()));
                break;
            case "real_floor":
                if (node.getChildren().size() != 1) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                aFormula = fpmgr
                        .round(a.asFloatValue().formula, FloatingPointRoundingMode.TOWARD_NEGATIVE); // TODO: check rounding mode
                node.setOutput(new FloatValue(input.context, (float) Math.floor(a.asFloatValue().concrete), aFormula));
                break;
            case "logic_or":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(new BooleanValue(input.context,
                        a.asBooleanValue().concrete || b.asBooleanValue().concrete,
                        fmgr.getBooleanFormulaManager().or(a.asBooleanValue().formula, b.asBooleanValue().formula)));
                break;
            case "logic_and":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(new BooleanValue(input.context,
                        a.asBooleanValue().concrete && b.asBooleanValue().concrete,
                        fmgr.getBooleanFormulaManager().and(a.asBooleanValue().formula, b.asBooleanValue().formula)));
                break;

            case "logic_not":
                if (node.getChildren().size() != 1) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();

                node.setOutput(new BooleanValue(input.context,
                        !a.asBooleanValue().concrete,
                        fmgr.getBooleanFormulaManager().not(a.asBooleanValue().formula)));
                break;
            case "logic_xor":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }

                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();

                node.setOutput(new BooleanValue(input.context,
                        a.asBooleanValue().concrete ^ b.asBooleanValue().concrete,
                        fmgr.getBooleanFormulaManager().xor(a.asBooleanValue().formula, b.asBooleanValue().formula)));
                break;
            case "int_compare_range":
                if (node.getChildren().size() != 3) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }
                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                c = node.getChildren().get(2).getOutput();

                node.setOutput(new BooleanValue(input.context,
                        a.asIntValue().concrete >= b.asIntValue().concrete && a.asIntValue().concrete <= c.asIntValue().concrete,
                        fmgr.getBooleanFormulaManager().and(
                                fmgr.getIntegerFormulaManager().greaterOrEquals(a.asIntValue().formula, b.asIntValue().formula),
                                fmgr.getIntegerFormulaManager().lessOrEquals(a.asIntValue().formula, c.asIntValue().formula)
                        )));
                break;
            case "int_sub":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }

                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asIntValue().ISUB(b.asIntValue()));
                break;
            case "int_mul":
                if (node.getChildren().size() != 2) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }

                a = node.getChildren().get(0).getOutput();
                b = node.getChildren().get(1).getOutput();
                node.setOutput(a.asIntValue().IMUL(b.asIntValue()));
                break;
            case "int_to_string":
                if (node.getChildren().size() != 1) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }

                a = node.getChildren().get(0).getOutput();
                node.setOutput(a.asIntValue().asStringValue());
                break;
            case "int_abs":
                if (node.getChildren().size() != 1) {
                    throw new RuntimeException("Unexpected case: " + node.getChildren().size());
                }

                a = node.getChildren().get(0).getOutput();
                // if then else
                BooleanFormula intAbsGE = imgr.greaterOrEquals(a.asIntValue().formula, imgr.makeNumber(0));
                node.setOutput(new IntValue(input.context, Math.abs(a.asIntValue().concrete), fmgr.getBooleanFormulaManager().ifThenElse(intAbsGE, a.asIntValue().formula, imgr.negate(a.asIntValue().formula))));
                break;
            case "leaf":

                if (node.getValue().startsWith("ARG")) {
                    if (input.getClass().equals(StringValue.class)) {
                        node.setOutput(input.asStringValue());
                    } else if (input.getClass().equals(BooleanValue.class)) {
                        node.setOutput(input.asBooleanValue());
                    } else if (input.getClass().equals(FloatValue.class)) {
                        node.setOutput(input.asFloatValue());
                    } else {
                        // Default -> input as int
                        //System.out.println("Input as int: " + input.asIntValue().concrete);
                        node.setOutput(input.asIntValue());
                    }
                } else if(node.getValue().equals("One")) {
                    node.setOutput(new FloatValue(input.context, 1.0f));
                } else {
                    // Parse constant
                    if (node.getValue().contains(".")) {
                        //System.out.println("Float constant: " + node.getValue());
                        node.setOutput(new FloatValue(input.context, Float.parseFloat(node.getValue())));
                    } else {
                        //System.out.println("Int constant: " + node.getValue());
                        node.setOutput(new IntValue(input.context, Integer.parseInt(node.getValue())));
                    }
                }
                break;
            default:
                throw new RuntimeException("Unexpected case: " + node.type);
        }

        return node.output;
    }

    // Method to parse tokens
    public Node parse(List<String> tokens) {

        // check if it is a leaf node, i.e. if it contains "("
        if (!tokens.contains("(")) {

            tokens = tokens.stream().filter(token -> !token.equals(",")).toList();

            if (tokens.size() != 1) {
                throw new RuntimeException("Unexpected case: " + String.join("    ", tokens));
            }

            Node n = new Node("leaf");
            n.setValue(tokens.get(0));
            return n;
        }

        // Output tokens
        //System.out.println("Tokens: " + String.join("    ", tokens));

        String nodeType = tokens.get(0);
        int startingIndex = 1;

        // find end index of node, i.e. closing bracket
        int endIndex = 0;
        int openBrackets = 0;
        ArrayList<ArrayList<String>> parameters = new ArrayList<>();
        for (int i = startingIndex; i < tokens.size(); i++) {
            String token = tokens.get(i);
            //padString(openBrackets);
            //System.out.println(token);
            if (token.equals("(")) {
                openBrackets++;
                if (openBrackets == 1) {
                    startingIndex = i + 1;
                }
            } else if (token.equals(")")) {
                if (openBrackets == 1) {
                    endIndex = i;
                    break;
                } else {
                    openBrackets--;
                }
            } else if (openBrackets == 1 && token.equals(",")) {
                endIndex = i;

                System.out.println("Starting index: " + startingIndex);
                System.out.println("End index: " + endIndex);

                // slice parameters
                ArrayList<String> parameter = new ArrayList<>();
                for (int j = startingIndex; j < endIndex; j++) {
                    System.out.println("Adding parameter: " + tokens.get(j));
                    parameter.add(tokens.get(j));
                }

                // repair enclosed paramerers by ( * )
                if (!parameter.isEmpty() && parameter.get(0).equals("(") && parameter.get(parameter.size() - 1).equals(")")) {
                    parameter.remove(0);
                    parameter.remove(parameter.size() - 1);
                }

                parameters.add(parameter);
                startingIndex = i + 1;
            }
        }

        // add last parameter
        ArrayList<String> lastParameter = new ArrayList<>();
        //System.out.println("Starting index: " + startingIndex);
        //System.out.println("End index: " + endIndex);
        //System.out.println("Tokens size: " + tokens.size());
        if (startingIndex == 1)
            startingIndex = 2; // if there is no nesting, the starting index is 2
        for (int j = startingIndex; j < endIndex; j++) {
            System.out.println("Adding final parameter: " + tokens.get(j));
            lastParameter.add(tokens.get(j));
        }
        parameters.add(lastParameter);

        // debug
        Node n = new Node(nodeType);
        for (ArrayList<String> parameter : parameters) {
            if (parameter.size() == 0)
                continue; // in case of empty parameter
            n.addChild(parse(parameter));
        }

        return n;
    }


    private void padString(int cnt) {
        for (int i = 0; i < cnt; i++) {
            System.out.print("    ");
        }
    }
}
