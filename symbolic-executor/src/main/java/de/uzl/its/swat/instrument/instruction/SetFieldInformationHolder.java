package de.uzl.its.swat.instrument.instruction;

import java.lang.reflect.Field;

public class SetFieldInformationHolder {
    public static class BaseSetFieldInformationHolder {
        public BaseSetFieldInformationHolder(Field f, Object target) {
            this.f = f;
            this.target = target;
        }

        public Field f;
        public Object target;
    }

    public static class ObjectSetFieldInformationHolder extends BaseSetFieldInformationHolder{
        public ObjectSetFieldInformationHolder(Field f, Object target, Object member) {
            super(f, target);
            this.member = member;
        }

        public Object member;
    }

    public static class BooleanSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public BooleanSetFieldInformationHolder(Field f, Object target, boolean member) {
            super(f, target);
            this.member = member;
        }

        public boolean member;
    }

    public static class ByteSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public ByteSetFieldInformationHolder(Field f, Object target, byte member) {
            super(f, target);
            this.member = member;
        }

        public byte member;
    }

    public static class CharSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public CharSetFieldInformationHolder(Field f, Object target, char member) {
            super(f, target);
            this.member = member;
        }

        public char member;
    }

    public static class DoubleSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public DoubleSetFieldInformationHolder(Field f, Object target, double member) {
            super(f, target);
            this.member = member;
        }

        public double member;
    }

    public static class FloatSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public FloatSetFieldInformationHolder(Field f, Object target, float member) {
            super(f, target);
            this.member = member;
        }

        public float member;
    }

    public static class IntSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public IntSetFieldInformationHolder(Field f, Object target, int member) {
            super(f, target);
            this.member = member;
        }

        public int member;
    }

    public static class LongSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public LongSetFieldInformationHolder(Field f, Object target, long member) {
            super(f, target);
            this.member = member;
        }

        public long member;
    }

    public static class ShortSetFieldInformationHolder extends BaseSetFieldInformationHolder {
        public ShortSetFieldInformationHolder(Field f, Object target, long member) {
            super(f, target);
            this.member = member;
        }

        public long member;
    }
}
