package de.uzl.its.swat.symbolicRegression;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrainingDataGenerator {


    private static Random random = new Random();

    public static List<InputOutputPair<Object[], Object>> generateTrainingData(String className, String methodName, Class<?>[] paramTypes, int samples, Object concreteValue) throws Exception {
        className = className.replace("/", ".");
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);

        // make sure the method is static
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("Method " + method + " is not static");
        }

        List<InputOutputPair<Object[], Object>> trainingData = new ArrayList<>();

        // use concrete value if available
        if (concreteValue != null) {
            Object[] args = new Object[paramTypes.length];
            for (int j = 0; j < paramTypes.length; j++) {
                args[j] = concreteValue;
            }

            Object output = method.invoke(null, args);
            trainingData.add(new InputOutputPair<>(args, output));
        }

        // generate random values
        for (int i = 0; i < samples; i++) {
            Object[] args = new Object[paramTypes.length];
            for (int j = 0; j < paramTypes.length; j++) {
                args[j] = randomValueForType(paramTypes[j]);
            }

            Object output = method.invoke(null, args);
            trainingData.add(new InputOutputPair<>(args, output));
        }

        return trainingData;
    }

    private static Object randomValueForType(Class<?> type) {
        // This method should be extended to handle more types as needed.
        if (type.equals(int.class)) {
            if (random.nextBoolean()) {
                return random.nextInt(0,255);
            }
            return random.nextInt();
        } else if (type.equals(double.class)) {
            if (random.nextBoolean()) {
                return random.nextDouble(0.0,255.0);
            }
            return random.nextDouble();
        } else if (type.equals(float.class)) {
            if (random.nextBoolean()) {
                return random.nextFloat(0.0f,255.0f);
            }
            return random.nextFloat();
        } else if (type.equals(long.class)) {
            if (random.nextBoolean()) {
                return random.nextLong(0,255);
            }
            return random.nextLong();
        } else if (type.equals(short.class)) {
            if (random.nextBoolean()) {
                return (short) random.nextInt(0,255);
            }
            return (short) random.nextInt();
        } else if (type.equals(byte.class)) {
            if (random.nextBoolean()) {
                return (byte) random.nextInt(0,255);
            }
            return (byte) random.nextInt();
        } else if (type.equals(boolean.class)) {
            return random.nextBoolean();
        } else if (type.equals(char.class)) {
            return (char) random.nextInt(256);
        } else if (type.equals(String.class)) {
            // generate random string
            int length = random.nextInt(10);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append((char) random.nextInt(256));
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("Type " + type + " not supported");
    }

}
