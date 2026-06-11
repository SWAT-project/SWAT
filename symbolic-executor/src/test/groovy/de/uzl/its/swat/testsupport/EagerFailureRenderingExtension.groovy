package de.uzl.its.swat.testsupport

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo

/**
 * Forces Spock failure messages to be rendered while the failing test's
 * resources are still alive.
 *
 * Spock renders condition failures lazily: the values captured in a failed
 * condition (e.g. an IntValue) are only stringified when the test framework
 * asks for the exception message, which Gradle does AFTER the spec's
 * cleanup() has run. Our value specs close their Z3 SolverContext in
 * cleanup(), and calling formula.toString() on a closed context makes the
 * native Z3 library abort the whole test JVM (assertion violation in
 * api_ast.cpp), discarding all test reports.
 *
 * This extension intercepts every feature method and, when it throws,
 * touches getMessage() on the entire throwable chain before the error
 * propagates. Spock caches the rendering on first access, so the later,
 * post-cleanup message lookups reuse the already-rendered string instead of
 * calling back into the solver.
 */
class EagerFailureRenderingExtension implements IGlobalExtension {

    private static final IMethodInterceptor EAGER_RENDER = { IMethodInvocation invocation ->
        try {
            invocation.proceed()
        } catch (Throwable t) {
            forceRender(t)
            throw t
        }
    } as IMethodInterceptor

    private static void forceRender(Throwable t) {
        Set<Throwable> seen = ([] as Set).asSynchronized()
        List<Throwable> queue = [t]
        while (!queue.isEmpty()) {
            Throwable cur = queue.removeLast()
            if (cur == null || !seen.add(cur)) {
                continue
            }
            try {
                cur.getMessage()
            } catch (Throwable ignored) {
                // Rendering must never mask the original failure
            }
            queue.add(cur.getCause())
            queue.addAll(cur.getSuppressed())
        }
    }

    @Override
    void visitSpec(SpecInfo spec) {
        spec.allFeatures*.featureMethod*.addInterceptor(EAGER_RENDER)
    }
}
