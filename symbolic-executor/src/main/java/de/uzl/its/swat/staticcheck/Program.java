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
        classes.put(c.name, c);
        if (c.name.startsWith(VERIFIER_PACKAGE)) return;
        for (MethodNode m : c.methods) {
            Method method = new Method(c, m);
            methods.put(method.key, method);
        }
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

    /** A default method for name+desc in the application interfaces of a class (null if none). */
    private Method resolveDefault(String clazz, String name, String desc) {
        for (String t : appSupertypes(clazz)) {
            ClassNode c = classes.get(t);
            if ((c.access & Opcodes.ACC_INTERFACE) == 0) continue;
            Method m = method(t, name, desc);
            if (m != null && m.hasCode() && !m.isStatic()) return m;
        }
        return null;
    }

    /**
     * The targets of an invokevirtual/invokeinterface on an application owner type: the method each
     * concrete application subtype of the owner resolves to. Returns null if some subtype resolves
     * to a library method that is not one of the known methods of java/lang/Object or java/lang/Enum.
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
            String[] lib = new String[1];
            Method m = resolveInChain(sub, name, desc, lib);
            if (m != null) {
                if (m.hasCode()) t.methods.add(m);
                else if ((m.node.access & Opcodes.ACC_NATIVE) != 0) return null;
                // abstract in a concrete class cannot happen for verified classes
                continue;
            }
            String declaring = lib[0] == null ? null : JdkModel.declaringClass(lib[0], name, desc);
            if (declaring != null) {
                t.libraryMethods.add(declaring + "." + name + desc);
                continue;
            }
            if (lib[0] != null && !JdkModel.knowsMethodsOf(lib[0])) return null;
            Method d = resolveDefault(sub, name, desc);
            if (d != null) t.methods.add(d);
            // else: AbstractMethodError, an Error
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
