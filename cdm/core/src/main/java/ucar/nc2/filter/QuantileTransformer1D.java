
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl; // Import the concrete class
//import org.apache.commons.math3.analysis.interpolation.LinearInterpolator


public class QuantileTransformer1D {

  /** The actual number of quantiles used to discretize the cumulative
   distribution function.*/
  private int n_quantiles_;
  /** ndarray of shape (n_quantiles, n_features)
   The values corresponding the quantiles of reference.
   But in my case now it's only 1D, so shape (n_quantiles) */
  private double [] quantiles_;
  /**ndarray of shape (n_quantiles, )
   Quantiles of references.*/
  private double [] references_;
  /** to be used in constructor */
  private int n_quantiles;
  /** to chose wether normal or uniform */
  private String outputDistribution;
  // Default values
  /** Constructor with all parameters */
  public QuantileTransformer1D(int n_quantiles, String outputDistribution) {
    this.n_quantiles = n_quantiles;
    this.outputDistribution = outputDistribution;
    System.out.println("I am in the constructor");

  }

  /** Dense matrix fit ? */
  /** should compute percentiles for dense matrix (i.e. not sparse) */
  public void _dense_fit(double [] X) {
    // Initialize the references array with the same length as X
    double[] references = new double[references_.length];
    /** X --> The data used to scale along the features axis. */
    int n_samples = X.length;

    for (int i = 0; i < this.references_.length; i++) {
      references[i] = this.references_[i] * 100;
    }
    System.out.println("I am in the _dense_fit");


    this.quantiles_ = computePercentiles(X,references);
    /** make it monotonically increasing */
    ensureMonotonic(this.quantiles_);

  }
  /** Compute the quantiles used for transforming. */
   public void fit(double [] X){
     /** Fit method --> Compute the quantiles used for transforming. */
     /** X --> The data used to scale along the features axis.
      * shaoe (n_samples) */

     int n_samples = X.length;
     // Compute the number of quantiles to use
     this.n_quantiles_ = Math.max(1, Math.min(this.n_quantiles, n_samples));
     // Compute the references array
     this.references_ = linspace(0, 1, this.n_quantiles_);
     System.out.println("I am in the fit");

     _dense_fit(X);

   }
   public double [] _transform_col(double [] X_col, double [] quantiles) throws MathException {
     String output_distribution = this.outputDistribution;
     double BOUNDS_THRESHOLD = 1e-7;

     double lower_bound_x = quantiles[0];
     double upper_bound_x = quantiles[quantiles.length-1];
     int lower_bound_y = 0;
     int upper_bound_y = 1;

     // Create lower and upper bounds indices
     boolean[] lowerBoundsIdx = new boolean[X_col.length];
     boolean[] upperBoundsIdx = new boolean[X_col.length];
     if ("normal".equals(output_distribution)) {
       for (int i = 0; i < X_col.length; i++) {
         lowerBoundsIdx[i] = X_col[i] - BOUNDS_THRESHOLD < lower_bound_x;
         upperBoundsIdx[i] = X_col[i] + BOUNDS_THRESHOLD > upper_bound_x;
       }
     } else if ("uniform".equals(output_distribution)) {
       for (int i = 0; i < X_col.length; i++) {
         lowerBoundsIdx[i] = X_col[i] == lower_bound_x;
         upperBoundsIdx[i] = X_col[i] == upper_bound_x;
       }
     }

     // Assuming X_col, quantiles, references_ are already defined variables of type double[]

     double[] interpolated1 = interpolate(X_col, quantiles, this.references_);
     double[] interpolated2 = interpolate(
         Arrays.stream(X_col).map(x -> -x).toArray(),
         Arrays.stream(reverseArray(quantiles)).map(x -> -x).toArray(),
         Arrays.stream(reverseArray(this.references_)).map(x -> -x).toArray()
     );

     double[] result = new double[X_col.length];
     for (int i = 0; i < X_col.length; i++) {
       result[i] = 0.5 * (interpolated1[i] - interpolated2[i]);
     }

     /**      X_col[upper_bounds_idx] = upper_bound_y
              X_col[lower_bounds_idx] = lower_bound_y */

     for (int i = 0; i < X_col.length; i++) {
       if (upperBoundsIdx[i]) {
         X_col[i] = upper_bound_y;
       }
       if (lowerBoundsIdx[i]) {
         X_col[i] = lower_bound_y;
       }
     }
     System.out.println("I am in the _transform_col");
// Assuming X_col, BOUNDS_THRESHOLD are defined variables

     if ("normal".equals(outputDistribution)) {
       // Create a normal distribution object
       NormalDistribution normalDist = new NormalDistributionImpl();

       // Perform the inverse transform using the percent point function (ppf)
       for (int i = 0; i < X_col.length; i++) {
         X_col[i] = normalDist.inverseCumulativeProbability(X_col[i]);
       }

       // Find the values to clip the data to avoid mapping to infinity
       double clip_min = normalDist.inverseCumulativeProbability(BOUNDS_THRESHOLD - Double.MIN_VALUE);
       double clip_max = normalDist.inverseCumulativeProbability(1 - (BOUNDS_THRESHOLD - Double.MIN_VALUE));

       // Clip the data such that the inverse transform will be consistent
       for (int i = 0; i < X_col.length; i++) {
         X_col[i] = Math.max(clip_min, Math.min(clip_max, X_col[i]));
       }
     }
     return X_col;
// For uniform distribution, the ppf is the identity function, so no transformation is needed
// Else output distribution is uniform and we let X_col unchanged

   }

   public double [] _transform(double [] X) throws MathException {
     int numRows = X.length;
     double [] newX;

     newX=_transform_col(X, this.quantiles_);
     System.out.println("I am in the _transform");

     return newX;
   }


  public static double[] computePercentiles(double[] values, double[] percentiles) {
    Arrays.sort(values);
    double[] results = new double[percentiles.length];
    for (int i = 0; i < percentiles.length; i++) {
      double rank = percentiles[i] / 100 * (values.length - 1);
      int lowerIndex = (int) Math.floor(rank);
      int upperIndex = (int) Math.ceil(rank);
      if (lowerIndex == upperIndex) {
        results[i] = values[lowerIndex];
      } else {
        double weight = rank - lowerIndex;
        results[i] = (1.0 - weight) * values[lowerIndex] + weight * values[upperIndex];
      }
    }
    return results;
  }

  public static void ensureMonotonic(double[] array) {
    for (int i = 1; i < array.length; i++) {
      if (array[i] < array[i - 1]) {
        array[i] = array[i - 1];
      }
    }
  }
  public static double[] reverseArray(double[] array) {
    double[] reversed = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      reversed[i] = array[array.length - 1 - i];
    }
    return reversed;
  }

  public static double[] interpolate(double[] x, double[] xp, double[] fp) {
    double[] interpolated = new double[x.length];
    for (int i = 0; i < x.length; i++) {
      double xi = x[i];
      if (xi < xp[0]) {
        interpolated[i] = fp[0];
      } else if (xi > xp[xp.length - 1]) {
        interpolated[i] = fp[fp.length - 1];
      } else {
        int j = 0;
        while (xi > xp[j + 1]) {
          j++;
        }
        double x0 = xp[j];
        double x1 = xp[j + 1];
        double y0 = fp[j];
        double y1 = fp[j + 1];
        interpolated[i] = y0 + (xi - x0) * (y1 - y0) / (x1 - x0);
      }
    }
    return interpolated;
  }

  public static double[] linspace(double min, double max, int points) {
    double[] d = new double[points];
    for (int i = 0; i < points; i++){
      d[i] = min + i * (max - min) / (points - 1);
    }
    return d;
  }

//  public static double[] fit_transformer(double X)

  public static void main(String[] args) throws MathException {
    // Example usage
    double[] data = {1.8, 2,2.3, 2.4,2.5, 5, 10, 11,12,13.1,14.4,15,16};
    QuantileTransformer1D transformer = new QuantileTransformer1D(10, "uniform");
    Arrays.sort(data);
    transformer.fit(data);
    double[] transformedData = transformer._transform(data);
    System.out.println(Arrays.toString(transformedData));
  }
}
