/**
 * This package contains the trace, its handler and its elements. The trace is used to record the
 * executed instructions and the corresponding symbolic constraints. The trace is used to generate
 * the dynamic execution tree in the symbolic explorer. Access to the trace is granted through the
 * {@link de.uzl.its.swat.symbolic.trace.SymbolicTraceHandler}. Accordingly, no other class should
 * access the trace directly.
 */
package de.uzl.its.swat.symbolic.trace;
