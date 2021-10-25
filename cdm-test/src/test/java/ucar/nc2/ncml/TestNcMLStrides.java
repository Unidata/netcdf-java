/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayInt;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test netcdf dataset in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestNcMLStrides extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestNcMLStrides(String name) {
    super(name);
  }

  NetcdfFile ncfile = null;
  String location = "file:" + TestDir.cdmUnitTestDir + "agg/strides/strides.ncml";

  public void setUp() {
    try {
      ncfile = NetcdfDatasets.openDataset(location, false, null);
      // System.out.println("ncfile opened = "+location);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = " + e);
    } catch (IOException e) {
      System.out.println("IO error = " + e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    ncfile.close();
  }

  public void testStride() throws IOException, InvalidRangeException {
    System.out.println("ncfile opened = " + location + "\n" + ncfile);
    Variable time = ncfile.findVariable("time");

    ArrayInt all = (ArrayInt) time.read();
    for (int i = 0; i < all.getSize(); i++)
      assert (all.getInt(i) == i + 1);

    testStride("0:13:3");

    for (int i = 1; i < 12; i++)
      testStride("0:13:" + i);
  }

  private void testStride(String stride) throws IOException, InvalidRangeException {
    Variable time = ncfile.findVariable("time");
    ArrayInt all = (ArrayInt) time.read();

    ArrayInt correct = (ArrayInt) all.section(new Section(stride).getRanges());
    logger.debug("correct({}) {}", stride, Ncdump.printArray(correct));
    ArrayInt data = (ArrayInt) time.read(stride);
    logger.debug("data({}) {}", stride, Ncdump.printArray(data));
    Index ci = correct.getIndex();
    Index di = data.getIndex();
    for (int i = 0; i < data.getSize(); i++)
      assert (data.getInt(di.set(i)) == correct.getInt(ci.set(i))) : stride + " index " + i + " = "
          + data.getInt(di.set(i)) + " != " + correct.getInt(ci.set(i));
  }

}
