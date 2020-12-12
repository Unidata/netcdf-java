package ucar.array;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MinMax {
  public abstract double min();

  public abstract double max();

  public static MinMax create(double min, double max) {
    return new AutoValue_MinMax(min, max);
  }

  @Override
  public String toString() {
    return "MinMax{" + "min=" + min() + ", max=" + max() + '}';
  }
}
