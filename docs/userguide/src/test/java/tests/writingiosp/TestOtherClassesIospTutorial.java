package tests.writingiosp;

import examples.writingiosp.OtherClassesIospTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public class TestOtherClassesIospTutorial {
  private static RandomAccessFile raf;

  private static Variable varShort;
  private static Variable varDouble;

  @BeforeClass
  public static void setUpTest() throws IOException {
    raf = RandomAccessFile.acquire(TestIospExamplesShared.testFilePath);
    Group.Builder rootGroup = Group.builder();
    Dimension d1 = Dimension.builder("i", 190).build();
    rootGroup.addDimension(d1);
    Dimension d2 = Dimension.builder("j", 5).build();
    rootGroup.addDimension(d2);
    Group parent = rootGroup.build();
    varShort = Variable.builder().setName("short_var").setArrayType(ArrayType.SHORT)
        .addDimension(d1).addDimension(d2).build(parent);
    varDouble = Variable.builder().setName("double_var").setArrayType(ArrayType.DOUBLE)
        .addDimension(d1).addDimension(d2).build(parent);
  }

  @Test
  public void testMakeArray() {
    OtherClassesIospTutorial.makeArray(varShort);
  }

  @Test
  public void testArrayIterator() {
    Array<Float> data = Arrays.factory(ArrayType.FLOAT, new int[] {10});
    OtherClassesIospTutorial.arrayIterator(data);
  }

  @Test
  public void testArrayIndices() {
    Array<Float> data = Arrays.factory(ArrayType.FLOAT, new int[] {10});
    OtherClassesIospTutorial.arrayIndices(data);
  }
}
