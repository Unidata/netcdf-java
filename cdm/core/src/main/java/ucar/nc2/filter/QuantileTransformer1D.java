import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;

public class QuantileTransformer1D {
  public static double[] transform(double[] inputArray) {



    // Step 1: Sorting
    double[] sortedArray = Arrays.copyOf(inputArray, inputArray.length);
    Arrays.sort(sortedArray);

    // Step 2: Assigning ranks
    Map<Double, Integer> rankMap = new HashMap<>();
    for (int i = 0; i < sortedArray.length; i++) {
      rankMap.put(sortedArray[i], i + 1);
    }

    // Create a TreeMap and pass the HashMap to its constructor
    TreeMap<Double, Integer> sortedMap = new TreeMap<>(rankMap);

    // Print out the sorted contents of the HashMap
    System.out.println("Contents of rankMap sorted by keys:");
    sortedMap.forEach((key, value) -> System.out.println(key + ": " + value));

    // Step 3: Calculating percentiles
    double[] percentiles = new double[inputArray.length];
    for (int i = 0; i < inputArray.length; i++) {
      double rank = rankMap.get(inputArray[i]);
      double percentile = (rank - 1) / (inputArray.length - 1);
      System.out.println("rank "+rank+" percentile, " + percentile);
      percentiles[i] = percentile;
    }

    // Step 5: Applying transformation
    double[] transformedArray = new double[inputArray.length];
    for (int i = 0; i < inputArray.length; i++) {
      transformedArray[i] = percentiles[i];
    }

    return transformedArray;
  }

  public static void main(String[] args) {
    // Example usage
    double[] data = {1.8, 2,2.3, 2.4,2.5, 5, 10, 11,12,13.1,14.4,15,16};
    Arrays.sort(data);
    double[] transformedData = QuantileTransformer1D.transform(data);
    System.out.println(Arrays.toString(transformedData));
  }
}
