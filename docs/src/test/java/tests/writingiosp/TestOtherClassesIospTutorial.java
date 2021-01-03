package tests.writingiosp;

import examples.writingiosp.OtherClassesIospTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public class TestOtherClassesIospTutorial {

  private final static String testFilePath =
      "src/site/files/netcdfJava_tutorial/writingiosp/lightningData.txt";

  private static RandomAccessFile raf;

  private static Variable varShort;
  private static Variable varDouble;

  @BeforeClass
  public static void setUpTest() throws IOException {
    raf = RandomAccessFile.acquire(testFilePath);
    Group.Builder rootGroup = Group.builder();
    Dimension d1 = Dimension.builder("i", 190).build();
    rootGroup.addDimension(d1);
    Dimension d2 = Dimension.builder("j", 5).build();
    rootGroup.addDimension(d2);
    Group parent = rootGroup.build();
    varShort = Variable.builder().setName("short_var").setDataType(DataType.SHORT).addDimension(d1)
        .addDimension(d2).build(parent);
    varDouble = Variable.builder().setName("double_var").setDataType(DataType.DOUBLE)
        .addDimension(d1).addDimension(d2).build(parent);
  }

  @Test
  public void testMakeArray() {
    OtherClassesIospTutorial.makeArray(varShort);
  }

  @Test
  public void testArrayIndexIterator() throws IOException {
    raf.seek(0);
    OtherClassesIospTutorial.arrayIndexIterator(raf, varShort);
  }

  @Test
  public void testArrayIndex() throws IOException {
    raf.seek(0);
    OtherClassesIospTutorial.arrayIndex(raf);
  }

  @Test
  public void testArrayRankAndType() throws IOException {
    raf.seek(0);
    OtherClassesIospTutorial.arrayRankAndType(raf);
  }
}
