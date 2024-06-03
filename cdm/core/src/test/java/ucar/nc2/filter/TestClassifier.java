package ucar.nc2.filter;

import static org.junit.Assert.*;
import org.junit.Test;

import ucar.nc2.filter.Classifier.InvalidValueException;

public class TestClassifier {
  @Test
  public void testClassifyIntArray_AllPositive() {
    Classifier classifier = new Classifier();
    int[] input = {1, 2, 3};
    int[] expected = {1, 1, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyIntArray_AllNegative() {
    Classifier classifier = new Classifier();
    int[] input = {-1, -2, -3};
    int[] expected = {0, 0, 0};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyIntArray_Mixed() {
    Classifier classifier = new Classifier();
    int[] input = {-1, 2, -3, 4};
    int[] expected = {0, 1, 0, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyIntArray_WithZero() {
    Classifier classifier = new Classifier();
    int[] input = {0, -1, 1, 0, 0, 0};
    int[] expected = {1, 0, 1, 1, 1, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  /** test doubles */
  @Test
  public void testClassifyDoubleArray_AllPositive() throws InvalidValueException {
    Classifier classifier = new Classifier();
    double[] input = {1.1, 2.2, 3.3};
    int[] expected = {1, 1, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyDoubleArray_AllNegative() throws InvalidValueException {
    Classifier classifier = new Classifier();
    double[] input = {-1.1, -2.2, -3.3};
    int[] expected = {0, 0, 0};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyDoubleArray_Mixed() throws InvalidValueException {
    Classifier classifier = new Classifier();
    double[] input = {-1.1, 2.2, -3.3, 4.4};
    int[] expected = {0, 1, 0, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyDoubleArray_WithZero() throws InvalidValueException {
    Classifier classifier = new Classifier();
    double[] input = {0.0, -1.1, 1.1, 0.0, 0.0, 0.0};
    int[] expected = {1, 0, 1, 1, 1, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test(expected = InvalidValueException.class)
  public void testClassifyDoubleArray_WithNaN() throws InvalidValueException {
    Classifier classifier = new Classifier();
    double[] input = {Double.NaN, 1.1, -1.1};
    classifier.classifyArray(input);
  }

  /** testing with floats */
  @Test
  public void testClassifyFloatArray_AllPositive() throws InvalidValueException {
    Classifier classifier = new Classifier();
    float[] input = {1.1f, 2.2f, 3.3f};
    int[] expected = {1, 1, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyFloatArray_AllNegative() throws InvalidValueException {
    Classifier classifier = new Classifier();
    float[] input = {-1.1f, -2.2f, -3.3f};
    int[] expected = {0, 0, 0};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyFloatArray_Mixed() throws InvalidValueException {
    Classifier classifier = new Classifier();
    float[] input = {-1.1f, 2.2f, -3.3f, 4.4f};
    int[] expected = {0, 1, 0, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test
  public void testClassifyFloatArray_WithZero() throws InvalidValueException {
    Classifier classifier = new Classifier();
    float[] input = {0.0f, -1.1f, 1.1f};
    int[] expected = {1, 0, 1};
    assertArrayEquals(expected, classifier.classifyArray(input));
  }

  @Test(expected = InvalidValueException.class)
  public void testClassifyFloatArray_WithNaN() throws InvalidValueException {
    Classifier classifier = new Classifier();
    float[] input = {Float.NaN, 1.1f, -1.1f};
    classifier.classifyArray(input);
  }


}
