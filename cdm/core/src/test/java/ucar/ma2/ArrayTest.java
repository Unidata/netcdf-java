package ucar.ma2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiConsumer;

public class ArrayTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Array zeroRank;
  private IndexIterator iter;
  private int[] currentCounter;

  @Test
  public void testFactory() throws Exception {
    zeroRank = Array.factory(DataType.INT, new int[0]);
    iter = zeroRank.getIndexIterator();
    currentCounter = iter.getCurrentCounter();
    assert currentCounter.length == 0;
  }


  @Test
  public void testUnsigned2() {
    int nz = 1;
    int ny = 2030;
    int nx = 1354;
    int size = nz * ny * nx;

    short[] vals = new short[size];
    for (int i = 0; i < size; i++)
      vals[i] = (short) i;

    Array data = Array.factory(DataType.USHORT, new int[] {nz, ny, nx}, vals);
    double sum = MAMath.sumDouble(data);
    double sumReduce = MAMath.sumDouble(data.reduce(0));
    Assert2.assertNearlyEquals(sum, sumReduce);
  }

  // Demonstrates bug in https://github.com/Unidata/thredds/issues/581.
  @Test
  public void testConstantArray_get1DJavaArray() {
    Array array = Array.factoryConstant(DataType.INT, new int[] {3}, new int[] {47});

    // Prior to fix, the actual value returned by get1DJavaArray was {47}.
    Assert.assertArrayEquals(new int[] {47, 47, 47}, (int[]) array.get1DJavaArray(int.class));
  }

  @Test
  public void testConstantArray_createView() {
    // For all Array subtypes, assert that we can create a logical view of an Array that uses an IndexConstant.
    // This is a regression test for bug fixes I made to the various Array*.factory(Index, ...) methods.
    // Prior to the fixes, this test would raise exceptions such as:
    // java.lang.ClassCastException: ucar.ma2.IndexConstant cannot be cast to ucar.ma2.Index1D
    // at ucar.ma2.ArrayBoolean$D1.<init>(ArrayBoolean.java:235)
    // at ucar.ma2.ArrayBoolean$D1.<init>(ArrayBoolean.java:226)
    // at ucar.ma2.ArrayBoolean.factory(ArrayBoolean.java:60)
    // at ucar.ma2.ArrayBoolean.createView(ArrayBoolean.java:103)
    // at ucar.ma2.Array.reduce(Array.java:922)
    // at ucar.ma2.ArrayTest.testConstantArray_reduce(ArrayTest.java:58)
    for (DataType dataType : DataType.values()) {
      Array array = Array.factoryConstant(dataType, new int[] {1, 3, 1}, null);
      Assert.assertArrayEquals(new int[] {3}, array.reduce().getShape());
    }
  }

  // base test, asserts done in verification consumer
  private void testSerialization(Array array, BiConsumer<Array, Array> verification) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ObjectOutputStream objOut = new ObjectOutputStream(out)) {
      objOut.writeObject(array);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail("Unable to serialize array: " + e);
    }
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    try (ObjectInputStream objIn = new ObjectInputStream(in)) {
      Array restoredArray = (Array) objIn.readObject();
      verification.accept(array, restoredArray);
    } catch (IOException | ReflectiveOperationException e) {
      e.printStackTrace();
      Assert.fail("Unable to restore array: " + e);
    }
  }

  /**
   * Tests that serialization of constant length arrays works. Serialization requires that the array structure remains
   * the same; since there is no equals method implemented by {@link Array}, storage and shape must be equal.
   */
  @Test
  public void testSerialization_constantLength() {
    testSerialization(Array.makeFromJavaArray(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}),
        (originalArray, restoredArray) -> {
          // currently there is no implementation of equals(), so we compare the relevant properties
          Assert.assertNotSame(originalArray, restoredArray);
          Assert.assertArrayEquals((int[]) originalArray.getStorage(), (int[]) restoredArray.getStorage());
          Assert.assertArrayEquals(originalArray.getShape(), restoredArray.getShape());
          // no need to preserve index
        });

    testSerialization(Array.makeFromJavaArray(new String[] {"ucar", "ma2", "Array"}),
        (originalArray, restoredArray) -> {
          Assert.assertNotSame(originalArray, restoredArray);
          Assert.assertArrayEquals((Object[]) originalArray.getStorage(), (Object[]) restoredArray.getStorage());
          Assert.assertArrayEquals(originalArray.getShape(), restoredArray.getShape());
        });

    Random rng = new Random();
    Array doubleArray = Array.factory(DataType.DOUBLE, new int[] {2, 4, 6});
    for (IndexIterator iterator = doubleArray.getIndexIterator(); iterator.hasNext();) {
      iterator.setDoubleNext(rng.nextDouble());
    }
    testSerialization(doubleArray, (originalArray, restoredArray) -> {
      Assert.assertNotSame(originalArray, restoredArray);
      Assert.assertArrayEquals((double[]) originalArray.getStorage(), (double[]) restoredArray.getStorage(),
          Double.MIN_NORMAL);
      Assert.assertArrayEquals(originalArray.getShape(), restoredArray.getShape());
    });
  }

  /**
   * Tests that serialization of variable length arrays works. Serialization requires that the array structure remains
   * the same; since there is no equals method implemented by {@link Array}, storage and shape must be equal. Storage is
   * an array of {@link Array}, therefore each inner variable length array must be checked.
   */
  @Test
  public void testSerialization_variableLength() {
    // settings for the length of each vlen array
    int minLength = 10;
    int maxLength = 51; // exclusive
    int[] fixedShape = {4, 6};

    // this generates arrays of varying length with random elements
    Random rng = new Random();
    int fullSize = Arrays.stream(fixedShape).reduce((acc, i) -> acc * i).getAsInt();
    Array[] vlenStorage = new Array[fullSize];
    int lengthDelta = maxLength - minLength;
    for (int i = 0; i < fullSize; i++) {
      int vLength = rng.nextInt(lengthDelta) + minLength;
      Array array = Array.factory(DataType.INT, new int[] {vLength});
      for (IndexIterator iterator = array.getIndexIterator(); iterator.hasNext();) {
        iterator.setIntNext(rng.nextInt());
      }
      vlenStorage[i] = array;
    }

    testSerialization(Array.makeVlenArray(fixedShape, vlenStorage), (originalArray, restoredArray) -> {
      Assert.assertNotSame(originalArray, restoredArray);
      Assert.assertArrayEquals(originalArray.getShape(), restoredArray.getShape());
      // compare each array in the vlen storage
      for (IndexIterator originalIt = originalArray.getIndexIterator(), restoredIt =
          restoredArray.getIndexIterator(); originalIt.hasNext();) {
        Array originalItem = (Array) originalIt.next();
        Array restoredItem = (Array) restoredIt.next();
        Assert.assertNotSame(originalItem, restoredItem);
        Assert.assertArrayEquals((int[]) originalItem.getStorage(), (int[]) restoredItem.getStorage());
        Assert.assertArrayEquals(originalItem.getShape(), restoredItem.getShape());
      }
    });
  }
}
