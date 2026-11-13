package org.sosy_lab.fixtures;

import objects.Box;
import org.sosy_lab.sv_benchmarks.ObjectFactory;
import org.sosy_lab.sv_benchmarks.Verifier;

/** A factory in an excluded package: it is not instrumented, so its nondet choices are concrete. */
public class PlainBoxFactory implements ObjectFactory<Box> {
  @Override
  public Box createObject() {
    return Verifier.nondetInt() == 1 ? new Box(7) : null;
  }
}
