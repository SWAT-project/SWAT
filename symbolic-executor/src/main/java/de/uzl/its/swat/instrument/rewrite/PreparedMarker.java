package de.uzl.its.swat.instrument.rewrite;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * The mark of a prepared class: an invisible class annotation {@code @de.uzl.its.swat.Prepared(version,
 * config)} that {@link de.uzl.its.swat.prepare.Prepare} adds after the rewrites. The annotation type
 * does not exist; the JVM ignores invisible annotations. {@code version} is {@link
 * Rewrites#FORMAT_VERSION}, {@code config} is {@link Rewrites#configHash()}.
 */
public final class PreparedMarker {

    /** Descriptor of the marker annotation. Not a class constant, so the shadow jar cannot relocate it. */
    public static final String DESC = "Lde/uzl/its/".concat("swat/Prepared;");

    /** The marker of a class: its format version and config hash. */
    public record Marker(int version, String config) {
        /** Whether a class with this marker was prepared exactly as this agent would rewrite it. */
        public boolean matches(String configHash) {
            return version == Rewrites.FORMAT_VERSION && configHash.equals(config);
        }
    }

    private PreparedMarker() {}

    /** Adds the marker; methods and everything else are copied unchanged. */
    public static byte[] mark(byte[] cls, String configHash) {
        ClassReader cr = new ClassReader(cls);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visitEnd() {
                AnnotationVisitor av = super.visitAnnotation(DESC, false);
                av.visit("version", Rewrites.FORMAT_VERSION);
                av.visit("config", configHash);
                av.visitEnd();
                super.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }

    /** The marker of a class, or null if it has none. */
    public static Marker read(byte[] cls) {
        Marker[] found = new Marker[1];
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!DESC.equals(desc)) {
                    return null;
                }
                return new AnnotationVisitor(Opcodes.ASM9) {
                    int version = -1;
                    String config = "";

                    @Override
                    public void visit(String name, Object value) {
                        if ("version".equals(name) && value instanceof Integer v) {
                            version = v;
                        } else if ("config".equals(name) && value instanceof String s) {
                            config = s;
                        }
                    }

                    @Override
                    public void visitEnd() {
                        found[0] = new Marker(version, config);
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found[0];
    }
}
