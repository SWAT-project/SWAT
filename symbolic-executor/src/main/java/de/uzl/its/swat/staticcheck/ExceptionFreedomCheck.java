package de.uzl.its.swat.staticcheck;

import java.nio.file.Path;
import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * A conservative static check that a program can never end with an uncaught RuntimeException,
 * the SV-COMP no-runtime-exception property. If it succeeds, the property holds on every
 * execution; if it fails, nothing is known.
 *
 * <p>The program is every class on its classpath, entered through {@code Main.main}. The check
 * proves that no RuntimeException can escape {@code main} or any static initializer:
 *
 * <ul>
 *   <li>Every instruction that the JVM specification lets throw a RuntimeException (array access
 *       and allocation, field access, calls, casts, integer division, {@code athrow}, monitors,
 *       {@code invokedynamic}) is proven not to throw one, from abstract values that track
 *       nullness, constant ints, array lengths, enum ordinals and runtime types, or is covered by a
 *       handler that catches every RuntimeException (whose own code is checked in turn).
 *   <li>Calls go to application methods that are themselves proven (a least fixpoint over the call
 *       graph, with class-hierarchy dispatch), to the modelled {@code Verifier}, or to the library
 *       methods of {@link JdkModel}, which throw nothing and never call back into application code.
 *       Every other library call fails the check.
 *   <li>Facts about fields, parameters and return values are the joins of all values the program
 *       stores, passes or returns (a least fixpoint from "no value"). A reference field also holds
 *       its default null, unless the field is assigned before any code that could read it runs
 *       (see {@link #initializedBeforeRead}).
 * </ul>
 *
 * <p>Errors (AssertionError, StackOverflowError, OutOfMemoryError, linkage errors, ...) are not
 * RuntimeExceptions and are not considered. Enum classes are assumed to be compiled from Java
 * source, so the ordinal of a constant is below the number of constants.
 */
public final class ExceptionFreedomCheck implements FactInterpreter.Facts, Opcodes {

    static final String MAIN_CLASS = "Main";
    static final String MAIN_DESC = "([Ljava/lang/String;)V";
    private static final int MAX_ROUNDS = 100;

    /**
     * The outcome: whether the program is proven free of uncaught RuntimeExceptions, whether it is
     * proven to never create an AssertionError (so no assert can fail), and why not.
     */
    public static final class Result {
        public final boolean exceptionFree;
        public final String reason;
        public final boolean assertionFree;
        public final String assertionReason;

        Result(boolean exceptionFree, String reason, boolean assertionFree, String assertionReason) {
            this.exceptionFree = exceptionFree;
            this.reason = reason;
            this.assertionFree = assertionFree;
            this.assertionReason = assertionReason;
        }

        static Result neither(String reason) {
            return new Result(false, reason, false, reason);
        }
    }

    private final Program program;
    private final Map<String, AbsValue> fieldFacts = new HashMap<>();
    private final Map<String, AbsValue[]> parameterFacts = new HashMap<>();
    private final Map<String, AbsValue> returnFacts = new HashMap<>();
    private final Set<String> initializedBeforeRead = new HashSet<>();
    private final Map<String, Frame<AbsValue>[]> frames = new HashMap<>();
    private final Map<AbstractInsnNode, Program.Targets> targetCache = new IdentityHashMap<>();
    private final Set<AbstractInsnNode> unknownTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    private Program.Method entry;
    private boolean changed;

    ExceptionFreedomCheck(Program program) {
        this.program = program;
    }

    public static Result check(List<Path> classpath) {
        try {
            return new ExceptionFreedomCheck(new Program(classpath)).run();
        } catch (Exception | StackOverflowError e) {
            return Result.neither("analysis failed: " + e);
        }
    }

    Result run() throws AnalyzerException {
        entry = program.method(MAIN_CLASS, "main", MAIN_DESC);
        if (entry == null || !entry.isStatic() || !entry.hasCode()) return Result.neither("no Main.main");

        computeInitializedBeforeRead();
        for (ClassNode c : program.appClasses()) {
            for (FieldNode f : c.fields) {
                String key = c.name + "." + f.name + ":" + f.desc;
                Type t = Type.getType(f.desc);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
                    fieldFacts.put(key, AbsValue.prim(t.getSize()));
                    continue;
                }
                if (f.value instanceof String) fieldFacts.put(key, AbsValue.exact("java/lang/String"));
                else if (!initializedBeforeRead.contains(key)) fieldFacts.put(key, AbsValue.NULL);
            }
        }

        // Facts: least fixpoint, from "no value" upwards.
        int round = 0;
        do {
            if (++round > MAX_ROUNDS) return Result.neither("facts did not converge");
            changed = false;
            for (Program.Method m : program.methods()) {
                if (!m.hasCode()) continue;
                Frame<AbsValue>[] f = new Analyzer<>(new FactInterpreter(this, m)).analyze(m.owner.name, m.node);
                frames.put(m.key, f);
                collectFacts(m, f);
            }
        } while (changed);

        // May-throw: least fixpoint over the call graph.
        Map<String, String> mayThrow = new LinkedHashMap<>();
        boolean grew;
        do {
            grew = false;
            for (Program.Method m : program.methods()) {
                if (!m.hasCode() || mayThrow.containsKey(m.key)) continue;
                String reason = escapingException(m, mayThrow);
                if (reason != null) {
                    mayThrow.put(m.key, reason);
                    grew = true;
                }
            }
        } while (grew);

        List<Program.Method> roots = new ArrayList<>();
        roots.add(entry);
        for (ClassNode c : program.appClasses()) {
            Program.Method clinit = program.method(c.name, "<clinit>", "()V");
            if (clinit != null) roots.add(clinit);
        }
        for (ClassNode c : program.appClasses()) {
            // The finalizer thread may run it on any unreachable object, even one whose constructor
            // threw before it assigned its fields.
            Program.Method fin = program.method(c.name, "finalize", "()V");
            if (fin != null && !fin.isStatic()) return Result.neither("finalizer " + fin.key);
        }
        Set<Program.Method> reachable = new LinkedHashSet<>();
        String open = openWorldCall(roots, reachable);
        if (open != null) return Result.neither(open);
        String assertion = assertionCreation(reachable);
        boolean assertionFree = assertion == null;
        if (assertionFree) assertion = "no AssertionError is created in code reachable from Main.main or a static initializer";
        for (Program.Method r : roots) {
            if (mayThrow.containsKey(r.key)) return new Result(false, explain(r.key, mayThrow), assertionFree, assertion);
        }
        return new Result(true, "no instruction reachable from Main.main or a static initializer can throw an uncaught RuntimeException",
                assertionFree, assertion);
    }

    /**
     * Where reachable code may create an AssertionError (null if nowhere). With a closed world,
     * only application code creates objects of application classes or of AssertionError: the
     * modelled library methods create none, and no assert of the JDK is enabled.
     */
    private String assertionCreation(Set<Program.Method> reachable) {
        for (Program.Method m : reachable) {
            Frame<AbsValue>[] f = frames.get(m.key);
            AbstractInsnNode[] insns = m.node.instructions.toArray();
            for (int i = 0; i < insns.length; i++) {
                if (f[i] == null) continue;
                String type = null;
                if (insns[i].getOpcode() == NEW) type = ((TypeInsnNode) insns[i]).desc;
                else if (insns[i].getOpcode() == INVOKESPECIAL && ((MethodInsnNode) insns[i]).name.equals("<init>"))
                    type = ((MethodInsnNode) insns[i]).owner;
                if (type != null && program.isAssignable(type, "java/lang/AssertionError"))
                    return m.key + ": line " + line(insns[i]) + ": creates " + type;
            }
        }
        return null;
    }

    /**
     * The analysis assumes a closed world: application code only runs when application code calls
     * it, with arguments the facts account for. A reachable call that may run unknown code (an
     * unmodelled library method, which could call back into the application) breaks that even if a
     * handler catches what it throws, so it fails the check. Returns why, or null, and collects the
     * reachable methods.
     */
    private String openWorldCall(List<Program.Method> roots, Set<Program.Method> reachable) {
        Deque<Program.Method> work = new ArrayDeque<>(roots);
        while (!work.isEmpty()) {
            Program.Method m = work.poll();
            if (!reachable.add(m)) continue;
            Frame<AbsValue>[] f = frames.get(m.key);
            if (f == null) return "reachable method without code: " + m.key;
            AbstractInsnNode[] insns = m.node.instructions.toArray();
            for (int i = 0; i < insns.length; i++) {
                if (f[i] == null) continue;
                if (insns[i] instanceof InvokeDynamicInsnNode && !isStringConcat((InvokeDynamicInsnNode) insns[i]))
                    return m.key + ": line " + line(insns[i]) + ": invokedynamic";
                if (!(insns[i] instanceof MethodInsnNode)) continue;
                MethodInsnNode mi = (MethodInsnNode) insns[i];
                String reason = openWorldReason(mi, f[i]);
                if (reason != null) return m.key + ": line " + line(mi) + ": " + reason;
                Program.Targets t = mi.owner.startsWith(Program.VERIFIER_PACKAGE) ? null : targets(mi);
                if (t != null) work.addAll(t.methods);
            }
        }
        return null;
    }

    /** Follows the may-throw reasons from a root down to the instruction that may throw. */
    private static String explain(String key, Map<String, String> mayThrow) {
        StringBuilder sb = new StringBuilder();
        Set<String> seen = new HashSet<>();
        String k = key;
        while (k != null && seen.add(k)) {
            String reason = mayThrow.get(k);
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(k).append(": ").append(reason);
            k = null;
            int i = reason.indexOf("call to ");
            if (i >= 0) {
                String callee = reason.substring(i + "call to ".length());
                if (mayThrow.containsKey(callee)) k = callee;
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // Facts

    private void joinInto(Map<String, AbsValue> map, String key, AbsValue v) {
        v = v.escaped();
        AbsValue old = map.get(key);
        AbsValue joined = old == null ? v : AbsValue.join(old, v);
        if (!joined.equals(old)) {
            map.put(key, joined);
            changed = true;
        }
    }

    private void collectFacts(Program.Method m, Frame<AbsValue>[] f) {
        AbstractInsnNode[] insns = m.node.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            Frame<AbsValue> fr = f[i];
            if (fr == null) continue;
            AbstractInsnNode insn = insns[i];
            int op = insn.getOpcode();
            if (op == PUTFIELD || op == PUTSTATIC) {
                FieldInsnNode fi = (FieldInsnNode) insn;
                String key = fieldKey(fi);
                if (key != null && isReferenceDesc(fi.desc)) joinInto(fieldFacts, key, top(fr, 0));
            } else if (op == ARETURN) {
                joinInto(returnFacts, m.key, top(fr, 0));
            } else if (op >= IRETURN && op <= DRETURN) {
                joinInto(returnFacts, m.key, top(fr, 0));
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode mi = (MethodInsnNode) insn;
                Program.Targets t = targets(mi);
                if (t == null) continue;
                Type[] args = Type.getArgumentTypes(mi.desc);
                for (Program.Method target : t.methods) {
                    AbsValue[] params = parameterFacts.computeIfAbsent(target.key, k -> new AbsValue[args.length]);
                    for (int a = 0; a < args.length; a++) {
                        AbsValue v = top(fr, args.length - 1 - a).escaped();
                        AbsValue joined = params[a] == null ? v : AbsValue.join(params[a], v);
                        if (!joined.equals(params[a])) {
                            params[a] = joined;
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    private static boolean isReferenceDesc(String desc) {
        return desc.startsWith("L") || desc.startsWith("[");
    }

    private static AbsValue top(Frame<AbsValue> f, int depth) {
        return f.getStack(f.getStackSize() - 1 - depth);
    }

    /** The key of the application field a field instruction accesses, or null for a library field. */
    private String fieldKey(FieldInsnNode fi) {
        String[] decl = new String[1];
        FieldNode f = program.resolveField(fi.owner, fi.name, fi.desc, decl);
        return f == null ? null : decl[0] + "." + f.name + ":" + f.desc;
    }

    @Override
    public AbsValue field(FieldInsnNode insn) {
        if (insn.getOpcode() == GETSTATIC && insn.owner.equals("java/lang/System")
                && (insn.name.equals("out") || insn.name.equals("err")) && insn.desc.equals("Ljava/io/PrintStream;")) {
            // Non-null, and exactly a PrintStream: System.setOut/setErr are not modelled, so no
            // program that passes the check can replace them.
            return AbsValue.exact("java/io/PrintStream");
        }
        String key = fieldKey(insn);
        if (key == null) return FactInterpreter.ofType(Type.getType(insn.desc));
        AbsValue v = fieldFacts.get(key);
        if (v == null) return AbsValue.bottom(Type.getType(insn.desc).getSize());
        return v;
    }

    @Override
    public AbsValue parameter(Program.Method method, int index, Type type) {
        if (method == entry) return AbsValue.array(MAIN_DESC.substring(1, MAIN_DESC.indexOf(')')), null, null);
        AbsValue[] params = parameterFacts.get(method.key);
        if (params == null || params[index] == null) return AbsValue.bottom(type.getSize());
        return params[index];
    }

    @Override
    public AbsValue callResult(MethodInsnNode insn, List<? extends AbsValue> args) {
        Type ret = Type.getReturnType(insn.desc);
        if (ret.getSort() == Type.VOID) return null;
        if (insn.owner.startsWith(Program.VERIFIER_PACKAGE)) {
            if (insn.name.equals("nondetString")) return AbsValue.exact("java/lang/String");
            return FactInterpreter.ofType(ret);
        }
        if (insn.getOpcode() == INVOKESTATIC && insn.name.equals("values") && program.isEnum(insn.owner)
                && insn.desc.equals("()[L" + insn.owner + ";")) {
            return AbsValue.array("[L" + insn.owner + ";", null, insn.owner);
        }
        if (insn.name.equals("ordinal") && insn.desc.equals("()I") && insn.getOpcode() != INVOKESTATIC) {
            return program.isEnum(insn.owner) ? AbsValue.PRIM1.withOrdinalOf(insn.owner) : AbsValue.PRIM1;
        }
        Program.Targets t = targets(insn);
        if (t == null) return FactInterpreter.ofType(ret);
        AbsValue result = null;
        for (Program.Method m : t.methods) {
            AbsValue r = returnFacts.get(m.key);
            if (r == null) r = AbsValue.bottom(ret.getSize());
            result = result == null ? r : AbsValue.join(result, r);
        }
        for (String lib : t.libraryMethods) {
            AbsValue r = libraryResult(lib, ret, args);
            result = result == null ? r : AbsValue.join(result, r);
        }
        return result == null ? FactInterpreter.ofType(ret) : result;
    }

    private static AbsValue libraryResult(String lib, Type ret, List<? extends AbsValue> args) {
        String[] parts = splitMethod(lib);
        JdkModel.Entry e = JdkModel.lookup(parts[0], parts[1], parts[2]);
        if (e == null) return FactInterpreter.ofType(ret);
        switch (e.returns) {
            case NONNULL_EXACT:
                return AbsValue.exact(ret.getInternalName());
            case RECEIVER:
                return args.get(0).escaped();
            case CLONE: {
                AbsValue r = args.get(0);
                return new AbsValue(AbsValue.Kind.NONNULL, 1, null, r.length, r.lengthOf, null, r.exactType,
                        r.exactType != null ? r.exactType : r.staticType, false);
            }
            default:
                return FactInterpreter.ofType(ret);
        }
    }

    /** owner, name and descriptor of "owner.name(desc)". */
    static String[] splitMethod(String key) {
        int paren = key.indexOf('(');
        int dot = key.lastIndexOf('.', paren);
        return new String[] {key.substring(0, dot), key.substring(dot + 1, paren), key.substring(paren)};
    }

    /** The possible targets of a call, or null if they cannot be determined. Not for Verifier calls. */
    private Program.Targets targets(MethodInsnNode mi) {
        if (unknownTargets.contains(mi)) return null;
        Program.Targets cached = targetCache.get(mi);
        if (cached != null) return cached;
        Program.Targets t = computeTargets(mi);
        if (t == null) unknownTargets.add(mi);
        else targetCache.put(mi, t);
        return t;
    }

    private Program.Targets computeTargets(MethodInsnNode mi) {
        if (mi.owner.startsWith(Program.VERIFIER_PACKAGE)) return new Program.Targets();
        int op = mi.getOpcode();
        if ((op == INVOKEVIRTUAL || op == INVOKEINTERFACE) && program.isApp(mi.owner)) {
            return program.virtualTargets(mi.owner, mi.name, mi.desc);
        }
        Program.Targets t = new Program.Targets();
        if (op == INVOKEVIRTUAL || op == INVOKEINTERFACE) {
            t.libraryMethods.add(mi.owner + "." + mi.name + mi.desc);
            return t;
        }
        if (op == INVOKESPECIAL && mi.name.equals("<init>")) {
            Program.Method m = program.method(mi.owner, mi.name, mi.desc);
            if (m != null) t.methods.add(m);
            else if (program.isApp(mi.owner)) return null;
            else t.libraryMethods.add(mi.owner + "." + mi.name + mi.desc);
            return t;
        }
        String[] lib = new String[1];
        Program.Method m = program.resolveInChain(mi.owner, mi.name, mi.desc, lib);
        if (m != null) {
            if (!m.hasCode()) return null;
            t.methods.add(m);
        } else if (lib[0] != null) {
            t.libraryMethods.add(lib[0] + "." + mi.name + mi.desc);
        } else {
            return null;
        }
        return t;
    }

    // ---------------------------------------------------------------------------------------------
    // Fields initialized before any read

    /**
     * Finds the reference fields that hold a value from their first assignment on, before any code
     * that could read them runs. Such a field never shows its default null, so its fact is the join
     * of the values stored into it only.
     *
     * <p>A static field qualifies if its class initializer assigns it in its straight-line prefix
     * (no jumps and no exception handlers), and nothing that prefix may execute (the methods it calls
     * and the static initializers of the classes it, its callees or the superclasses touch, all
     * transitively) reads a field of that name. An instance field qualifies if every constructor of
     * its class assigns it on the receiver in such a prefix, which neither lets the receiver escape
     * nor may execute a read of the field. An object is only reachable through the receiver of its
     * constructor until it escapes, so no read can come from elsewhere; a constructor that throws
     * before the assignment leaves no reference to its object behind.
     */
    private void computeInitializedBeforeRead() throws AnalyzerException {
        for (ClassNode c : program.appClasses()) {
            for (FieldNode f : c.fields) {
                if (!isReferenceDesc(f.desc)) continue;
                String key = c.name + "." + f.name + ":" + f.desc;
                boolean isStatic = (f.access & ACC_STATIC) != 0;
                if (isStatic ? staticFieldInitializedFirst(c, f) : instanceFieldInitializedFirst(c, f)) {
                    initializedBeforeRead.add(key);
                }
            }
        }
    }

    /** The instructions every execution of the method runs first, in order, up to the first jump. */
    private static List<AbstractInsnNode> straightLinePrefix(MethodNode m) {
        List<AbstractInsnNode> out = new ArrayList<>();
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() < 0) continue; // labels, line numbers, frames
            int op = insn.getOpcode();
            if (insn instanceof JumpInsnNode || insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode
                    || op == ATHROW || op == RET || (op >= IRETURN && op <= RETURN)) break;
            out.add(insn);
        }
        return out;
    }

    private static boolean coveredByHandler(MethodNode m, AbstractInsnNode insn) {
        int idx = m.instructions.indexOf(insn);
        for (TryCatchBlockNode tcb : m.tryCatchBlocks) {
            if (m.instructions.indexOf(tcb.start) <= idx && idx < m.instructions.indexOf(tcb.end)) return true;
        }
        return false;
    }

    private boolean staticFieldInitializedFirst(ClassNode c, FieldNode f) {
        Program.Method clinit = program.method(c.name, "<clinit>", "()V");
        if (clinit == null) return false;
        List<AbstractInsnNode> before = new ArrayList<>();
        for (AbstractInsnNode insn : straightLinePrefix(clinit.node)) {
            if (coveredByHandler(clinit.node, insn)) return false;
            if (insn.getOpcode() == PUTSTATIC && assigns((FieldInsnNode) insn, c, f)) {
                // The supertypes are initialized before the class; the class itself is being
                // initialized already, so touching it again does not rerun its initializer.
                Set<String> supertypes = new HashSet<>(program.appSupertypes(c.name));
                supertypes.remove(c.name);
                return !mayRead(before, supertypes, Set.of(c.name), f.name, f.desc, true);
            }
            before.add(insn);
        }
        return false;
    }

    private boolean instanceFieldInitializedFirst(ClassNode c, FieldNode f) throws AnalyzerException {
        boolean any = false;
        for (MethodNode mn : c.methods) {
            if (!mn.name.equals("<init>")) continue;
            any = true;
            Program.Method ctor = program.method(c.name, mn.name, mn.desc);
            Frame<AbsValue>[] fr = new Analyzer<>(new FactInterpreter(UNKNOWN_FACTS, ctor)).analyze(c.name, mn);
            List<AbstractInsnNode> before = new ArrayList<>();
            boolean assigned = false;
            for (AbstractInsnNode insn : straightLinePrefix(mn)) {
                if (coveredByHandler(mn, insn)) return false;
                Frame<AbsValue> frame = fr[mn.instructions.indexOf(insn)];
                if (frame == null) return false;
                if (insn.getOpcode() == PUTFIELD && assigns((FieldInsnNode) insn, c, f) && top(frame, 1).isThis
                        && !top(frame, 0).isThis) {
                    assigned = true;
                    break;
                }
                if (letsReceiverEscape(insn, frame, new HashSet<>())) return false;
                before.add(insn);
            }
            if (!assigned || mayRead(before, Set.of(), Set.of(), f.name, f.desc, false)) return false;
        }
        return any;
    }

    private boolean assigns(FieldInsnNode fi, ClassNode c, FieldNode f) {
        String[] decl = new String[1];
        FieldNode target = program.resolveField(fi.owner, fi.name, fi.desc, decl);
        return target == f && c.name.equals(decl[0]);
    }

    /**
     * Whether the instruction hands the receiver (this) to code that could keep it: as an argument
     * or receiver of a call, or as a value stored into a field or an array. The superclass or
     * delegated constructor may take it if its own code does not let it escape either.
     */
    private boolean letsReceiverEscape(AbstractInsnNode insn, Frame<AbsValue> frame, Set<String> checking) {
        int op = insn.getOpcode();
        if (op == PUTFIELD || op == PUTSTATIC || op == AASTORE || op == ARETURN || op == ATHROW) {
            return top(frame, 0).isThis;
        }
        if (insn instanceof InvokeDynamicInsnNode) {
            int n = Type.getArgumentTypes(((InvokeDynamicInsnNode) insn).desc).length;
            for (int i = 0; i < n; i++) if (top(frame, i).isThis) return true;
            return false;
        }
        if (!(insn instanceof MethodInsnNode)) return false;
        MethodInsnNode mi = (MethodInsnNode) insn;
        int n = Type.getArgumentTypes(mi.desc).length;
        for (int i = 0; i < n; i++) if (top(frame, i).isThis) return true;
        if (op == INVOKESTATIC || !top(frame, n).isThis) return false;
        if (op != INVOKESPECIAL || !mi.name.equals("<init>")) return true;
        if (mi.owner.equals("java/lang/Object") && mi.desc.equals("()V")) return false;
        if (mi.owner.equals("java/lang/Enum") && mi.desc.equals("(Ljava/lang/String;I)V")) return false;
        Program.Method ctor = program.method(mi.owner, mi.name, mi.desc);
        if (ctor == null || !ctor.hasCode() || !checking.add(ctor.key)) return true;
        try {
            Frame<AbsValue>[] fr = new Analyzer<>(new FactInterpreter(UNKNOWN_FACTS, ctor)).analyze(ctor.owner.name, ctor.node);
            AbstractInsnNode[] insns = ctor.node.instructions.toArray();
            for (int i = 0; i < insns.length; i++) {
                if (fr[i] != null && letsReceiverEscape(insns[i], fr[i], checking)) return true;
            }
            return false;
        } catch (AnalyzerException e) {
            return true;
        }
    }

    /**
     * Whether running the given instructions may read a field with this name and descriptor:
     * directly, in a method they may call (transitively), or in a static initializer they may
     * trigger (except those of the classes being initialized already). Unknown callees count as
     * reads.
     */
    private boolean mayRead(List<AbstractInsnNode> start, Set<String> initializedClasses, Set<String> initializing,
                            String name, String desc, boolean isStatic) {
        Deque<Program.Method> work = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        Set<String> classes = new HashSet<>(initializing);
        Deque<String> pendingClasses = new ArrayDeque<>(initializedClasses);
        List<AbstractInsnNode> insns = new ArrayList<>(start);
        while (true) {
            for (AbstractInsnNode insn : insns) {
                int op = insn.getOpcode();
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fi = (FieldInsnNode) insn;
                    if ((op == (isStatic ? GETSTATIC : GETFIELD)) && fi.name.equals(name) && fi.desc.equals(desc)) return true;
                    if (op == GETSTATIC || op == PUTSTATIC) pendingClasses.add(fi.owner);
                } else if (op == NEW) {
                    pendingClasses.add(((TypeInsnNode) insn).desc);
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    if (!isStringConcat((InvokeDynamicInsnNode) insn)) return true;
                } else if (insn instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) insn;
                    if (mi.owner.startsWith(Program.VERIFIER_PACKAGE)) {
                        if (!isModelledVerifierCall(mi)) return true;
                        continue;
                    }
                    if (op == INVOKESTATIC) pendingClasses.add(mi.owner);
                    Program.Targets t = targets(mi);
                    if (t == null) return true;
                    for (String lib : t.libraryMethods) {
                        String[] p = splitMethod(lib);
                        JdkModel.Entry e = JdkModel.lookup(p[0], p[1], p[2]);
                        // Without values, a library call is only known not to call back if its model
                        // has no argument condition about callbacks and no override can intervene.
                        if (e == null || e.condition == JdkModel.Condition.STRING_ARGS) return true;
                        if (op != INVOKESTATIC && !program.isApp(mi.owner) && !JdkModel.cannotBeOverridden(p[0], p[1], p[2])
                                && !e.exactReceiver) return true;
                    }
                    for (Program.Method m : t.methods) if (seen.add(m.key)) work.add(m);
                }
            }
            while (!pendingClasses.isEmpty()) {
                String cls = pendingClasses.poll();
                if (cls.startsWith("[") || !program.isApp(cls)) continue;
                for (String sup : program.appSupertypes(cls)) {
                    if (!classes.add(sup)) continue;
                    Program.Method clinit = program.method(sup, "<clinit>", "()V");
                    if (clinit != null && seen.add(clinit.key)) work.add(clinit);
                }
            }
            if (work.isEmpty()) return false;
            Program.Method m = work.poll();
            insns = new ArrayList<>();
            if (!m.hasCode()) return true;
            for (AbstractInsnNode insn : m.node.instructions) insns.add(insn);
        }
    }

    /** Facts for analyses that only need the receiver flag: nothing is known about other values. */
    private static final FactInterpreter.Facts UNKNOWN_FACTS = new FactInterpreter.Facts() {
        @Override
        public AbsValue field(FieldInsnNode insn) {
            return FactInterpreter.ofType(Type.getType(insn.desc));
        }

        @Override
        public AbsValue parameter(Program.Method method, int index, Type type) {
            return FactInterpreter.ofType(type);
        }

        @Override
        public AbsValue callResult(MethodInsnNode insn, List<? extends AbsValue> args) {
            return FactInterpreter.ofType(Type.getReturnType(insn.desc));
        }
    };

    // ---------------------------------------------------------------------------------------------
    // Checking instructions

    static boolean isStringConcat(InvokeDynamicInsnNode indy) {
        if (!indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")) return false;
        for (Type t : Type.getArgumentTypes(indy.desc)) {
            if (t.getSort() == Type.OBJECT && !t.getInternalName().equals("java/lang/String")) return false;
            if (t.getSort() == Type.ARRAY) return false;
        }
        return true;
    }

    private static boolean isModelledVerifierCall(MethodInsnNode mi) {
        return mi.owner.equals(Program.VERIFIER_PACKAGE + "Verifier") && mi.getOpcode() == INVOKESTATIC
                && (mi.name.equals("assume") || (mi.name.startsWith("nondet") && !mi.name.equals("nondetObject")));
    }

    /**
     * Why a RuntimeException may escape the method, or null if none can, given the methods already
     * known to let one escape.
     */
    private String escapingException(Program.Method m, Map<String, String> mayThrow) {
        Frame<AbsValue>[] f = frames.get(m.key);
        MethodNode mn = m.node;
        AbstractInsnNode[] insns = mn.instructions.toArray();
        int n = insns.length;
        List<TryCatchBlockNode> handlers = mn.tryCatchBlocks;
        boolean[][] covers = new boolean[handlers.size()][n];
        for (int h = 0; h < handlers.size(); h++) {
            TryCatchBlockNode tcb = handlers.get(h);
            int s = mn.instructions.indexOf(tcb.start), e = mn.instructions.indexOf(tcb.end);
            for (int i = s; i < e; i++) covers[h][i] = true;
        }
        // An instruction may raise a RuntimeException; handlers that may catch one. Rethrowing a
        // caught exception raises one only if its handler may have caught one: iterate.
        String[] raises = new String[n];
        boolean[] handlerCatches = new boolean[handlers.size()];
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int i = 0; i < n; i++) {
                if (f[i] == null || raises[i] != null) continue;
                String r = raises(m, insns[i], f[i], mayThrow, handlerCatches);
                if (r == null) continue;
                raises[i] = r;
                grew = true;
                // Over-approximated: every handler that covers it may have caught it.
                for (int h = 0; h < handlers.size(); h++) if (covers[h][i]) handlerCatches[h] = true;
            }
        }
        for (int i = 0; i < n; i++) {
            if (raises[i] == null) continue;
            boolean caught = false;
            for (int h = 0; h < handlers.size() && !caught; h++) {
                caught = covers[h][i] && catchesEveryRuntimeException(handlers.get(h).type, insns[i]);
            }
            if (!caught) return "line " + line(insns[i]) + ": " + raises[i];
        }
        return null;
    }

    private static int line(AbstractInsnNode insn) {
        for (AbstractInsnNode p = insn; p != null; p = p.getPrevious()) {
            if (p instanceof LineNumberNode) return ((LineNumberNode) p).line;
        }
        return -1;
    }

    private static boolean catchesEveryRuntimeException(String type, AbstractInsnNode insn) {
        if (type == null || type.equals("java/lang/Throwable") || type.equals("java/lang/Exception")
                || type.equals("java/lang/RuntimeException")) return true;
        // Integer division can only throw ArithmeticException.
        int op = insn.getOpcode();
        return type.equals("java/lang/ArithmeticException") && (op == IDIV || op == IREM || op == LDIV || op == LREM);
    }

    private String raises(Program.Method m, AbstractInsnNode insn, Frame<AbsValue> f, Map<String, String> mayThrow,
                          boolean[] handlerCatches) {
        int op = insn.getOpcode();
        switch (op) {
            case IDIV: case IREM: case LDIV: case LREM: {
                AbsValue d = top(f, 0);
                if (d.isBottom() || (d.constant != null && d.constant != 0)) return null;
                return "division by a value that may be zero";
            }
            case IALOAD: case LALOAD: case FALOAD: case DALOAD: case BALOAD: case CALOAD: case SALOAD: case AALOAD:
                return arrayAccess(top(f, 1), top(f, 0));
            case IASTORE: case LASTORE: case FASTORE: case DASTORE: case BASTORE: case CASTORE: case SASTORE:
                return arrayAccess(top(f, 2), top(f, 1));
            case AASTORE: {
                String r = arrayAccess(top(f, 2), top(f, 1));
                if (r != null) return r;
                AbsValue arr = top(f, 2), v = top(f, 0);
                if (v.isNull() || v.isBottom() || arr.isBottom()) return null;
                String elem = arr.exactType != null ? arr.exactType.substring(1) : null;
                if (elem != null && elem.startsWith("L")) elem = elem.substring(1, elem.length() - 1);
                if (elem != null && program.isAssignable(trustedType(v), elem)) return null;
                return "array store of a value that may not fit the array";
            }
            case ARRAYLENGTH:
                return top(f, 0).isNonNull() ? null : "length of an array that may be null";
            case NEWARRAY: case ANEWARRAY:
                return nonNegative(top(f, 0)) ? null : "array size may be negative";
            case MULTIANEWARRAY: {
                int dims = ((MultiANewArrayInsnNode) insn).dims;
                for (int i = 0; i < dims; i++) if (!nonNegative(top(f, i))) return "array size may be negative";
                return null;
            }
            case GETFIELD:
                return top(f, 0).isNonNull() ? null : "field read on a reference that may be null";
            case PUTFIELD:
                return top(f, 1).isNonNull() ? null : "field write on a reference that may be null";
            case CHECKCAST: {
                AbsValue v = top(f, 0);
                if (v.isNull() || v.isBottom() || program.isAssignable(trustedType(v), ((TypeInsnNode) insn).desc)) return null;
                return "cast that may fail";
            }
            case ATHROW: {
                AbsValue v = top(f, 0);
                if (v.isBottom()) return null;
                if (v.caughtBy >= 0) return handlerCatches[v.caughtBy] ? "rethrow of a caught RuntimeException" : null;
                if (v.exactType != null && isNeverRuntimeException(v.exactType)) return null;
                return "throw of a value that may be a RuntimeException";
            }
            case MONITORENTER: case MONITOREXIT:
                return "monitor instruction";
            case INVOKEDYNAMIC:
                return isStringConcat((InvokeDynamicInsnNode) insn) ? null : "invokedynamic";
            case INVOKEVIRTUAL: case INVOKESPECIAL: case INVOKESTATIC: case INVOKEINTERFACE:
                return call((MethodInsnNode) insn, f, mayThrow);
            default:
                return null;
        }
    }

    private String arrayAccess(AbsValue arr, AbsValue idx) {
        if (arr.isBottom() || idx.isBottom()) return null;
        if (!arr.isNonNull()) return "access to an array that may be null";
        if (arr.length != null && idx.constant != null && idx.constant >= 0 && idx.constant < arr.length) return null;
        if (arr.lengthOf != null && arr.lengthOf.equals(idx.ordinalOf)) return null;
        return "array index that may be out of bounds";
    }

    private static boolean nonNegative(AbsValue count) {
        return count.isBottom() || (count.constant != null && count.constant >= 0) || count.lengthOf != null;
    }

    /** A type every non-null value of the slot is an instance of, if one is reliably known. */
    private String trustedType(AbsValue v) {
        if (v.exactType != null) return v.exactType;
        String t = v.staticType;
        if (t == null) return null;
        // The verifier does not check interface types, so only class and array types are trusted.
        if (t.startsWith("[") || t.equals("java/lang/Object") || JdkModel.isFinalClass(t)) return t;
        ClassNode c = program.clazz(t);
        if (c != null && program.isApp(t) && (c.access & ACC_INTERFACE) == 0) return t;
        return null;
    }

    /** The runtime class is a Throwable that is not a RuntimeException. */
    private boolean isNeverRuntimeException(String exactType) {
        List<String> chain = program.superChain(exactType);
        String root = chain.get(chain.size() - 1);
        if (program.isApp(root)) return false;
        return root.equals("java/lang/AssertionError") || root.equals("java/lang/Error")
                || root.equals("java/lang/Exception") || root.equals("java/lang/Throwable");
    }

    /**
     * Why the call may run code the analysis does not know (null if it cannot): an unresolved or
     * unmodelled callee, or a library method whose model does not cover this receiver or these
     * arguments.
     */
    private String openWorldReason(MethodInsnNode mi, Frame<AbsValue> f) {
        int op = mi.getOpcode();
        if (mi.owner.startsWith(Program.VERIFIER_PACKAGE)) {
            return isModelledVerifierCall(mi) ? null : "call to " + mi.owner + "." + mi.name;
        }
        Type[] args = Type.getArgumentTypes(mi.desc);
        AbsValue receiver = op == INVOKESTATIC ? null : top(f, args.length);
        if (receiver != null && receiver.isBottom()) return null;
        Program.Targets t = targets(mi);
        if (t == null) return "unresolved call to " + mi.owner + "." + mi.name + mi.desc;
        for (String lib : t.libraryMethods) {
            String[] p = splitMethod(lib);
            JdkModel.Entry e = JdkModel.lookup(p[0], p[1], p[2]);
            if (e == null) return "library call to " + lib;
            boolean viaLibraryOwner = !program.isApp(mi.owner) && op != INVOKESTATIC && !(op == INVOKESPECIAL && mi.name.equals("<init>"));
            boolean exactOwner = receiver != null && p[0].equals(receiver.exactType);
            if (viaLibraryOwner && !JdkModel.cannotBeOverridden(p[0], p[1], p[2]) && !exactOwner)
                return "library call that an override may intercept: " + lib;
            if (e.exactReceiver && !exactOwner) return "library call on a receiver of unknown class: " + lib;
            if (e.condition != JdkModel.Condition.STRING_ARGS) continue;
            for (int a = 0; a < args.length; a++) {
                if (args[a].getSort() != Type.OBJECT && args[a].getSort() != Type.ARRAY) continue;
                AbsValue v = top(f, args.length - 1 - a);
                if (!v.isNull() && !v.isBottom()
                        && (trustedType(v) == null || !JdkModel.SAFE_TO_STRING.contains(trustedType(v))))
                    return "library call that may call toString() of the argument: " + lib;
            }
        }
        return null;
    }

    private String call(MethodInsnNode mi, Frame<AbsValue> f, Map<String, String> mayThrow) {
        String open = openWorldReason(mi, f);
        if (open != null) return open;
        if (mi.owner.startsWith(Program.VERIFIER_PACKAGE)) return null;
        int op = mi.getOpcode();
        Type[] args = Type.getArgumentTypes(mi.desc);
        AbsValue receiver = op == INVOKESTATIC ? null : top(f, args.length);
        if (receiver != null && !receiver.isNonNull()) return "call on a reference that may be null: " + mi.owner + "." + mi.name;
        if (receiver != null && receiver.isBottom()) return null;
        Program.Targets t = targets(mi);
        for (String lib : t.libraryMethods) {
            String[] p = splitMethod(lib);
            JdkModel.Entry e = JdkModel.lookup(p[0], p[1], p[2]);
            if (e.condition != JdkModel.Condition.NONNULL_ARGS) continue;
            for (int a = 0; a < args.length; a++) {
                if (args[a].getSort() != Type.OBJECT && args[a].getSort() != Type.ARRAY) continue;
                if (!top(f, args.length - 1 - a).isNonNull())
                    return "library call with an argument that may be null: " + lib;
            }
        }
        for (Program.Method target : t.methods) {
            if (mayThrow.containsKey(target.key)) return "call to " + target.key;
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Command line: the classpath entries of the program. Prints one JSON object with the result.
     */
    public static void main(String[] args) {
        List<Path> cp = new ArrayList<>();
        for (String a : args) for (String p : a.split(java.io.File.pathSeparator)) if (!p.isEmpty()) cp.add(Path.of(p));
        Result r = check(cp);
        System.out.println("{\"exception_free\": " + r.exceptionFree + ", \"reason\": \"" + escape(r.reason)
                + "\", \"assertion_free\": " + r.assertionFree + ", \"assertion_reason\": \"" + escape(r.assertionReason) + "\"}");
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
