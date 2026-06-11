/**
 * <p>
 * This package provides classes and templates for managing metadata about classes and their fields,
 * both at instrumentation time and at runtime.
 * </p>
 *
 * <p>
 * The central component is {@link de.uzl.its.swat.metadata.ClassDepot}, which acts as a registry to track each class and
 * its fields. It exposes two interface views:
 * </p>
 * <ul>
 *     <li>{@link de.uzl.its.swat.metadata.ClassDepotInstrumentation} - for assigning/creating indices during
 *     the instrumentation process.</li>
 *     <li>{@link de.uzl.its.swat.metadata.ClassDepotRuntime} - for looking up indices at runtime.</li>
 * </ul>
 * <p>
 * {@link de.uzl.its.swat.metadata.ClassTemplate} stores the per-class metadata backing both views.
 * </p>
 */
package de.uzl.its.swat.metadata;
