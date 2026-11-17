package de.uzl.its.swat.symbolic.invoke.java.util;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.symbolic.invoke.java.lang.StringCallBranches.Branch;
import de.uzl.its.swat.symbolic.shadow.ShadowContext;
import de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler;
import de.uzl.its.swat.symbolic.value.ContainerPlaceHolder;
import de.uzl.its.swat.symbolic.value.ContainerPlaceHolder.Result;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.ValueFactory;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ContainerContents;
import de.uzl.its.swat.symbolic.value.reference.ContainerContents.Kind;
import de.uzl.its.swat.symbolic.value.reference.HashEntries;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Models of JDK containers ({@code java.util} lists, deques and hash maps, their views and
 * iterators, and a few {@code Collections}/{@code Arrays} factories) that only move references:
 * storing an element, copying a container, returning an element, a view or an iterator.
 *
 * <p>The concrete call still runs; the model records the shadow of what goes in (the {@link
 * ContainerContents} of the container's shadow) and, for a call that returns a reference, picks
 * the shadow of what comes out when the concrete result is known ({@link #resolve}). A call that
 * inspects the elements ({@code contains}, {@code indexOf}, {@code remove(Object)}, sorting,
 * {@code equals}, {@code hashCode}, {@code toString}, streams, ...) is not modelled: a container
 * holding a symbolic value counts as symbolic, so such a call records context loss as before.
 *
 * <p>Hashed parts are modelled by reference only while they are concrete: there a map key must
 * not be symbolic, and a map whose stored keys have become symbolic is not modelled any more. Index
 * arguments must be concrete too. Anything else falls back to the generic handling of a missing
 * invocation.
 *
 * <p><b>Exact hash containers.</b> A {@code HashMap}, {@code LinkedHashMap}, {@code HashSet} or
 * {@code LinkedHashSet} whose creation and every call since were modelled has an exact record of
 * its entries ({@link HashEntries}). With it, calls by key are modelled for symbolic String and
 * boxed integral keys too ({@link #hashCall}): the lookup is recorded as the equality chain {@code
 * key.equals(k_j)} over the stored keys, library branches as those of the String models ({@link
 * #takeBranches}), so the path fixes which entry matched and the solver reaches the others. What
 * the call returns then follows from the record, and is checked against what the JVM returned
 * ({@link #resolve}, {@link #checkPrimitive}).
 *
 * <p>The record is given up ({@link #forget}) by anything that is not modelled exactly: a call
 * outside the tables, a key of another class (its equals is unknown), a call that ran application
 * code or threw, the container (or a view of it) passed to code that is not modelled, a change
 * through a view, a result that does not match. If the container held a symbolic key, giving up is
 * a context loss, and every later call by key is one as well ({@link
 * ContainerContents#keysSymbolic}). The iteration order of a plain hash container depends on the
 * keys' hash codes: iterating (or copying) one with two or more entries and a symbolic key is a
 * context loss, whereas a linked one iterates in the recorded order.
 */
public final class CollectionInvocation {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /** The JDK classes whose shadow tracks contents, with the calls modelled on them. */
    private static final Map<String, Kind> KIND_BY_CLASS = new HashMap<>();

    static {
        for (String c : List.of("ArrayList", "LinkedList", "Vector", "Stack", "ArrayDeque")) {
            KIND_BY_CLASS.put("java/util/" + c, Kind.LIST);
        }
        for (String c : List.of("HashMap", "LinkedHashMap", "Hashtable")) {
            KIND_BY_CLASS.put("java/util/" + c, Kind.MAP);
        }
        for (String c : List.of("HashSet", "LinkedHashSet")) {
            KIND_BY_CLASS.put("java/util/" + c, Kind.SET);
        }
    }

    /** The classes with an exact record of their entries (not Hashtable: it rejects null). */
    private static final Set<String> HASH_CLASSES = Set.of(
            "java/util/HashMap", "java/util/LinkedHashMap", "java/util/HashSet", "java/util/LinkedHashSet");

    /** The calls modelled on the exact entries of a hash container. */
    private enum HashOp {
        INIT, COPY_INIT, COPY, GET, GET_OR_DEFAULT, CONTAINS, PUT, PUT_IF_ABSENT, REMOVE, ADD, SIZE, IS_EMPTY, CLEAR
    }

    private static final Map<String, HashOp> HASH_MAP_OPS = Map.ofEntries(
            Map.entry("<init>()V", HashOp.INIT),
            Map.entry("<init>(I)V", HashOp.INIT),
            Map.entry("<init>(IF)V", HashOp.INIT),
            Map.entry("<init>(Ljava/util/Map;)V", HashOp.COPY_INIT),
            Map.entry("putAll(Ljava/util/Map;)V", HashOp.COPY),
            Map.entry("get(Ljava/lang/Object;)Ljava/lang/Object;", HashOp.GET),
            Map.entry("getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", HashOp.GET_OR_DEFAULT),
            Map.entry("containsKey(Ljava/lang/Object;)Z", HashOp.CONTAINS),
            Map.entry("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", HashOp.PUT),
            Map.entry("putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", HashOp.PUT_IF_ABSENT),
            Map.entry("remove(Ljava/lang/Object;)Ljava/lang/Object;", HashOp.REMOVE),
            Map.entry("size()I", HashOp.SIZE),
            Map.entry("isEmpty()Z", HashOp.IS_EMPTY),
            Map.entry("clear()V", HashOp.CLEAR));

    private static final Map<String, HashOp> HASH_SET_OPS = Map.ofEntries(
            Map.entry("<init>()V", HashOp.INIT),
            Map.entry("<init>(I)V", HashOp.INIT),
            Map.entry("<init>(IF)V", HashOp.INIT),
            Map.entry("<init>(Ljava/util/Collection;)V", HashOp.COPY_INIT),
            Map.entry("addAll(Ljava/util/Collection;)Z", HashOp.COPY),
            Map.entry("add(Ljava/lang/Object;)Z", HashOp.ADD),
            Map.entry("contains(Ljava/lang/Object;)Z", HashOp.CONTAINS),
            Map.entry("remove(Ljava/lang/Object;)Z", HashOp.REMOVE),
            Map.entry("size()I", HashOp.SIZE),
            Map.entry("isEmpty()Z", HashOp.IS_EMPTY),
            Map.entry("clear()V", HashOp.CLEAR));

    /** The immutable empty containers of {@code Collections.emptyMap()}, {@code emptySet()}, {@code emptyList()}. */
    private static final Set<String> EMPTY_CLASSES = Set.of(
            "java/util/Collections$EmptyMap", "java/util/Collections$EmptySet", "java/util/Collections$EmptyList");

    /** Lookups whose result in an empty container does not depend on the argument. */
    private static final Set<String> EMPTY_LOOKUPS = Set.of(
            "get(Ljava/lang/Object;)Ljava/lang/Object;", "containsKey(Ljava/lang/Object;)Z",
            "containsValue(Ljava/lang/Object;)Z", "contains(Ljava/lang/Object;)Z");

    /** Returned by {@link #hashCall} for a call that is left to the generic handling. */
    private static final Value<?, ?> NOT_MODELLED = new PlaceHolder(false);

    /** The branches of the last modelled call (the equality chain of a lookup by key). */
    private static final ThreadLocal<List<Branch>> BRANCHES = ThreadLocal.withInitial(ArrayList::new);

    /**
     * The library branches the model of the last call relies on, for the visitor to record: on
     * the path, they fix which entry of a hash container a lookup found.
     */
    public static List<Branch> takeBranches() {
        List<Branch> pending = BRANCHES.get();
        if (pending.isEmpty()) {
            return List.of();
        }
        List<Branch> taken = List.copyOf(pending);
        pending.clear();
        return taken;
    }

    private enum Op {
        INIT, // a new, empty container
        STORE, // stores argument `arg` (lists) or the value `arg` under key `key` (maps)
        COPY, // stores what the container in argument `arg` holds
        ELEMENT, // returns an element
        STORE_ELEMENT, // STORE, then returns an element (set, put, Stack.push)
        VIEW, // returns a view or iterator of the container
        ARRAY, // returns a new Object[] with the elements
        PLAIN // returns a primitive or nothing and only depends on concrete state
    }

    /**
     * @param op what the call does
     * @param arg the argument stored or copied, -1 for none
     * @param key the map-key argument, which must be concrete, -1 for none
     * @param initFirst whether the call is a constructor that starts with empty contents
     */
    private record Model(Op op, int arg, int key, boolean initFirst) {}

    private static final Map<String, Model> LIST = new HashMap<>();
    private static final Map<String, Model> MAP = new HashMap<>();
    private static final Map<String, Model> VIEW = new HashMap<>();
    /** On a hash set with exact entries, next to the calls by element ({@link #HASH_SET_OPS}). */
    private static final Map<String, Model> SET = new HashMap<>();

    private static void put(Map<String, Model> table, Op op, int arg, int key, String... sigs) {
        for (String sig : sigs) table.put(sig, new Model(op, arg, key, sig.startsWith("<init>")));
    }

    static {
        put(LIST, Op.INIT, -1, -1, "<init>()V", "<init>(I)V");
        put(LIST, Op.COPY, 0, -1, "<init>(Ljava/util/Collection;)V", "addAll(Ljava/util/Collection;)Z");
        put(LIST, Op.COPY, 1, -1, "addAll(ILjava/util/Collection;)Z");
        put(LIST, Op.STORE, 0, -1,
                "add(Ljava/lang/Object;)Z", "addLast(Ljava/lang/Object;)V", "addFirst(Ljava/lang/Object;)V",
                "offer(Ljava/lang/Object;)Z", "offerLast(Ljava/lang/Object;)Z", "offerFirst(Ljava/lang/Object;)Z",
                "push(Ljava/lang/Object;)V", "addElement(Ljava/lang/Object;)V",
                "insertElementAt(Ljava/lang/Object;I)V", "setElementAt(Ljava/lang/Object;I)V");
        put(LIST, Op.STORE, 1, -1, "add(ILjava/lang/Object;)V");
        put(LIST, Op.STORE_ELEMENT, 1, -1, "set(ILjava/lang/Object;)Ljava/lang/Object;");
        put(LIST, Op.STORE_ELEMENT, 0, -1, "push(Ljava/lang/Object;)Ljava/lang/Object;");
        put(LIST, Op.ELEMENT, -1, -1,
                "get(I)Ljava/lang/Object;", "getFirst()Ljava/lang/Object;", "getLast()Ljava/lang/Object;",
                "element()Ljava/lang/Object;", "peek()Ljava/lang/Object;", "peekFirst()Ljava/lang/Object;",
                "peekLast()Ljava/lang/Object;", "poll()Ljava/lang/Object;", "pollFirst()Ljava/lang/Object;",
                "pollLast()Ljava/lang/Object;", "pop()Ljava/lang/Object;", "remove()Ljava/lang/Object;",
                "remove(I)Ljava/lang/Object;", "removeFirst()Ljava/lang/Object;", "removeLast()Ljava/lang/Object;",
                "firstElement()Ljava/lang/Object;", "lastElement()Ljava/lang/Object;",
                "elementAt(I)Ljava/lang/Object;");
        put(LIST, Op.VIEW, -1, -1,
                "iterator()Ljava/util/Iterator;", "listIterator()Ljava/util/ListIterator;",
                "listIterator(I)Ljava/util/ListIterator;", "descendingIterator()Ljava/util/Iterator;",
                "elements()Ljava/util/Enumeration;", "subList(II)Ljava/util/List;");
        put(LIST, Op.ARRAY, -1, -1, "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;");
        put(LIST, Op.PLAIN, -1, -1,
                "size()I", "isEmpty()Z", "clear()V", "trimToSize()V", "ensureCapacity(I)V");

        put(MAP, Op.INIT, -1, -1, "<init>()V", "<init>(I)V", "<init>(IF)V");
        put(MAP, Op.COPY, 0, -1, "<init>(Ljava/util/Map;)V", "putAll(Ljava/util/Map;)V");
        put(MAP, Op.STORE_ELEMENT, 1, 0,
                "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        put(MAP, Op.ELEMENT, -1, 0,
                "get(Ljava/lang/Object;)Ljava/lang/Object;", "remove(Ljava/lang/Object;)Ljava/lang/Object;",
                "getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        put(MAP, Op.PLAIN, -1, 0, "containsKey(Ljava/lang/Object;)Z");
        put(MAP, Op.VIEW, -1, -1,
                "keySet()Ljava/util/Set;", "values()Ljava/util/Collection;", "entrySet()Ljava/util/Set;",
                "keys()Ljava/util/Enumeration;", "elements()Ljava/util/Enumeration;");
        put(MAP, Op.PLAIN, -1, -1, "size()I", "isEmpty()Z", "clear()V");

        put(SET, Op.VIEW, -1, -1, "iterator()Ljava/util/Iterator;");
        put(SET, Op.ARRAY, -1, -1, "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;");

        put(VIEW, Op.PLAIN, -1, -1,
                "hasNext()Z", "hasMoreElements()Z", "hasPrevious()Z", "nextIndex()I", "previousIndex()I",
                "size()I", "isEmpty()Z", "remove()V");
        put(VIEW, Op.ELEMENT, -1, -1,
                "next()Ljava/lang/Object;", "nextElement()Ljava/lang/Object;", "previous()Ljava/lang/Object;",
                "getKey()Ljava/lang/Object;", "getValue()Ljava/lang/Object;", "get(I)Ljava/lang/Object;");
        put(VIEW, Op.VIEW, -1, -1, "iterator()Ljava/util/Iterator;", "listIterator()Ljava/util/ListIterator;");
        put(VIEW, Op.ARRAY, -1, -1, "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;");
    }

    private CollectionInvocation() {}

    /**
     * Applies the model of a call that was not instrumented.
     *
     * @return the return value of the call, or {@code null} if the call is not modelled
     */
    public static Value<?, ?> invoke(
            String owner,
            String name,
            String desc,
            Value<?, ?>[] args,
            boolean isInstance,
            ObjectValue<?, ?> instance,
            long invokeId) throws NotImplementedException {
        BRANCHES.get().clear();
        if (isInstance) {
            if (instance == null || instance.isNullValue()) {
                return null;
            }
            ContainerContents contents = instance.getContents();
            Kind kind = contents != null ? contents.kind : kindOf(instance);
            if (kind == null) {
                if (contents == null && instance.className != null
                        && EMPTY_CLASSES.contains(instance.className.replace('.', '/'))
                        && EMPTY_LOOKUPS.contains(name + desc)) {
                    // Collections.emptyMap/emptySet/emptyList: nothing is a key or an element, so
                    // the lookup returns null or false whatever its argument is (taken from the JVM)
                    logger.debug("Modelled lookup {}.{}{} in an empty immutable container", owner, name, desc);
                    return PlaceHolder.instance;
                }
                return null;
            }
            if (kind == Kind.MAP || kind == Kind.SET) {
                Value<?, ?> result;
                try {
                    result = hashCall(kind, owner, name, desc, args, instance, invokeId);
                } catch (RuntimeException e) {
                    // The record may be half updated: give it up
                    logger.warn("Exact model of {}.{}{} failed", owner, name, desc, e);
                    BRANCHES.get().clear();
                    forget(instance.getContents(), owner, name, desc, invokeId);
                    result = kind == Kind.SET ? NOT_MODELLED : null;
                }
                if (result != null) {
                    return result == NOT_MODELLED ? null : result;
                }
                contents = instance.getContents();
            }
            Model model = (switch (kind) {
                case LIST -> LIST;
                case MAP -> MAP;
                case SET -> SET;
                case VIEW -> VIEW;
            }).get(name + desc);
            if (kind == Kind.SET && (contents == null || contents.hash() == null)) {
                model = null; // a set is modelled only while its exact entries are known
            }
            if (model == null) {
                // Not modelled: the call may have changed the container (or the one behind a view)
                forget(contents, owner, name, desc, invokeId);
                return null;
            }
            if (kind == Kind.VIEW && name.equals("remove")) {
                forget(contents, owner, name, desc, invokeId); // Iterator.remove changes the container behind it
            }
            Value<?, ?> result = apply(model, kind, owner, name, desc, args, instance, invokeId);
            if (result == null) {
                forget(instance.getContents(), owner, name, desc, invokeId); // not modelled after all
            }
            return result;
        }
        if (owner.equals("java/util/Collections") || owner.equals("java/util/Arrays")) {
            return invokeStatic(owner, name, desc, args, invokeId);
        }
        return null;
    }

    /**
     * A call on a hash map or set by key (or element), modelled on its exact entries.
     *
     * @return the result of the call; {@link #NOT_MODELLED} to leave the call to the generic
     *     handling; {@code null} for a call the general model of {@link #apply} decides (a call
     *     that is not by key, a map without exact entries, or a map call with concrete keys that
     *     returns a reference, which is found by its address as before: the entries are updated)
     */
    private static Value<?, ?> hashCall(
            Kind kind,
            String owner,
            String name,
            String desc,
            Value<?, ?>[] args,
            ObjectValue<?, ?> instance,
            long invokeId) throws NotImplementedException {
        HashOp op = (kind == Kind.MAP ? HASH_MAP_OPS : HASH_SET_OPS).get(name + desc);
        if (op == null) {
            return null;
        }
        InvocationEntry call = new InvocationEntry(owner, name, desc, true, invokeId, true);
        String className = instance.className == null ? "" : instance.className.replace('.', '/');
        boolean linked = className.startsWith("java/util/Linked");
        List<Branch> branches = new ArrayList<>();
        if (op == HashOp.INIT || op == HashOp.COPY_INIT) {
            if (!HASH_CLASSES.contains(className)) {
                return kind == Kind.SET ? NOT_MODELLED : null;
            }
            if (op == HashOp.INIT && kind == Kind.MAP) {
                return null; // apply creates the contents, with the entries
            }
            ContainerContents fresh = new ContainerContents(kind);
            HashEntries entries = new HashEntries(linked);
            fresh.setHash(entries);
            if (op == HashOp.COPY_INIT && copyInto(kind, fresh, entries, args[0], branches) == null) {
                // A source without exact entries: a map as before, a set is not modelled
                return kind == Kind.SET ? NOT_MODELLED : null;
            }
            instance.setContents(fresh);
            BRANCHES.get().addAll(branches);
            logger.debug("Modelled hash container call {}.{}{} on {}", owner, name, desc, instance);
            return PlaceHolder.instance;
        }
        ContainerContents contents = instance.getContents();
        HashEntries entries = contents == null ? null : contents.hash();
        if (entries == null) {
            return kind == Kind.SET ? NOT_MODELLED : null;
        }
        switch (op) {
            case SIZE -> {
                return ContainerPlaceHolder.exactPrimitive(contents, entries.size(), contents.hasSymbolicKeys(), call);
            }
            case IS_EMPTY -> {
                return ContainerPlaceHolder.exactPrimitive(contents, entries.size() == 0, contents.hasSymbolicKeys(), call);
            }
            case CLEAR -> {
                entries.clear();
                return PlaceHolder.instance;
            }
            case COPY -> {
                Boolean changed = copyInto(kind, contents, entries, args[0], branches);
                if (changed == null) {
                    return lose(kind, contents, call);
                }
                BRANCHES.get().addAll(branches);
                logger.debug("Modelled hash container call {}.{}{} on {}", owner, name, desc, instance);
                return kind == Kind.SET
                        ? ContainerPlaceHolder.exactPrimitive(contents, changed, !branches.isEmpty(), call)
                        : PlaceHolder.instance;
            }
            default -> { }
        }
        // A call by key
        Value<?, ?> key = args[0];
        boolean symbolic = contents.hasSymbolicKeys() || key.isSymbolic();
        if (!HashEntries.supported(key) || (symbolic && entries.size() > HashEntries.MAX_ENTRIES)) {
            return lose(kind, contents, call);
        }
        int at = entries.find(key, entries.size(), branches);
        if (at == -2) {
            return lose(kind, contents, call);
        }
        HashEntries.Entry found = at >= 0 ? entries.entries().get(at) : null;
        Value<?, ?> result = found == null ? null : found.value;
        switch (op) {
            case GET -> { }
            case GET_OR_DEFAULT -> {
                if (found == null) result = args[1];
            }
            case PUT -> {
                if (found != null) {
                    found.value = args[1];
                } else {
                    entries.append(key, args[1]);
                    if (symbolic) contents.addKey(key);
                }
                if (symbolic) contents.addValue(args[1]);
            }
            case PUT_IF_ABSENT -> {
                if (found == null) {
                    entries.append(key, args[1]);
                    if (symbolic) {
                        contents.addKey(key);
                        contents.addValue(args[1]);
                    }
                } else if (!(found.value instanceof ObjectValue<?, ?> old)) {
                    return lose(kind, contents, call); // cannot tell whether the old value is null
                } else if (old.isNullValue()) {
                    found.value = args[1]; // a key mapped to null counts as absent
                    if (symbolic) contents.addValue(args[1]);
                }
            }
            case REMOVE -> {
                if (found != null) entries.remove(at);
            }
            case ADD -> {
                if (found == null) {
                    entries.append(key, null);
                    contents.addElement(key);
                }
            }
            case CONTAINS -> { }
            default -> throw new IllegalStateException("Not a call by key: " + op);
        }
        BRANCHES.get().addAll(branches);
        logger.debug("Modelled hash container call {}.{}{} on {}: entry {}", owner, name, desc, instance, at);
        if (op == HashOp.CONTAINS || (kind == Kind.SET && op == HashOp.REMOVE)) {
            return ContainerPlaceHolder.exactPrimitive(contents, found != null, symbolic, call);
        }
        if (op == HashOp.ADD) {
            return ContainerPlaceHolder.exactPrimitive(contents, found == null, symbolic, call);
        }
        if (!symbolic) {
            return null; // concrete keys only: the reference returned is found by its address, as before
        }
        return ContainerPlaceHolder.exactElement(contents, result, call);
    }

    /**
     * Adds what the container in {@code arg} holds to exact entries, as {@code putAll}, {@code
     * addAll} and the copy constructors do: each key is looked up and stored like a single one.
     * The source must have exact contents itself: a map or set with exact entries, or (into a
     * set) a list whose order is known.
     *
     * @return whether an entry was added, or {@code null} if the copy cannot be modelled exactly
     *     (the entries may then be half updated and must be given up)
     */
    private static Boolean copyInto(
            Kind kind, ContainerContents contents, HashEntries entries, Value<?, ?> arg, List<Branch> branches)
            throws NotImplementedException {
        if (!(arg instanceof ObjectValue<?, ?> ov) || ov.isNullValue() || ov.getContents() == null) {
            return null;
        }
        ContainerContents source = ov.getContents();
        if (source == contents) {
            return false;
        }
        List<Value<?, ?>> keys = new ArrayList<>();
        List<Value<?, ?>> values = new ArrayList<>();
        boolean distinct; // the keys are pairwise different, so each is new or equals an old entry
        boolean inOrder; // the keys are listed in the order the source iterates
        if (source.hash() != null && source.kind == kind) {
            for (HashEntries.Entry e : source.hash().entries()) {
                keys.add(e.key);
                values.add(e.value);
            }
            distinct = true;
            inOrder = source.hash().iteratesInOrder();
        } else if (kind == Kind.SET && source.kind == Kind.LIST && source.exactOrdered() != null) {
            for (Value<?, ?> element : source.exactOrdered()) {
                keys.add(element);
                values.add(null);
            }
            distinct = false;
            inOrder = true;
        } else {
            return null;
        }
        boolean symbolic = contents.hasSymbolicKeys();
        for (Value<?, ?> key : keys) {
            if (key == null || !HashEntries.supported(key)) {
                return null;
            }
            symbolic |= key.isSymbolic();
        }
        if (symbolic && entries.size() + keys.size() > HashEntries.MAX_ENTRIES) {
            return null;
        }
        int before = entries.size();
        int added = 0;
        for (int i = 0; i < keys.size(); i++) {
            Value<?, ?> key = keys.get(i);
            int at = entries.find(key, distinct ? before : entries.size(), branches);
            if (at == -2) {
                return null;
            }
            if (at < 0) {
                entries.append(key, values.get(i));
                added++;
                if (kind == Kind.MAP) {
                    contents.addKey(key);
                } else {
                    contents.addElement(key);
                }
            } else if (kind == Kind.MAP) {
                entries.entries().get(at).value = values.get(i);
            }
            if (kind == Kind.MAP) {
                contents.addValue(values.get(i));
            }
        }
        if (added >= 2 && !inOrder) {
            entries.forgetOrder(); // a linked container got them in the source's (hash) order
        }
        return added > 0;
    }

    /** Gives up the exact entries at a call that is then handled without them. */
    private static Value<?, ?> lose(Kind kind, ContainerContents contents, InvocationEntry call) {
        forget(contents, call);
        return kind == Kind.SET ? NOT_MODELLED : null;
    }

    private static void forget(ContainerContents contents, String owner, String name, String desc, long invokeId) {
        if (contents != null) {
            forget(contents, new InvocationEntry(owner, name, desc, true, invokeId, true));
        }
    }

    /**
     * Gives up the exact entries of a hash container (and of those behind a view), because {@code
     * call} is not modelled exactly. If one held a symbolic key, nothing on the path says what it
     * holds from here on: a context loss.
     */
    private static void forget(ContainerContents contents, InvocationEntry call) {
        if (contents != null && contents.forgetHash()) {
            contextLoss(call, "gives up the exact entries of a hash container with symbolic keys");
        }
    }

    /**
     * A call that is not modelled got {@code value}: if it is a hash container with exact entries
     * (or a view of one), the call may have changed it, so the entries are given up.
     */
    public static void escaped(Value<?, ?> value, String owner, String name, String desc, long invokeId) {
        if (value instanceof ObjectValue<?, ?> ov && !ov.isNullValue() && ov.getContents() != null) {
            forget(ov.getContents(), new InvocationEntry(owner, name, desc, false, invokeId, true));
        }
    }

    private static void contextLoss(InvocationEntry call, String why) {
        logger.warn("{}.{}{} {}, causes context loss", call.owner(), call.name(), call.desc(), why);
        try {
            long threadId = Thread.currentThread().getId();
            ThreadHandler.getSymbolicTraceHandler(threadId).recordSymbolicContextLoss();
            ThreadHandler.recordMissingInvocation(threadId, call);
            ThreadHandler.recordContextLossInvocation(threadId, call);
        } catch (NoThreadContextException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Checks the boolean or int a call on exact entries returned against what the entries say. A
     * difference means the record is not what the JVM holds: it is given up.
     */
    public static void checkPrimitive(ContainerPlaceHolder ph, Object concrete) {
        if (!ph.exact || ph.result != Result.PRIMITIVE) {
            return;
        }
        Object expected = ph.expected;
        boolean same = expected instanceof Boolean b
                ? (concrete instanceof Boolean c ? c.equals(b) : concrete instanceof Number n && (n.intValue() != 0) == b)
                : concrete instanceof Number n && expected instanceof Integer i && n.intValue() == i;
        if (!same) {
            mismatch(ph, concrete);
        }
    }

    private static void mismatch(ContainerPlaceHolder ph, Object concrete) {
        logger.warn("Result of {}.{}{} is {}, the exact entries say {}",
                ph.call.owner(), ph.call.name(), ph.call.desc(), concrete, ph.expected);
        // Also if the entries were given up since: the call's branches and result relied on them
        boolean symbolic = ph.symbolic || ph.contents.hasSymbolicKeys();
        ph.contents.forgetHash();
        if (symbolic) {
            contextLoss(ph.call, "returned something else than its exact entries say");
        }
    }

    /**
     * The shadow of the element the exact entries say a call returned, if the concrete result is
     * that element; {@code null} if it is not.
     */
    private static Value<?, ?> resolveExact(ContainerPlaceHolder ph, Object concrete, int address, ShadowContext stack)
            throws Exception {
        boolean isNull = address == ObjectValue.ADDRESS_NULL || concrete == null;
        if (ph.expected == null || (ph.expected instanceof ObjectValue<?, ?> n && n.isNullValue())) {
            return isNull ? ValueFactory.createNULLValue() : null;
        }
        if (isNull || !(ph.expected instanceof ObjectValue<?, ?> expected)) {
            return null;
        }
        if (expected instanceof StringValue s) {
            if (!(concrete instanceof String) || !concrete.equals(s.concrete)) return null;
        } else if (expected instanceof BoxedValue<?> b) {
            if (b.getVal() == null || b.getVal().concrete == null || !concrete.equals(b.getVal().concrete)) return null;
        } else if ((expected.getAddress() != ObjectValue.ADDRESS_UNKNOWN && expected.getAddress() != address)
                || !matches(expected, concrete)) {
            return null;
        }
        if (expected.getAddress() == ObjectValue.ADDRESS_UNKNOWN) {
            expected.setAddress(address);
            stack.putToHeap(address, expected);
        }
        return expected;
    }

    /** Whether calls on this object are modelled as a JDK container's (see {@link #invoke}). */
    public static boolean isContainer(ObjectValue<?, ?> instance) {
        return instance.getContents() != null || kindOf(instance) != null;
    }

    private static Kind kindOf(ObjectValue<?, ?> instance) {
        if (instance.className == null || instance instanceof AbstractArrayValue<?, ?, ?, ?, ?>) {
            return null;
        }
        return KIND_BY_CLASS.get(instance.className.replace('.', '/'));
    }

    private static Value<?, ?> apply(
            Model model,
            Kind kind,
            String owner,
            String name,
            String desc,
            Value<?, ?>[] args,
            ObjectValue<?, ?> instance,
            long invokeId) throws NotImplementedException {
        // Every argument the model does not store must be concrete: indices, sizes, keys
        for (int i = 0; i < args.length; i++) {
            if (i != model.arg() && args[i].isSymbolic()) {
                return null;
            }
        }
        ContainerContents contents = instance.getContents();
        if (model.initFirst() || contents == null) {
            contents = new ContainerContents(kind);
            if (!model.initFirst()) {
                contents.forgetOrder(); // a list SWAT did not see created: its earlier elements are unknown
            }
        }
        if (model.op() == Op.INIT && kind == Kind.MAP && instance.className != null
                && HASH_CLASSES.contains(instance.className.replace('.', '/'))) {
            // A new hash map: its exact entries are known (none), see hashCall
            contents.setHash(new HashEntries(instance.className.replace('.', '/').startsWith("java/util/Linked")));
        }
        trackOrder(model, name + desc, contents);
        if (kind == Kind.MAP && contents.keysSymbolic(new HashSet<>())) {
            return null;
        }
        Value<?, ?> stored = model.arg() >= 0 ? args[model.arg()] : null;
        if (model.op() == Op.COPY) {
            ContainerContents source = sourceOf(stored, kind == Kind.MAP);
            if (source == null) {
                return null;
            }
            contents.addSource(source);
        }
        if (model.op() == Op.STORE || model.op() == Op.STORE_ELEMENT) {
            if (model.key() >= 0) {
                contents.addKey(args[model.key()]);
            }
            contents.addValue(stored);
            if (contents.ordered() != null && APPENDS.contains(name + desc)) {
                List<Value<?, ?>> ordered = contents.ordered();
                ordered.set(ordered.size() - 1, stored);
            }
        }
        instance.setContents(contents);
        InvocationEntry call = new InvocationEntry(owner, name, desc, true, invokeId, true);
        if (model.op() != Op.PLAIN && model.op() != Op.VIEW && model.op() != Op.INIT && model.op() != Op.STORE
                && contents.hashOrderUnknown()) {
            // Which element a view, an iterator or a copy of a hash container yields next depends
            // on the hash codes of its symbolic keys, which the path does not fix. (How many it
            // yields does not: sizes and hasNext are exact.)
            contextLoss(call, "depends on the iteration order of a hash container with symbolic keys");
        }
        logger.debug("Modelled container call {}.{}{} on {}", owner, name, desc, instance);
        return switch (model.op()) {
            case INIT, STORE, COPY, PLAIN -> PlaceHolder.instance;
            case ELEMENT, STORE_ELEMENT -> {
                if (contents.kind == Kind.VIEW && contents.yieldsEntries && !name.startsWith("get")) {
                    yield new ContainerPlaceHolder(Result.VIEW, contents, false, List.of(), call);
                }
                List<Value<?, ?>> candidates =
                        name.equals("getOrDefault") || name.equals("push") ? List.of(args[args.length - 1]) : List.of();
                Value<?, ?> positional = positional(contents, name + desc, args);
                if (positional != null) {
                    candidates = List.of(positional);
                }
                yield new ContainerPlaceHolder(Result.ELEMENT, contents, false, candidates, call);
            }
            case VIEW -> new ContainerPlaceHolder(
                    Result.VIEW, contents, name.equals("entrySet") || contents.yieldsEntries, List.of(), call);
            case ARRAY -> new ContainerPlaceHolder(Result.ARRAY, contents, false,
                    contents.ordered() == null ? List.of() : List.copyOf(contents.ordered()), call);
        };
    }

    /** Calls that append at the end of a list: the only stores that keep its order known. */
    private static final Set<String> APPENDS = Set.of(
            "add(Ljava/lang/Object;)Z", "addLast(Ljava/lang/Object;)V", "offer(Ljava/lang/Object;)Z",
            "offerLast(Ljava/lang/Object;)Z", "addElement(Ljava/lang/Object;)V");

    /** Calls that read an element by position without changing the list. */
    private static final Set<String> POSITIONAL_READS = Set.of("get(I)Ljava/lang/Object;", "elementAt(I)Ljava/lang/Object;");

    /** Calls that neither change a list nor its order. */
    private static final Set<String> ORDER_NEUTRAL = Set.of(
            "get(I)Ljava/lang/Object;", "elementAt(I)Ljava/lang/Object;", "getFirst()Ljava/lang/Object;",
            "getLast()Ljava/lang/Object;", "element()Ljava/lang/Object;", "peek()Ljava/lang/Object;",
            "peekFirst()Ljava/lang/Object;", "peekLast()Ljava/lang/Object;", "firstElement()Ljava/lang/Object;",
            "lastElement()Ljava/lang/Object;", "size()I", "isEmpty()Z", "trimToSize()V", "ensureCapacity(I)V",
            "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;",
            "<init>()V", "<init>(I)V");

    /** Keeps a list's order (ContainerContents.ordered) up to date with a modelled call. */
    private static void trackOrder(Model model, String sig, ContainerContents contents) {
        if (contents.kind != Kind.LIST) {
            return;
        }
        if (APPENDS.contains(sig)) {
            contents.appendOrdered(null); // filled in below, once the stored value is known
        } else if (sig.equals("clear()V")) {
            contents.clearOrdered();
        } else if (!ORDER_NEUTRAL.contains(sig)) {
            contents.forgetOrder(); // inserts, removals, sets, copies, views (which can remove)
        }
    }

    /** The shadow stored at the index a positional read asks for, or null if unknown. */
    private static Value<?, ?> positional(ContainerContents contents, String sig, Value<?, ?>[] args) {
        List<Value<?, ?>> ordered = contents.ordered();
        if (ordered == null || !POSITIONAL_READS.contains(sig) || args.length != 1
                || !(args[0] instanceof IntValue index) || index.isSymbolic()) {
            return null;
        }
        int i = index.concrete;
        return i >= 0 && i < ordered.size() ? ordered.get(i) : null;
    }

    private static Value<?, ?> invokeStatic(String owner, String name, String desc, Value<?, ?>[] args, long invokeId)
            throws NotImplementedException {
        InvocationEntry call = new InvocationEntry(owner, name, desc, false, invokeId, true);
        if (owner.equals("java/util/Arrays")) {
            if (name.equals("asList") && desc.equals("([Ljava/lang/Object;)Ljava/util/List;")
                    && args[0] instanceof ObjectArrayValue array) {
                ContainerContents view = new ContainerContents(Kind.VIEW);
                view.addSource(array);
                return new ContainerPlaceHolder(Result.VIEW, view, false, List.of(), call);
            }
            return null;
        }
        switch (name + desc) {
            case "singleton(Ljava/lang/Object;)Ljava/util/Set;",
                    "singletonList(Ljava/lang/Object;)Ljava/util/List;" -> {
                ContainerContents view = new ContainerContents(Kind.VIEW);
                view.addValue(args[0]);
                return new ContainerPlaceHolder(Result.VIEW, view, false, List.of(), call);
            }
            case "enumeration(Ljava/util/Collection;)Ljava/util/Enumeration;",
                    "unmodifiableCollection(Ljava/util/Collection;)Ljava/util/Collection;",
                    "unmodifiableList(Ljava/util/List;)Ljava/util/List;",
                    "unmodifiableSet(Ljava/util/Set;)Ljava/util/Set;",
                    "unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;" -> {
                ContainerContents source = sourceOf(args[0], false);
                if (source == null) {
                    return null;
                }
                return new ContainerPlaceHolder(Result.VIEW, source, source.yieldsEntries, List.of(), call);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * The contents of a container argument that is copied or viewed: its recorded contents, or
     * empty contents for a container that holds nothing symbolic. {@code null} (not modelled) for
     * an untracked container that is symbolic, or, when the copy goes into a map, for a source that
     * is not a map or whose keys may be symbolic.
     */
    private static ContainerContents sourceOf(Value<?, ?> arg, boolean intoMap) throws NotImplementedException {
        if (!(arg instanceof ObjectValue<?, ?> ov) || ov.isNullValue()) {
            return null;
        }
        ContainerContents source = ov.getContents();
        if (source == null) {
            return ov.isSymbolic() ? null : new ContainerContents(Kind.VIEW);
        }
        if (intoMap && (source.kind != Kind.MAP || source.keysSymbolic(new HashSet<>()))) {
            return null;
        }
        return source;
    }

    /**
     * Picks the shadow of the object a modelled call returned, once the concrete object is known.
     *
     * <ul>
     *   <li>An element is the shadow stored under the object's address. If there is none, the
     *       shadow is recovered as for any unmodelled call; if the container may hold a symbolic
     *       value, that recovery may have lost it, which is recorded as context loss.
     *   <li>A view or iterator gets contents backed by the container's.
     *   <li>An array returned by {@code toArray()} or {@code toArray(T[])} gets the stored shadows
     *       as its elements, with the concrete array's element type.
     * </ul>
     */
    public static Value<?, ?> resolve(
            ContainerPlaceHolder ph,
            Object concrete,
            int address,
            ShadowContext stack,
            SymbolicTraceHandler symbolicTraceHandler) throws Exception {
        if (ph.exact && ph.result == Result.ELEMENT) {
            Value<?, ?> element = resolveExact(ph, concrete, address, stack);
            if (element != null) {
                return element;
            }
            mismatch(ph, concrete);
        }
        if (address == ObjectValue.ADDRESS_NULL || concrete == null) {
            return ValueFactory.createNULLValue();
        }
        switch (ph.result) {
            case ELEMENT -> {
                // The one shadow stored under the address; if there are several (one object stored
                // twice with different shadows), the candidate: for get(i) the one at that position
                Value<?, ?> match = ph.contents.lookup(address);
                if (match == null) {
                    for (Value<?, ?> c : ph.candidates) {
                        if (c instanceof ObjectValue<?, ?> ov && ov.getAddress() == address) match = c;
                    }
                }
                if (match != null && matches(match, concrete)) {
                    return match;
                }
                Value<?, ?> recovered = recover(concrete, address, stack);
                if (ph.contents.isSymbolic(new HashSet<>())) {
                    recordContextLoss(ph.call, symbolicTraceHandler);
                }
                return recovered;
            }
            case VIEW -> {
                Value<?, ?> recovered = recover(concrete, address, stack);
                if (recovered instanceof ObjectValue<?, ?> ov
                        && !(ov instanceof StringValue)
                        && !(ov instanceof BoxedValue<?>)
                        && !(ov instanceof AbstractArrayValue<?, ?, ?, ?, ?>)
                        && !(ov instanceof ObjectArrayValue)) {
                    if (ov.getContents() == null) {
                        ov.setContents(ContainerContents.viewOf(ph.contents, ph.yieldsEntries));
                    } else {
                        ov.getContents().addSource(ph.contents);
                    }
                } else if (ph.contents.isSymbolic(new HashSet<>())) {
                    recordContextLoss(ph.call, symbolicTraceHandler);
                }
                return recovered;
            }
            case ARRAY -> {
                if (!(concrete instanceof Object[] elements)) {
                    return recover(concrete, address, stack);
                }
                long threadId = Thread.currentThread().getId();
                IntValue size = new IntValue(ThreadHandler.getSolverContext(threadId), elements.length);
                // toArray(T[]) returns an array of T (the argument itself if it is large enough)
                String elementType = elements.getClass().getComponentType().getName().replace('.', '/');
                ObjectArrayValue array = ValueFactory.createObjectArrayValue(elementType, size);
                array.setAddress(address);
                boolean unmatched = false;
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i] == null) {
                        array.storeElement(new IntValue(size.context, i), ValueFactory.createNULLValue());
                        continue;
                    }
                    int elementAddress = System.identityHashCode(elements[i]);
                    // The shadow stored at this position, if the list's order is known and it was
                    // stored for this very object; else the one stored under its address
                    Value<?, ?> match = null;
                    if (ph.candidates.size() == elements.length
                            && ph.candidates.get(i) instanceof ObjectValue<?, ?> ov && ov.getAddress() == elementAddress) {
                        match = ph.candidates.get(i);
                    }
                    if (match == null) {
                        match = ph.contents.lookup(elementAddress);
                    }
                    if (match != null && matches(match, elements[i])) {
                        array.storeElement(new IntValue(size.context, i), match);
                    } else {
                        // left unset: read back like any element of an untracked array
                        unmatched = true;
                    }
                }
                if (unmatched && ph.contents.isSymbolic(new HashSet<>())) {
                    recordContextLoss(ph.call, symbolicTraceHandler);
                }
                stack.putToHeap(address, array);
                return array;
            }
            default -> { }
        }
        return recover(concrete, address, stack);
    }

    /** Whether a stored shadow can stand for the concrete object (guards against address reuse). */
    private static boolean matches(Value<?, ?> shadow, Object concrete) {
        if (shadow instanceof StringValue s) {
            return concrete instanceof String && concrete.equals(s.concrete);
        }
        if (shadow instanceof ObjectValue<?, ?> ov && ov.className != null && !(ov instanceof BoxedValue<?>)
                && !(ov instanceof AbstractArrayValue<?, ?, ?, ?, ?>) && !(ov instanceof ObjectArrayValue)) {
            return ov.className.replace('.', '/').equals(concrete.getClass().getName().replace('.', '/'));
        }
        return true;
    }

    /** What {@code GETVALUE_Object} does for the result of an unmodelled call. */
    private static Value<?, ?> recover(Object concrete, int address, ShadowContext stack) throws Exception {
        Value<?, ?> heap = stack.getFromHeap(address);
        if (heap != null) {
            return heap;
        }
        Value<?, ?> created = ValueFactory.createObjectValue(concrete, address);
        stack.putToHeap(address, created);
        return created;
    }

    private static void recordContextLoss(InvocationEntry call, SymbolicTraceHandler symbolicTraceHandler)
            throws NoThreadContextException {
        logger.warn("Result of {}.{}{} could not be matched to a stored shadow, causes context loss",
                call.owner(), call.name(), call.desc());
        symbolicTraceHandler.recordSymbolicContextLoss();
        long threadId = Thread.currentThread().getId();
        ThreadHandler.recordMissingInvocation(threadId, call);
        ThreadHandler.recordContextLossInvocation(threadId, call);
    }
}
