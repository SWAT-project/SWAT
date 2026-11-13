package objects;

import org.sosy_lab.sv_benchmarks.ObjectFactory;
import org.sosy_lab.sv_benchmarks.Verifier;

public class BoxFactory implements ObjectFactory<Box> {
  @Override
  public Box createObject() {
    switch (Verifier.nondetInt()) {
      case 1:
        return new Box(Verifier.nondetInt());
      default:
        return null;
    }
  }
}
