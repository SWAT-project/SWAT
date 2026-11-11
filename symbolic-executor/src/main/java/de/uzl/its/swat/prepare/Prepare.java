package de.uzl.its.swat.prepare;

import de.uzl.its.swat.instrument.rewrite.PreparedMarker;
import de.uzl.its.swat.instrument.rewrite.Rewrites;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Prepares a classpath: applies SWAT's semantic rewrites ({@link Rewrites}) ahead of time, so that
 * the static analysis and the executor see the same code.
 *
 * <pre>java -Dconfig.path=&lt;swat.cfg&gt; -cp symbolic-executor.jar de.uzl.its.swat.prepare.Prepare [--no-mark] &lt;out-dir&gt; &lt;entry&gt;...</pre>
 *
 * The configuration is read as by the agent (it decides which rewrites run and on which classes).
 * Every directory entry {@code i} of the classpath is copied to {@code <out-dir>/<i>}, with each
 * class the rewrites change replaced by its rewritten, {@link PreparedMarker marked} version. Jars
 * and missing entries are kept as they are: their classes are rewritten at load time, like classes
 * generated at run time. The prepared classpath is the original one with each directory replaced
 * by its copy, so every name still resolves to the same entry.
 *
 * <p>Output (stdout and {@code <out-dir>/prepared.json}): one JSON object with {@code classpath}
 * (the prepared classpath), {@code config}, {@code format}, {@code prepared}, {@code copied},
 * {@code failed} (classes the rewrites failed on; copied unchanged, so the agent tries again at
 * load time) and {@code duration_s}. Exit status 0 unless the tool itself fails. {@code --no-mark}
 * leaves the marker out, for comparing the output with the agent's ({@code scripts/check-prepare.py}).
 */
public final class Prepare {

    private Prepare() {}

    public static void main(String[] args) throws Exception {
        boolean mark = true;
        if (args.length > 0 && args[0].equals("--no-mark")) {
            mark = false;
            args = java.util.Arrays.copyOfRange(args, 1, args.length);
        }
        if (args.length < 2) {
            System.err.println("usage: Prepare [--no-mark] <out-dir> <classpath entry>...");
            System.exit(2);
        }
        // A rewrite error must fail the class, not halt the tool (ErrorHandler rethrows then)
        if (System.getProperty("exitOnError") == null) {
            System.setProperty("exitOnError", "false");
        }
        long start = System.nanoTime();
        Path out = Path.of(args[0]).toAbsolutePath();
        List<Path> entries = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            entries.add(Path.of(args[i]).toAbsolutePath());
        }
        Result r = prepare(out, entries, mark);
        r.durationS = (System.nanoTime() - start) / 1e9;
        String json = r.toJson();
        Files.writeString(out.resolve("prepared.json"), json + "\n");
        System.out.println(json);
        System.out.flush();
        // The transformers' classes register a shutdown hook (the agent's); end without waiting
        Runtime.getRuntime().halt(0);
    }

    /** What {@link #prepare} did. */
    public static final class Result {
        public final List<String> classpath = new ArrayList<>();
        public int prepared;
        public int copied;
        public final List<String> failed = new ArrayList<>();
        public double durationS;
        public final String config = Rewrites.configHash();

        String toJson() {
            StringBuilder b = new StringBuilder("{\"format\": ").append(Rewrites.FORMAT_VERSION)
                    .append(", \"config\": ").append(quote(config))
                    .append(", \"classpath\": [");
            for (int i = 0; i < classpath.size(); i++) {
                b.append(i > 0 ? ", " : "").append(quote(classpath.get(i)));
            }
            b.append("], \"prepared\": ").append(prepared).append(", \"copied\": ").append(copied)
                    .append(", \"failed\": [");
            for (int i = 0; i < failed.size(); i++) {
                b.append(i > 0 ? ", " : "").append(quote(failed.get(i)));
            }
            return b.append("], \"duration_s\": ").append(String.format(java.util.Locale.ROOT, "%.3f", durationS))
                    .append("}").toString();
        }
    }

    /**
     * Prepares the classpath {@code entries} into {@code out} (see the class comment). Classes the
     * rewrites change are written marked; everything else is copied unchanged.
     */
    public static Result prepare(Path out, List<Path> entries, boolean mark) throws IOException {
        Result r = new Result();
        Files.createDirectories(out);
        List<URL> urls = new ArrayList<>();
        for (Path e : entries) {
            urls.add(e.toUri().toURL());
        }
        // For the common supertypes in frame computation: the task's classes plus, as the parent,
        // SWAT's own and the JDK's, as the agent sees them through the application class loader
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), Prepare.class.getClassLoader())) {
            List<ClassFileTransformer> steps = Rewrites.steps();
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < entries.size(); i++) {
                Path entry = entries.get(i);
                if (!Files.isDirectory(entry)) {
                    r.classpath.add(entry.toString());
                    continue;
                }
                Path target = out.resolve(Integer.toString(i));
                prepareDirectory(entry, target, steps, loader, seen, mark, r);
                r.classpath.add(target.toString());
            }
        }
        return r;
    }

    private static void prepareDirectory(Path entry, Path target, List<ClassFileTransformer> steps,
                                         ClassLoader loader, Set<String> seen, boolean mark, Result r) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(entry)) {
            // Sorted, so that the output does not depend on the file system's order
            files = walk.filter(Files::isRegularFile).sorted().toList();
        }
        for (Path f : files) {
            Path rel = entry.relativize(f);
            Path dest = target.resolve(rel.toString());
            Files.createDirectories(dest.getParent());
            String name = rel.toString().replace('\\', '/');
            if (!name.endsWith(".class") || name.endsWith("module-info.class") || name.startsWith("META-INF/")) {
                Files.copy(f, dest);
                continue;
            }
            String cname = name.substring(0, name.length() - ".class".length());
            byte[] in = Files.readAllBytes(f);
            if (!cname.equals(className(in))) {
                // Not loadable from this entry (e.g. a package directory that is also on the
                // classpath itself): the JVM never defines it under this name
                Files.write(dest, in);
                r.copied++;
                continue;
            }
            if (!seen.add(cname)) {
                // Shadowed by an earlier entry: never loaded
                Files.write(dest, in);
                r.copied++;
                continue;
            }
            byte[] rewritten;
            try {
                rewritten = Rewrites.apply(steps, loader, cname, in);
            } catch (Throwable t) {
                r.failed.add(cname + ": " + t);
                Files.write(dest, in);
                r.copied++;
                continue;
            }
            if (rewritten == in) {
                Files.write(dest, in);
                r.copied++;
            } else {
                Files.write(dest, mark ? PreparedMarker.mark(rewritten, r.config) : rewritten);
                r.prepared++;
            }
        }
    }

    /** The class's own name, or null if it is not a readable class file. */
    private static String className(byte[] cls) {
        try {
            return new org.objectweb.asm.ClassReader(cls).getClassName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }
}
