package de.uzl.its.swat.common;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.exceptions.*;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.ValueFactory;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

import de.uzl.its.swat.thread.ThreadHandler;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;

import static java.lang.Thread.currentThread;

public class Util {

    public static final String DIVIDER = "-".repeat(80);
    public static final String FIELD_DIVIDER = "#";
    public static final String METHOD_DIVIDER = ":";
    private static final Config config = Config.instance();
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static final String[] includeList = config.getInstrumentationScope();

    private static final String[] excludeList = config.getInstrumentationExcludePackages();


    private static final String[] symbolicScope = config.getSymbolicScope().length > 0 ?
            config.getSymbolicScope() : config.getInstrumentationScope();

    private static final HashMap<String, Boolean> shouldInstrumentCache = new HashMap<>();

    // Maintain a set of class names that have been de-interned
    private static final Set<String> deInternedClasses = new HashSet<>(Arrays.asList(
            String.class.getName(), Boolean.class.getName(), Byte.class.getName(), Short.class.getName(),
            Character.class.getName(), Integer.class.getName(), Long.class.getName()
    ));


    /**
     * Just a method for validating the engine!
     *
     * @param type Expected datatype of the Value
     * @param val Value to be validated
     * @return True if the Value is of the provided Type
     */
    public static boolean isMatchingType(Type type, Value<?, ?> val) throws NoThreadContextException {
        if (val == ValueFactory.createNULLValue()) return true;
        String descriptor = type.getDescriptor();
        return switch (descriptor) {
            case "I" -> val instanceof IntValue || val instanceof IntegerObjectValue
                || val instanceof ByteValue || val instanceof ShortValue
                || val instanceof CharValue;
            case "Z" -> isABoolean(val);
            case "C" -> isAChar(val) || val instanceof CharacterObjectValue;
            case "D" -> val instanceof DoubleValue || val instanceof DoubleObjectValue;
            case "F" -> val instanceof FloatValue || val instanceof FloatObjectValue;
            case "J" -> val instanceof LongValue || val instanceof LongObjectValue;
            case "S" -> isAShort(val);
            case "B" -> isaByte(val);
                // case "Ljava/lang/String;" -> val instanceof StringValue;
            default -> val instanceof ObjectValue;
        };

        /*
        B	byte	signed byte
        C	char	Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16
        D	double	double-precision floating-point value
        F	float	single-precision floating-point value
        I	int	    integer
        J	long	long
        L ClassName ;	reference	an instance of class ClassName
        S	short	signed short
        Z	boolean	true or false
        */
    }

    public static Value<?, ?> convertType(Type type, Value<?, ?> val) throws NoThreadContextException, NotImplementedException, ValueConversionException {
        if (val == ValueFactory.createNULLValue()) return val;
        String descriptor = type.getDescriptor();
        return switch (descriptor) {
            case "I" -> val.asIntValue();
            case "Z" -> val.asBooleanValue();
            case "C" -> val.asCharValue();
            case "D" -> val.asDoubleValue();
            case "F" -> val.asFloatValue();
            case "J" -> val.asLongValue();
            case "S" -> val.asShortValue();
            case "B" -> val.asByteValue();
            // case "Ljava/lang/String;" -> val instanceof StringValue;
            default -> val;
        };
    }


    private static boolean isAChar(Value<?, ?> val) {
        return val instanceof CharValue
                || (val instanceof IntValue && ((IntValue) val).concrete <= 65535);
    }

    private static boolean isABoolean(Value<?, ?> val) {
        return val instanceof BooleanValue
                || (val instanceof IntValue && ((IntValue) val).concrete <= 1);
    }

    private static boolean isaByte(Value<?, ?> val) {
        return val instanceof ByteValue
                || (val instanceof IntValue && ((IntValue) val).concrete <= 255);
    }

    private static boolean isAShort(Value<?, ?> val) {
        return val instanceof ShortValue
                || (val instanceof IntValue && ((IntValue) val).concrete <= 65535);
    }

    /**
     * Checks if a class should be instrumented. The decision is based on both include and exclude
     * packages, with priority given to the most specific (longest matching prefix) package.
     *
     * <p>If the include packages are set, only classes in those packages will be instrumented. If
     * the exclude packages are set, classes in those packages will not be instrumented. If a class
     * matches both an include and an exclude package, the more specific match (longer prefix) will
     * take precedence.
     *
     * @param cname the name of the class
     * @return true if the class should be instrumented, false otherwise
     */
    public static boolean shouldInstrument(String cname) {
        cname = formatClassName(cname);
        // Special case for SV-Comp
        if (cname.equals("de/uzl/its/swat/instrument/svcomp/Verifier")
                && config.getInstrumentationTransformer().equals(TransformerType.SV_COMP)) {
            logger.trace("Instrumenting SV-Comp Verifier class");
            return true;
        }

        if (isInstrumentedUtilClass(cname)) {
            logger.trace("Instrumenting UtilInstrumented class");
            return true;
        }

        if (shouldInstrumentCache.containsKey(cname)) {
            return shouldInstrumentCache.get(cname);
        }
        int longestInclude = includeList.length > 0 ? longestPrefix(includeList, cname) : -1;
        int longestExclude = excludeList.length > 0 ? longestPrefix(excludeList, cname) : -1;
        boolean verdict = includeList.length > 0 ? longestInclude > longestExclude : longestExclude == -1;
        if (verdict) {
            logger.trace("Instrumenting '{}': Included prefix = {}, Excluded prefix = {}", cname, longestInclude,
                    longestExclude);
        }
        // logger.trace("Decision to instrument '{}': Included prefix = {}, Excluded prefix = {}, Verdict = {}",
        //        cname, longestInclude, longestExclude, verdict);

        shouldInstrumentCache.put(cname, verdict);
        return verdict;
    }

    public static boolean isInstrumentedUtilClass(String cname) {
        cname = formatClassName(cname);
        return cname.equals("de/uzl/its/swat/common/UtilInstrumented");
    }

    public static boolean ignoreMethod(String methodName) {
        return methodName.equals("$jacocoInit");
    }

    public static boolean isInSymbolicScope(String cname) {
        cname = formatClassName(cname);
        int longestInclude = symbolicScope.length > 0 ? longestPrefix(symbolicScope, cname) : -1;
        int longestExclude = excludeList.length > 0 ? longestPrefix(excludeList, cname) : -1;
        boolean verdict = symbolicScope.length > 0 ? longestInclude > longestExclude : longestExclude == -1;
        return verdict;
    }

    public static boolean useCheckClassAdapterForClass(String cname) {

        if (Config.instance().getCheckClassAdapterClass().equals("*")) {
            return true;
        } else {
            return formatClassName(cname).equals(Config.instance().getCheckClassAdapterClass());
        }
    }

    private static int longestPrefix(String[] arr, String str) {
        return Arrays.stream(arr).filter(str::startsWith).mapToInt(String::length).max().orElse(-1);
    }


    public static Field[] getDeclaredFields(Class<?> clazz)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException {
        // Get fields, this might cause a class load, which might trigger an instrumented class
        // loader. Hence, we have to disable the tracking during the load.
        ThreadHandler.disableThreadContext(currentThread().getId()); // Todo is that needed?
        Field[] fields = clazz.getDeclaredFields();
        ThreadHandler.enableThreadContext(currentThread().getId());
        return fields;
    }

    public static Class<?> getSuperclass(Class<?> clazz)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException {
        ThreadHandler.disableThreadContext(currentThread().getId()); // Todo is that needed?
        Class<?> superclass = clazz.getSuperclass();
        ThreadHandler.enableThreadContext(currentThread().getId());
        return superclass;
    }

    public static Class<?>[] getInterfaces(Class<?> clazz)
            throws ThreadAlreadyEnabledException, ThreadAlreadyDisabledException, NoThreadContextException {
        ThreadHandler.disableThreadContext(currentThread().getId()); // Todo is that needed?
        Class<?>[] interfaces = clazz.getInterfaces();
        ThreadHandler.enableThreadContext(currentThread().getId());
        return interfaces;
    }


    private static void checkClassName(String className) {
        SWATAssert.check(!className.contains(";") ||!className.contains("(") || !className.contains(")") || !className.contains("L"),
                "Class name should not contain ';' or brackets");
    }
    /**
     * Formats a class name to the format used in the JVM.
     * @param className the class name
     * @return the formatted class name
     */
    public static String formatClassName(String className) {
        checkClassName(className);
        if(className.contains(".")) {
            return className.replace(".", "/");
        }
        return className;
    }

    public static String formatFieldName(String className, String fieldName) {
        checkClassName(className);
        return className.replace("/", ".") + FIELD_DIVIDER + fieldName;
    }

    public static String formatMethodName(String className, String methodName, String desc) {
        return formatClassName(className) + METHOD_DIVIDER + methodName + METHOD_DIVIDER + desc;
    }

    public static String extractMethodName(String methodName) {
        String [] mNameParts = methodName.split(METHOD_DIVIDER);
        SWATAssert.check(mNameParts.length == 3, "Method divider should be present twice: cname<div>mname<div>desc");
        return mNameParts[mNameParts.length - 2];
    }

    /**
     * Removes the suffix from a class name.
     * This is intended for classes that are enhanced or proxied by frameworks like Hibernate or Spring.
     * Todo: This is a very simple implementation and does not cover all cases. In particular, classes are allowed to
     * have dollar signs in their names, which can lead to false positives.
     * @param cname the class name
     * @return the class name without the suffix
     */
    public static String extractBaseCname(String cname) {
            int idx = cname.indexOf("$$");
            if (idx == -1) {
                return cname;
            }
            return cname.substring(0, idx);
    }


    /**
     * Loads a class for a given name and disables the logging during the load. Currently it exits
     * the program in case the class is not found.
     *
     * @param className classname to be loaded
     * @return the loaded class or null if class is not found
     */
    public static Class<?> loadClassForName(String className)
            throws NoThreadContextException, ThreadAlreadyDisabledException, ThreadAlreadyEnabledException, ClassNotFoundException {
        // ToDo Refactor...
        ThreadHandler.disableThreadContext(currentThread().getId()); // Todo is that needed?

        String [] classNameParts = className.split("\\$\\$");
        classNameParts[0] = classNameParts[0].replace("/", ".");
        String classNameNormalized = String.join("$$", classNameParts);

        Class<?> clazz =
                Class.forName(classNameNormalized, true, Thread.currentThread().getContextClassLoader());

        ThreadHandler.enableThreadContext(currentThread().getId());

        return clazz;
    }

    /**
     * Temporary method used for validation
     *
     * @param c
     * @param ifaceList
     */
    private static void addAllInterfacesOfClass(Class<?> c, List<Class<?>> ifaceList) {
        List<Class<?>> ifaceListCurrent = new ArrayList<>(Arrays.asList(c.getInterfaces()));
        while (!ifaceListCurrent.isEmpty()) {
            ifaceList.addAll(ifaceListCurrent);
            List<Class<?>> ifaceListNew = new ArrayList<>();
            for (Class<?> iface : ifaceListCurrent) {
                ifaceListNew.addAll(Arrays.asList(iface.getInterfaces()));
            }
            ifaceListCurrent.clear();
            ifaceListCurrent.addAll(ifaceListNew);
        }
    }

    @SuppressWarnings("unused")
    public static Class<?>[] getAllInterfaces(Class<?> clazz) {
        Class<?> current = clazz;

        List<Class<?>> ifaceList = new ArrayList<>();
        // Walk up the inheritance tree until we reach Object
        while (current != null && current != Object.class) {
            addAllInterfacesOfClass(current, ifaceList);
            current = current.getSuperclass();
        }

        return ifaceList.toArray(new Class<?>[0]);
    }

    /**
     * Temporary method used for validation
     * @param clazz
     * @return
     */
    private static Set<Field> getAllFields(Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();
        Class<?> current = clazz;

        List<Class<?>> ifaceList = new ArrayList<>();
        // Walk up the inheritance tree until we reach Object
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            addAllInterfacesOfClass(current, ifaceList);
            current = current.getSuperclass();
        }

        for (Class<?> iface : ifaceList) {
            Collections.addAll(fields, iface.getDeclaredFields());
        }
        return fields;
    }

    /**
     * Temporary method used for validation
     * @param className
     * @param isStatic
     * @return
     */
    public static int getFieldCount(String className, @Nullable Class<?> cls, boolean isStatic)
            throws NoThreadContextException, ThreadAlreadyDisabledException, ThreadAlreadyEnabledException, ClassNotFoundException {
        Class<?> clazz;
        if (cls == null) {
            clazz = loadClassForName(className);
        } else {
            clazz = cls;
        }
        Set<Field> fields = getAllFields(clazz);
        int count = 0;
        for (Field field : fields) {
            if (isStatic == Modifier.isStatic(field.getModifiers())) {
                count++;
            }
        }
        return count;
    }
    public static boolean isSymbolicClass(String cname) {
        ArrayList<String> symbolicClasses = config.getInstrumentationParameterSymbolicClassNames();
        return symbolicClasses.contains(cname);
    }
    public static int symbolicClassIdx(String cname) {
        ArrayList<String> symbolicClasses = config.getInstrumentationParameterSymbolicClassNames();
        return symbolicClasses.indexOf(cname);
    }

    public static boolean isSymbolicMethod(String cname, String name) {
        int cidx = symbolicClassIdx(cname);
        SWATAssert.enforce(cidx != -1, "Class {} is not symbolic, cannot check method {}", cname, name);
        String symbolicMethodPattern = config.getInstrumentationParameterSymbolicMethodNames().get(cidx);

        return Pattern.matches(symbolicMethodPattern, name);
    }
    static boolean shouldUseValueEquality(Object a, Object b) {
        // If either is null, Objects.equals handles it correctly anyway
        if (a == null || b == null) {
            return true;
        }

        // Check if either object's class has been de-interned
        return isDeInternedClass(a.getClass()) || isDeInternedClass(b.getClass());
    }

    private static boolean isDeInternedClass(Class<?> clazz) {
        // Check the exact class and optionally superclasses if inheritance matters
        return deInternedClasses.contains(clazz.getName());
    }

}
