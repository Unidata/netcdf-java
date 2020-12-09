package ucar.array;

/** A mix-in interface for evaluating if a value is missing. */
public interface IsMissingEvaluator extends ucar.ma2.IsMissingEvaluator {
  /** true if there may be missing data */
  boolean hasMissing();

  /** Test if val is a missing data value */
  boolean isMissing(double val);
}
