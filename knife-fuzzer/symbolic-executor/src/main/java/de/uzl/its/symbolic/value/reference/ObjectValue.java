package de.uzl.its.symbolic.value.reference;

import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.symbolic.value.reference.array.ObjectArrayValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

public class ObjectValue<T, K> extends Value<T, K> {

    public static final int ADDRESS_UNKNOWN = -1;
    public String className;
    protected int address; // address 0 is null, address -1 is uninitialized address

    // For array object, fields are the content of the array.
    // For plain object, fields are the instance variables.
    private Value<?, ?>[] fields;
    private IntValue nFields;

    public void setFields(Value<?, ?>[] fields) {
        this.fields = fields;
    }

    public Value<?, ?>[] getFields() {
        return fields;
    }

    private BooleanFormulaManager bmgr;

    // Initialize
    // TODO: why is the first arg there?
    public ObjectValue(SolverContext context, int unused, int v) {
        fields = null;
        address = v;
        this.context = context;
        this.bmgr = context.getFormulaManager().getBooleanFormulaManager();
    }

    public ObjectValue(SolverContext context, String className, IntValue nFields) {
        fields = new Value[nFields.concrete];
        this.context = context;
        this.nFields = nFields;
        address = -1;
        this.className = className;
        this.bmgr = context.getFormulaManager().getBooleanFormulaManager();
    }

    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        MAKE_SYMBOLIC();
        if (namePrefix != null) {
            name = namePrefix + "_" + name;
        }
        return name;
    }

    @Override
    public String MAKE_SYMBOLIC() {
        if (name == null) {
            name = "o" + symbol;
            symbol = symbol + inc;
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof ObjectValue) {
            ObjectValue<?, ?> other = (ObjectValue<?, ?>) o;
            return (address == other.address && fields.length == other.fields.length);
        } else {
            return false;
        }
    }

    public IntValue getnFields() {
        return nFields;
    }

    public int getAddress() {
        return address;
    }

    @Override
    public Object getConcrete() { // (TODO): confusing
        return address;
    } // TODO: WTF is that supposed to mean?!

    /** Private default constructor for serialization */
    private ObjectValue() {}

    /**
     * Compares if two references are identical
     *
     * @param o2 The other object
     * @return A BooleanValue representing the result
     */
    public BooleanFormula IF_ACMPEQ(ObjectValue o2) {
        BooleanFormula b = bmgr.makeBoolean(this == o2);
        return b;
    }

    /**
     * Compares if two references are not identical
     *
     * @param o2 The other object
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IF_ACMPNE(ObjectValue o2) {
        BooleanFormula b = bmgr.makeBoolean(this != o2);
        return b;
    }

    /**
     * Compares if this reference is the null value; NULL is represented by the address 0
     *
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IFNULL() {
        return bmgr.makeBoolean(this.address == 0);
    }

    /**
     * Compares if this reference is not the null value NULL is represented by the address 0
     *
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IFNONNULL() {
        return bmgr.makeBoolean(this.address != 0);
    }

    public Value<?, ?> getField(int fieldId) {
        if (address == 0) {
            throw new NullPointerException("User NullPointerException");
        }
        if (fields == null) {
            return PlaceHolder.instance;
        } else {
            Value<?, ?> v = fields[fieldId];
            if (v == null) {
                return PlaceHolder.instance;
            }
            return v;
        }
    }

    public void setField(int fieldId, Value<?, ?> value) {
        if (address == 0) {
            System.out.println("SEVERE: Trying to set a field of NULL");
        } else if (fields != null) {
            fields[fieldId] = value;
        }
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {
        if (this.name != null) {
            for (String prefix : PlaceHolder.valueOriginPrefixMap.values()) {
                if (!prefix.equals("") && this.name.startsWith(prefix)) {
                    return new PlaceHolder(true, PlaceHolder.prefixValueOriginMap.get(prefix));
                }
            }
            return PlaceHolder.symbolicInstance;
        }
        return PlaceHolder.instance;
    }

    public String toString() {
        return "ObjectValue{\"address:\""
                + Integer.toHexString(address)
                + "\",\"name\":\""
                + name
                + "\",\"class\":\""
                + className
                + "\"}";
    }

    @Override
    public ObjectValue<?, ?> asObjectValue() {
        return this;
    }

    @Override
    public FloatValue asFloatValue() {
        throw new RuntimeException("Cannot convert objectValue to FloatValue");
        // return new FloatValue(this.address); // TODO not verified yet
    }

    public AbstractArrayValue<?, ?, ?, ?, ?> asArrayValue() {
        throw new RuntimeException("Cannot convert ObjectValue to AbstractArrayValue");
    }

    public ObjectArrayValue asObjectArrayValue() {
        throw new RuntimeException("Cannot convert ObjectValue to ObjectArrayValue");
    }

    @Override
    public IntValue asIntValue() {

        System.out.println(
                "Fields-Length: "
                        + (fields == null ? "null" : fields.length)
                        + ", Address: "
                        + address);

        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .forEach(
                        frame ->
                                System.out.println(
                                        Thread.currentThread().getName()
                                                + " | "
                                                + frame.getDeclaringClass()
                                                + " | "
                                                + frame.getMethodName()
                                                + " | "
                                                + frame.getLineNumber()));

        Runtime.getRuntime().halt(1);
        return null;
    }
}
