package de.uzl.its.swat.instrument.instruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * Names the catch handlers of a class so that the static pre-analysis graph can find them.
 *
 * <p>When the walk over the graph has to give up after an exception the graph does not model, it
 * can resume at the handler the execution entered (see {@code HANDLER_ENTRY}). The graph is built
 * by SootUp, which keeps neither bytecode offsets nor the exception table's order, so a handler is
 * named by its content instead:
 *
 * <pre>owner.name(desc)|caught types|protected lines</pre>
 *
 * <p>The caught types are the handler's types in all its exception table entries, sorted, with
 * {@code java.lang.Throwable} for a catch-any entry; the protected lines are the source lines of
 * the instructions in the ranges it protects, sorted. The extractor builds the same key from
 * SootUp's view ({@code IntraProceduralCFGExtractor}) and leaves out handlers whose key is not
 * unique in their method (javac's copies of a {@code finally} block). Change both together.
 */
public final class HandlerKeys {

    private HandlerKeys() {
    }

    /**
     * For every method (by name and descriptor), the key of the handler of each of its try-catch
     * blocks, in the order {@code visitTryCatchBlock} reports them.
     */
    public static Map<String, List<String>> compute(byte[] classBytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, ClassReader.SKIP_FRAMES);
        Map<String, List<String>> keys = new HashMap<>();
        for (MethodNode mn : cn.methods) {
            if (mn.tryCatchBlocks == null || mn.tryCatchBlocks.isEmpty()) {
                continue;
            }
            List<AbstractInsnNode> insns = Arrays.asList(mn.instructions.toArray());
            int[] lineAt = new int[insns.size()];
            int line = -1;
            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof LineNumberNode ln) {
                    line = ln.line;
                }
                lineAt[i] = line;
            }
            Map<LabelNode, TreeSet<String>> types = new HashMap<>();
            Map<LabelNode, TreeSet<Integer>> lines = new HashMap<>();
            for (TryCatchBlockNode t : mn.tryCatchBlocks) {
                types.computeIfAbsent(t.handler, h -> new TreeSet<>())
                        .add(t.type == null ? "java.lang.Throwable" : t.type.replace('/', '.'));
                TreeSet<Integer> protectedLines = lines.computeIfAbsent(t.handler, h -> new TreeSet<>());
                for (int i = insns.indexOf(t.start); i < insns.indexOf(t.end); i++) {
                    if (insns.get(i).getOpcode() >= 0 && lineAt[i] > 0) {
                        protectedLines.add(lineAt[i]);
                    }
                }
            }
            List<String> perBlock = new ArrayList<>();
            for (TryCatchBlockNode t : mn.tryCatchBlocks) {
                perBlock.add(cn.name + "." + mn.name + mn.desc + "|" + String.join(",", types.get(t.handler))
                        + "|" + String.join(",", lines.get(t.handler).stream().map(String::valueOf).toList()));
            }
            keys.put(mn.name + mn.desc, perBlock);
        }
        return keys;
    }
}
