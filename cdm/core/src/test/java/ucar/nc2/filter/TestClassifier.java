package ucar.nc2.filter;

import static org.junit.Assert.*;
import org.junit.Test;
import ucar.ma2.Array;


public class TestClassifier {
  @Test
  public void testClassifyIntArray_AllPositive() {
    Classifier classifier = new Classifier();
    int[] input = {1, 2, 3};
    int[] expected = {1, 1, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyIntArray(DATA));
  }

  @Test
  public void testClassifyIntArray_AllNegative() {
    Classifier classifier = new Classifier();
    int[] input = {-1, -2, -3};
    int[] expected = {0, 0, 0};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyIntArray(DATA));
  }

  @Test
  public void testClassifyIntArray_Mixed() {
    Classifier classifier = new Classifier();
    int[] input = {-1, 2, -3, 4};
    int[] expected = {0, 1, 0, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyIntArray(DATA));
  }

  @Test
  public void testClassifyIntArray_WithZero() {
    Classifier classifier = new Classifier();
    int[] input = {0, -1, 1, 0, 0, 0};
    int[] expected = {1, 0, 1, 1, 1, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyIntArray(DATA));
  }

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



  /** testing with floats */
  @Test
  public void testClassifyFloatArray_AllPositive() {
    Classifier classifier = new Classifier();
    float[] input = {1.1f, 2.2f, 3.3f};
    int[] expected = {1, 1, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyFloatArray(DATA));
  }

  @Test
  public void testClassifyFloatArray_AllNegative() {
    Classifier classifier = new Classifier();
    float[] input = {-1.1f, -2.2f, -3.3f};
    int[] expected = {0, 0, 0};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyFloatArray(DATA));
  }

  @Test
  public void testClassifyFloatArray_Mixed() {
    Classifier classifier = new Classifier();
    float[] input = {-1.1f, 2.2f, -3.3f, 4.4f};
    int[] expected = {0, 1, 0, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyFloatArray(DATA));
  }

  @Test
  public void testClassifyFloatArray_WithZero() {
    Classifier classifier = new Classifier();
    float[] input = {0.0f, -1.1f, 1.1f};
    int[] expected = {1, 0, 1};
    Array DATA = Array.makeFromJavaArray(input);
    assertArrayEquals(expected, classifier.classifyFloatArray(DATA));
  }



}
