package ucar.ma2;

/**
 * A mix-in interface for evaluating if a value is missing.
 *
 * @author John
 * @since 12/27/12
 */
public interface IsMissingEvaluator {

  /**
   * true if there may be missing data
   * 
   * @return true if there may be missing data
   */
  boolean hasMissing();

  /**
   * if val is a missing data value
   * 
   * @param val test this value
   * @return true if val is missing data
   */
  boolean isMissing(double val);

}
