package de.uzl.its.swat.symbolicRegression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import lombok.*;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
public class ProxyInvoke {

    private final String owner;
    private final String name;
    private final Type[] desc;
    private final Value<?, ?>[] args;
    private String descString = "";

    @SneakyThrows
    public Value invoke() {
        if (owner.contains("swat"))
            return null;

        System.out.println("Invoking: ");
        System.out.println("Owner: " + owner);
        System.out.println("Name: " + name);
        for (Type type : desc) {
            System.out.println("Type: " + type);
            descString += type.getDescriptor().replace("/", ".");
        }
        for (Value<?, ?> arg : args) {
            System.out.println("Arg: " + arg);
        }

        createTrainingData();
        //if (1==1)
        //    return null;

        if (args.length != 1 ||
                (!args[0].getClass().equals(IntValue.class)
                && !args[0].getClass().equals(CharValue.class))) {
            throw new RuntimeException("Unexpected case: " + args[0].getClass());
        }

        // Download formulas:  rsync -avz hyjavu@141.83.62.215:/home/hyjavu/playground/SMT-Gradient-Stuff/symbolic-regression-training/formulas ../
        String formulaPath = "/Users/felixmachtle/Programmieren/Uni/Symbolic_Regression_Fuzzer/symbolic-regression-training/formulas/" + owner.replace("/", ".") + "_" + name + "_" + descString + "_best_individual.json";

        // Check if formula exists
        if (Files.exists(java.nio.file.Paths.get(formulaPath))) {
            // Load formula
            Gson gson = new Gson();
            String json = Files.readString(java.nio.file.Paths.get(formulaPath));
            FormulaDTO formulaDTO = gson.fromJson(json, FormulaDTO.class);
            System.out.println("Loaded formula: " + formulaDTO.hof);
            System.out.println("File: " + formulaPath);

            if (formulaDTO.score > 0.1) {
                System.out.println("WARNING: Score is high: " + formulaDTO.score);
                return null;
            }

            FormulaParser formulaParser = new FormulaParser();
            List<String> tokens = formulaParser.tokenize(formulaDTO.hof);
            Node ast = formulaParser.parse(tokens);

            Value param = (Value) args[0];
            FormulaManager fmgr = param.context.getFormulaManager();
            Value resultingValue = formulaParser.astToSmt(ast, fmgr, param);
            return resultingValue;
        } else {
            System.out.println("No formula found for: " + formulaPath);
        }

        /*


        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //String json = gson.toJson(ast);
        //System.out.println("AST: " + json);

        System.out.println("Result: " + resultingValue);
        return resultingValue;
        */
         return null;
    }

    @SneakyThrows
    private void createTrainingData() {
        List<InputOutputPair<Object[], Object>> ts = TrainingDataGenerator.generateTrainingData(owner, name, new Class[]{int.class}, 2048, args[0].concrete);
        // export as gson
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(ts);
        String exportFilenamePath = "/Users/felixmachtle/Programmieren/Uni/Symbolic_Regression_Fuzzer/symbolic-regression-training/training/" + owner.replace("/", ".") + "_" + name + "_" + descString + ".json";
        Files.writeString(java.nio.file.Paths.get(exportFilenamePath), json);
    }
}
