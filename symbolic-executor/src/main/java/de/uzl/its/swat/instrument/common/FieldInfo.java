package de.uzl.its.swat.instrument.common;

import de.uzl.its.swat.instrument.Utils;
import org.objectweb.asm.Type;

/**
 * A record representing metadata for a field annotated with {@code @Symbolic}.
 *
 * @param declaringClass the class name in which the field is declared
 * @param name   the field name
 * @param desc   the field descriptor (type information)
 * @param type   the field type
 * @param access the field access flags
 */
public record FieldInfo(String declaringClass, String name, String desc, int access) {

    public boolean isStatic() {
        return Utils.isStatic(access);
    }
    public Type getType() {
        return Type.getType(desc);
    }
}
