/**
 * <p>
 * This package provides classes and templates for managing metadata about classes and their fields,
 * both at instrumentation time and at runtime.
 * </p>
 *
 * <p>
 * The central component is {@link de.uzl.its.swat.metadata.ClassDepot}, which acts as a registry to track each class and
 * its fields. It uses two different template types:
 * <ul>
 *     <li>{@link de.uzl.its.swat.metadata.InstrumentationClassTemplate} - for assigning/creating field indices during
 *     the instrumentation process.</li>
 *     <li>{@link de.uzl.its.swat.metadata.RuntimeClassTemplate} - for looking up field indices at runtime.</li>
 * </ul>
 * <strong>{@link de.uzl.its.swat.metadata.ClassTemplate}</strong> is an abstract base for these template
 * implementations.
 * </p>
 */
package de.uzl.its.swat.metadata;
