package de.uzl.its.swat.prepare;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads (without initializing) every class in the given classpath directories through the
 * application class loader, so that the agent's transformers see each of them. Used with {@code
 * -Dswat.dumpRewritten=<dir>} to check that {@link Prepare} writes what the agent's load-time
 * rewrites produce ({@code scripts/check-prepare.py}).
 */
public final class LoadClasses {

    private LoadClasses() {}

    public static void main(String[] args) throws Exception {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        int loaded = 0;
        int failed = 0;
        for (String a : args) {
            Path dir = Path.of(a);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files;
            try (Stream<Path> walk = Files.walk(dir)) {
                files = walk.filter(p -> p.toString().endsWith(".class")).sorted().toList();
            }
            for (Path f : files) {
                String name = dir.relativize(f).toString().replace('\\', '/');
                if (name.endsWith("module-info.class") || name.startsWith("META-INF/")) continue;
                name = name.substring(0, name.length() - ".class".length());
                try {
                    if (!name.equals(new org.objectweb.asm.ClassReader(Files.readAllBytes(f)).getClassName())) {
                        continue; // not loadable under this name from this entry
                    }
                } catch (RuntimeException e) {
                    continue;
                }
                name = name.replace('/', '.');
                try {
                    Class.forName(name, false, loader);
                    loaded++;
                } catch (Throwable t) {
                    failed++;
                    System.err.println("[LoadClasses] " + name + ": " + t);
                }
            }
        }
        System.out.println("[LoadClasses] loaded " + loaded + ", failed " + failed);
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
