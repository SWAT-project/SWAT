package de.uzl.its.swat.staticcheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The application classes of a program (every class file on its classpath) and the queries the
 * check needs on them: hierarchy, method and field resolution, and the possible targets of a call.
 * Every class that is not on the classpath is a library (JDK) class.
 */
final class Program {

    /**
     * The SV-COMP Verifier package: modelled by the check instead of analysed. Built at run time,
     * so that the shadow jar's relocation of org.sosy_lab (java-smt) does not rewrite it.
     */
    static final String VERIFIER_PACKAGE = "org/".concat("sosy_lab/sv_benchmarks/");

    /** A method of an application class. */
    static final class Method {
        final ClassNode owner;
        final MethodNode node;
        final String key;

        Method(ClassNode owner, MethodNode node) {
            this.owner = owner;
            this.node = node;
            this.key = owner.name + "." + node.name + node.desc;
        }

        boolean isStatic() {
            return (node.access & Opcodes.ACC_STATIC) != 0;
        }

        boolean hasCode() {
            return (node.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * Where a call may go: application methods, plus library methods (owner.name+desc) when the
     * receiver can be a library object or inherit a library implementation.
     */
    static final class Targets {
        final Set<Method> methods = new LinkedHashSet<>();
        final Set<String> libraryMethods = new LinkedHashSet<>();
    }

    private final Map<String, ClassNode> classes = new LinkedHashMap<>();
    private final Map<String, Method> methods = new LinkedHashMap<>();
    private final Map<String, List<String>> subtypes = new HashMap<>();

    Program(List<Path> classpath) throws IOException {
        for (Path entry : classpath) {
            if (Files.isDirectory(entry)) {
                try (Stream<Path> files = Files.walk(entry)) {
                    for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".class"))::iterator) {
                        try (InputStream in = Files.newInputStream(p)) {
                            add(in);
                        }
                    }
                }
            } else if (entry.toString().endsWith(".jar") && Files.isRegularFile(entry)) {
                try (JarFile jar = new JarFile(entry.toFile())) {
                    for (JarEntry e : Collections.list(jar.entries())) {
                        if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                            try (InputStream in = jar.getInputStream(e)) {
                                add(in);
                            }
                        }
                    }
                }
            }
        }
        for (ClassNode c : classes.values()) {
            if (c.superName != null) subtypes.computeIfAbsent(c.superName, k -> new ArrayList<>()).add(c.name);
            for (String i : c.interfaces) subtypes.computeIfAbsent(i, k -> new ArrayList<>()).add(c.name);
        }
    }

    private void add(InputStream in) throws IOException {
        ClassNode c = new ClassNode();
        new ClassReader(in).accept(c, ClassReader.SKIP_FRAMES);
        // The first definition on the classpath wins, as for the JVM's class loader.
        if (classes.containsKey(c.name)) return;
        if (unsupported == null) unsupported = unsupportedReason(c.name);
        classes.put(c.name, c);
        if (c.name.startsWith(VERIFIER_PACKAGE)) return;
        for (MethodNode m : c.methods) {
            Method method = new Method(c, m);
            methods.put(method.key, method);
        }
    }

    /**
     * Why a class on the classpath breaks the assumption that the classpath is the program (null
     * if it does not). The JVM does not load such a class the way the analysis models it: it
     * refuses java/ packages, serves packages the platform defines from the platform, and the
     * analysis models only Verifier and ObjectFactory of the Verifier package, so the static
     * initializer of any other class there would run unanalysed.
     */
    private static String unsupportedReason(String name) {
        int slash = name.lastIndexOf('/');
        String pkg = slash < 0 ? "" : name.substring(0, slash);
        if (name.startsWith(VERIFIER_PACKAGE)) {
            if (name.equals(VERIFIER_PACKAGE + "Verifier") || name.equals(VERIFIER_PACKAGE + "ObjectFactory")) return null;
            return "class " + name + " in the Verifier package";
        }
        for (String prefix : PLATFORM_PREFIXES) {
            if (name.startsWith(prefix)) return "class " + name + " in a platform package";
        }
        if (PLATFORM_PACKAGES.contains(pkg.replace('/', '.'))) return "class " + name + " in a package of the JDK";
        return null;
    }

    /**
     * Package prefixes of the platform: java/, javax/, jdk/, sun/, and those that exist only in the
     * JDK 8 runtime (not as modules of the JDK the check runs on).
     */
    private static final List<String> PLATFORM_PREFIXES = List.of("java/", "javax/", "jdk/", "sun/", "com/sun/",
            "org/omg/", "org/w3c/", "org/xml/", "org/ietf/", "org/jcp/");

    /** The packages of the JDK's boot modules (the platform the check runs on). */
    private static final Set<String> PLATFORM_PACKAGES = new HashSet<>();

    static {
        for (Module m : ModuleLayer.boot().modules()) PLATFORM_PACKAGES.addAll(m.getPackages());
    }

    /** Set if a class on the classpath breaks the model of class loading; the check then fails. */
    private String unsupported;

    String unsupportedClasspath() {
        return unsupported;
    }

    /** An application class (not a library class, and not the modelled Verifier package). */
    boolean isApp(String internalName) {
        return classes.containsKey(internalName) && !internalName.startsWith(VERIFIER_PACKAGE);
    }

    boolean isOnClasspath(String internalName) {
        return classes.containsKey(internalName);
    }

    ClassNode clazz(String internalName) {
        return classes.get(internalName);
    }

    Collection<Method> methods() {
        return methods.values();
    }

    Method method(String owner, String name, String desc) {
        return methods.get(owner + "." + name + desc);
    }

    Collection<ClassNode> appClasses() {
        List<ClassNode> out = new ArrayList<>();
        for (ClassNode c : classes.values()) if (isApp(c.name)) out.add(c);
        return out;
    }

    boolean isEnum(String internalName) {
        ClassNode c = classes.get(internalName);
        return c != null && isApp(internalName) && (c.access & Opcodes.ACC_ENUM) != 0
                && "java/lang/Enum".equals(c.superName);
    }

    /** The class and its application superclasses, ending with the first library class (if any). */
    List<String> superChain(String internalName) {
        List<String> chain = new ArrayList<>();
        String c = internalName;
        while (c != null) {
            chain.add(c);
            ClassNode node = classes.get(c);
            if (node == null || !isApp(c)) break;
            c = node.superName;
        }
        return chain;
    }

    /** All application types that are subtypes of the given type, the type itself included. */
    Set<String> appSubtypes(String type) {
        Set<String> out = new LinkedHashSet<>();
        Deque<String> work = new ArrayDeque<>();
        work.add(type);
        while (!work.isEmpty()) {
            String t = work.poll();
            if (isApp(t) && !out.add(t)) continue;
            for (String s : subtypes.getOrDefault(t, List.of())) if (!out.contains(s)) work.add(s);
        }
        return out;
    }

    /** All application supertypes (classes and interfaces) of an application type, itself included. */
    Set<String> appSupertypes(String type) {
        Set<String> out = new LinkedHashSet<>();
        Deque<String> work = new ArrayDeque<>();
        work.add(type);
        while (!work.isEmpty()) {
            String t = work.poll();
            ClassNode c = classes.get(t);
            if (c == null || !isApp(t) || !out.add(t)) continue;
            if (c.superName != null) work.add(c.superName);
            work.addAll(c.interfaces);
        }
        return out;
    }

    /** The field a field instruction refers to: declared in the owner or a superclass/superinterface. */
    FieldNode resolveField(String owner, String name, String desc, String[] declaringClass) {
        Deque<String> work = new ArrayDeque<>();
        work.add(owner);
        Set<String> seen = new HashSet<>();
        while (!work.isEmpty()) {
            String t = work.poll();
            if (!seen.add(t)) continue;
            ClassNode c = classes.get(t);
            if (c == null || !isApp(t)) continue;
            for (FieldNode f : c.fields) {
                if (f.name.equals(name) && f.desc.equals(desc)) {
                    declaringClass[0] = t;
                    return f;
                }
            }
            work.addAll(c.interfaces);
            if (c.superName != null) work.add(c.superName);
        }
        return null;
    }

    /**
     * The method a static or special call (or a virtual call on a known runtime class) executes,
     * searching the class and its superclasses. Returns the application method, or null with
     * libraryOwner[0] set to the first library class of the chain when the search leaves the
     * application.
     */
    Method resolveInChain(String owner, String name, String desc, String[] libraryOwner) {
        for (String c : superChain(owner)) {
            if (!isApp(c)) {
                libraryOwner[0] = c;
                return null;
            }
            Method m = method(c, name, desc);
            if (m != null) return m;
        }
        return null;
    }

    /** All default methods for name+desc in the application interfaces of a class. */
    private List<Method> defaults(String clazz, String name, String desc) {
        List<Method> out = new ArrayList<>();
        for (String t : appSupertypes(clazz)) {
            ClassNode c = classes.get(t);
            if ((c.access & Opcodes.ACC_INTERFACE) == 0) continue;
            Method m = method(t, name, desc);
            if (m != null && m.hasCode() && !m.isStatic() && (m.node.access & Opcodes.ACC_PRIVATE) == 0) out.add(m);
        }
        return out;
    }

    /**
     * The targets of an invokevirtual/invokeinterface on an application owner type: a superset of
     * the methods the JVM may select for each concrete application subtype of the owner (JVMS
     * 5.4.6). Rather than following the selection rules exactly (package-private overriding,
     * maximally-specific default methods), every non-private instance method with that name and
     * descriptor in the superclass chain, and every default method of the application
     * superinterfaces, counts. A library implementation counts unless a public or protected
     * application method overrides it. Returns null if a library class or library interface whose
     * methods are unknown might provide it.
     */
    Targets virtualTargets(String owner, String name, String desc) {
        Targets t = new Targets();
        Method declared = method(owner, name, desc);
        if (declared != null && (declared.node.access & Opcodes.ACC_PRIVATE) != 0) {
            // A private method is selected directly (invokevirtual on a nestmate's private method).
            if (!declared.hasCode()) return null;
            t.methods.add(declared);
            return t;
        }
        for (String sub : appSubtypes(owner)) {
            ClassNode c = classes.get(sub);
            if ((c.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) continue;
            boolean overridesLibrary = false;
            String lib = null;
            for (String k : superChain(sub)) {
                if (!isApp(k)) {
                    lib = k;
                    break;
                }
                Method m = method(k, name, desc);
                if (m == null || m.isStatic() || (m.node.access & Opcodes.ACC_PRIVATE) != 0) continue;
                if ((m.node.access & Opcodes.ACC_NATIVE) != 0) return null;
                if (m.hasCode()) t.methods.add(m);
                if ((m.node.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) overridesLibrary = true;
            }
            t.methods.addAll(defaults(sub, name, desc));
            if (overridesLibrary) continue;
            // A default method of a library interface could be selected instead, and we do not know them.
            for (String sup : appSupertypes(sub)) {
                for (String i : classes.get(sup).interfaces) {
                    if (!isApp(i) && !JdkModel.hasNoDefaultMethods(i)) return null;
                }
            }
            if (lib == null) continue;
            String declaring = JdkModel.declaringClass(lib, name, desc);
            if (declaring != null) t.libraryMethods.add(declaring + "." + name + desc);
            else if (!JdkModel.knowsMethodsOf(lib)) return null;
        }
        return t;
    }

    /** Whether a value of type {@code from} is always assignable to type {@code to}. */
    boolean isAssignable(String from, String to) {
        if (from == null || to == null) return false;
        if (from.equals(to) || to.equals("java/lang/Object")) return true;
        if (from.startsWith("[")) {
            if (to.equals("java/lang/Cloneable") || to.equals("java/io/Serializable")) return true;
            if (!to.startsWith("[")) return false;
            String fe = from.substring(1), te = to.substring(1);
            if (fe.startsWith("[") || te.startsWith("[")) return isAssignable(fe, te);
            if (fe.startsWith("L") && te.startsWith("L"))
                return isAssignable(fe.substring(1, fe.length() - 1), te.substring(1, te.length() - 1));
            return fe.equals(te);
        }
        if (to.startsWith("[")) return false;
        Deque<String> work = new ArrayDeque<>();
        work.add(from);
        Set<String> seen = new HashSet<>();
        while (!work.isEmpty()) {
            String t = work.poll();
            if (!seen.add(t)) continue;
            if (t.equals(to)) return true;
            if (isApp(t)) {
                ClassNode c = classes.get(t);
                if (c.superName != null) work.add(c.superName);
                work.addAll(c.interfaces);
            } else {
                work.addAll(JdkModel.knownSupertypes(t));
            }
        }
        return false;
    }
}
