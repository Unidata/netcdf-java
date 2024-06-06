package ucar.nc2.filter;

import static org.junit.Assert.*;
import org.junit.Test;
import ucar.ma2.Array;


public class TestClassifier {



  /** test doubles */
  @Test
  public void testClassifyDoubleArray_AllPositive() {
    Classifier classifier = new Classifier();
    double[] input = {1.1, 2.2, 3.3};
    int[] expected = {1, 1, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyDoubleArray(DATA));
  }

  @Test
  public void testClassifyDoubleArray_AllNegative() {
    Classifier classifier = new Classifier();
    double[] input = {-1.1, -2.2, -3.3};
    int[] expected = {0, 0, 0};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyDoubleArray(DATA));
  }

  @Test
  public void testClassifyDoubleArray_Mixed() {
    Classifier classifier = new Classifier();
    double[] input = {-1.1, 2.2, -3.3, 4.4};
    int[] expected = {0, 1, 0, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyDoubleArray(DATA));
  }

  @Test
  public void testClassifyDoubleArray_WithZero() {
    Classifier classifier = new Classifier();
    double[] input = {0.0, -1.1, 1.1, 0.0, 0.0, 0.0};
    int[] expected = {1, 0, 1, 1, 1, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyDoubleArray(DATA));
  }



}
