package de.uzl.its.swat.common;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Optional phase profile of one executor run, for measuring where the CPU time of a round goes.
 * Off unless {@code -Dswat.profile=<file>} is set; then each {@link #mark} records the wall clock,
 * the CPU time of the calling thread and of the whole process (Linux /proc, ns), and {@link #dump}
 * appends the marks, the class-load/instrumentation counters and the CPU time per JVM thread to
 * the file. Disabled, every call is one static boolean check.
 */
public final class Profile {
    public static final String FILE = System.getProperty("swat.profile");
    public static final boolean ON = FILE != null;

    private static final List<String> marks = new ArrayList<>();
    private static final long T0 = System.nanoTime();

    // Class loads seen by the transformer chain (outermost transform calls only)
    private static int classLoads, transformedClasses;
    private static long transformNs, bytesIn, bytesOut;
    private static final ThreadLocal<long[]> current = new ThreadLocal<>();
    private static boolean dumped;

    private Profile() {}

    public static synchronized void mark(String label) {
        if (!ON) return;
        marks.add(String.format("%-22s wall_ms %9.3f thread_cpu_ms %9.3f proc_cpu_ms %9.3f proc_ticks_ms %7d",
                label, (System.nanoTime() - T0) / 1e6, threadCpuNs() / 1e6, processCpuNs() / 1e6, processTicksMs()));
    }

    /** First transformer of the chain: starts timing an outermost class load. */
    public static ClassFileTransformer begin() {
        return new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader l, String n, Class<?> r, ProtectionDomain d, byte[] b) {
                if (r == null && current.get() == null) {
                    current.set(new long[] {System.nanoTime(), 0, b.length, System.identityHashCode(b)});
                }
                return null;
            }
        };
    }

    /** Last transformer of the chain: ends timing of the class load begin() started. */
    public static ClassFileTransformer end() {
        return new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader l, String n, Class<?> r, ProtectionDomain d, byte[] b) {
                long[] s = current.get();
                if (r == null && s != null) {
                    current.remove();
                    synchronized (Profile.class) {
                        classLoads++;
                        transformNs += System.nanoTime() - s[0];
                        
                        if (System.identityHashCode(b) != (int) s[3] || b.length != s[2]) {
                            transformedClasses++;
                            bytesIn += s[2];
                            bytesOut += b.length;
                        }
                    }
                }
                return null;
            }
        };
    }

    public static synchronized void dump(String label) {
        if (!ON || dumped) return;
        mark(label);
        StringBuilder sb = new StringBuilder();
        sb.append("=== swat.profile pid ").append(ProcessHandle.current().pid()).append('\n');
        for (String m : marks) sb.append(m).append('\n');
        sb.append(String.format("transform: loads %d transformed %d bytes_in %d bytes_out %d wall_ms %.3f%n",
                classLoads, transformedClasses, bytesIn, bytesOut, transformNs / 1e6));
        Map<String, long[]> byName = new TreeMap<>();
        try (var tasks = Files.list(Path.of("/proc/self/task"))) {
            for (Path t : (Iterable<Path>) tasks::iterator) {
                try {
                    String comm = Files.readString(t.resolve("comm")).trim().replaceAll("[0-9]+$", "#");
                    long ns = Long.parseLong(Files.readString(t.resolve("schedstat")).split(" ")[0]);
                    long[] v = byName.computeIfAbsent(comm, k -> new long[2]);
                    v[0] += ns;
                    v[1]++;
                } catch (IOException | RuntimeException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        for (Map.Entry<String, long[]> e : byName.entrySet()) {
            sb.append(String.format("thread %-20s n %3d cpu_ms %9.3f%n", e.getKey(), e.getValue()[1], e.getValue()[0] / 1e6));
        }
        try (FileWriter w = new FileWriter(FILE, true)) {
            w.write(sb.toString());
        } catch (IOException ignored) {
        }
        dumped = true;
    }

    private static long threadCpuNs() {
        try {
            return Long.parseLong(Files.readString(Path.of("/proc/thread-self/schedstat")).split(" ")[0]);
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    /** CPU time of the live threads of the process (threads that ended are not included). */
    private static long processCpuNs() {
        long sum = 0;
        try (var tasks = Files.list(Path.of("/proc/self/task"))) {
            for (Path t : (Iterable<Path>) tasks::iterator) {
                try {
                    sum += Long.parseLong(Files.readString(t.resolve("schedstat")).split(" ")[0]);
                } catch (IOException | RuntimeException ignored) {
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return sum;
    }

    /** user+sys CPU of the whole process including ended threads, in clock ticks (10 ms). */
    private static long processTicksMs() {
        try {
            String s = Files.readString(Path.of("/proc/self/stat"));
            String[] f = s.substring(s.lastIndexOf(')') + 2).split(" ");
            return (Long.parseLong(f[11]) + Long.parseLong(f[12])) * 10;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}
