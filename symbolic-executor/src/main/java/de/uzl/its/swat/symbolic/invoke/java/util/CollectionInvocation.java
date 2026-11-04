package de.uzl.its.swat.symbolic.invoke.java.util;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
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
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
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
 * <p>Hashed parts are modelled only while they are concrete: a map key must not be symbolic, and a
 * map whose stored keys have become symbolic is not modelled any more. Index arguments must be
 * concrete too. Anything else falls back to the generic handling of a missing invocation.
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
        put(LIST, Op.ARRAY, -1, -1, "toArray()[Ljava/lang/Object;");
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

        put(VIEW, Op.PLAIN, -1, -1,
                "hasNext()Z", "hasMoreElements()Z", "hasPrevious()Z", "nextIndex()I", "previousIndex()I",
                "size()I", "isEmpty()Z", "remove()V");
        put(VIEW, Op.ELEMENT, -1, -1,
                "next()Ljava/lang/Object;", "nextElement()Ljava/lang/Object;", "previous()Ljava/lang/Object;",
                "getKey()Ljava/lang/Object;", "getValue()Ljava/lang/Object;", "get(I)Ljava/lang/Object;");
        put(VIEW, Op.VIEW, -1, -1, "iterator()Ljava/util/Iterator;", "listIterator()Ljava/util/ListIterator;");
        put(VIEW, Op.ARRAY, -1, -1, "toArray()[Ljava/lang/Object;");
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
        if (isInstance) {
            if (instance == null || instance.isNullValue()) {
                return null;
            }
            ContainerContents contents = instance.getContents();
            Kind kind = contents != null ? contents.kind : kindOf(instance);
            if (kind == null) {
                return null;
            }
            Model model = (switch (kind) {
                case LIST -> LIST;
                case MAP -> MAP;
                case VIEW -> VIEW;
            }).get(name + desc);
            if (model == null) {
                return null;
            }
            return apply(model, kind, owner, name, desc, args, instance, invokeId);
        }
        if (owner.equals("java/util/Collections") || owner.equals("java/util/Arrays")) {
            return invokeStatic(owner, name, desc, args, invokeId);
        }
        return null;
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
        }
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
        }
        instance.setContents(contents);
        InvocationEntry call = new InvocationEntry(owner, name, desc, true, invokeId, true);
        logger.debug("Modelled container call {}.{}{} on {}", owner, name, desc, instance);
        return switch (model.op()) {
            case INIT, STORE, COPY, PLAIN -> PlaceHolder.instance;
            case ELEMENT, STORE_ELEMENT -> {
                if (contents.kind == Kind.VIEW && contents.yieldsEntries && !name.startsWith("get")) {
                    yield new ContainerPlaceHolder(Result.VIEW, contents, false, List.of(), call);
                }
                List<Value<?, ?>> candidates =
                        name.equals("getOrDefault") || name.equals("push") ? List.of(args[args.length - 1]) : List.of();
                yield new ContainerPlaceHolder(Result.ELEMENT, contents, false, candidates, call);
            }
            case VIEW -> new ContainerPlaceHolder(
                    Result.VIEW, contents, name.equals("entrySet") || contents.yieldsEntries, List.of(), call);
            case ARRAY -> new ContainerPlaceHolder(Result.ARRAY, contents, false, List.of(), call);
        };
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
     *   <li>An array returned by {@code toArray()} gets the stored shadows as its elements.
     * </ul>
     */
    public static Value<?, ?> resolve(
            ContainerPlaceHolder ph,
            Object concrete,
            int address,
            ShadowContext stack,
            SymbolicTraceHandler symbolicTraceHandler) throws Exception {
        if (address == ObjectValue.ADDRESS_NULL || concrete == null) {
            return ValueFactory.createNULLValue();
        }
        switch (ph.result) {
            case ELEMENT -> {
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
                ObjectArrayValue array = ValueFactory.createObjectArrayValue("java/lang/Object", size);
                array.setAddress(address);
                boolean unmatched = false;
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i] == null) {
                        array.storeElement(new IntValue(size.context, i), ValueFactory.createNULLValue());
                        continue;
                    }
                    Value<?, ?> match = ph.contents.lookup(System.identityHashCode(elements[i]));
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
