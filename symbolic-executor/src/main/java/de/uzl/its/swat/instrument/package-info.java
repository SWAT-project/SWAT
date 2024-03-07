/**
 * This package contains all classes required for the instrumentation of the Java bytecode.
 * The instrumentation relies on <a href="https://asm.ow2.io">ASM</a> for bytecode manipulation.
 * Instrumentation logic is split into three core components:
 *  - The {@link de.uzl.its.swat.instrument.instruction.InstructionTransformer} is responsible for adding instrumentation that tracks each executed instruction that is in scope.
 *  - The {@link de.uzl.its.swat.instrument.symbolicwrapper.SymbolicWrapperTransformer} provides wrapping for the symbolic scope and is responsible for adding calls that initiate and terminate the symbolic tracking.
 * The last group of Transformers is responsible for adding handling that marks the correct values as symbolic. Currently, two types are supported:
 *  - The {@link de.uzl.its.swat.instrument.svcomp.SVCompTransformer} is responsible for adding instrumentation that is compatible with the <a href="https://sv-comp.sosy-lab.org/2024/">SV-Comp</a> format.
 *  - The {@link de.uzl.its.swat.instrument.parameter.ParameterTransformer} makes all method parameters symbolic that match the pattern specified in the configuration.
 */
package de.uzl.its.swat.instrument;
