package de.uzl.its.swat.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public record TryCatchBlock(Label start, Label end, Label handler, Label jumpTarget, String type) {

    public void visit(MethodVisitor mv) {
        mv.visitTryCatchBlock(start, end, handler, type);
    }
}
