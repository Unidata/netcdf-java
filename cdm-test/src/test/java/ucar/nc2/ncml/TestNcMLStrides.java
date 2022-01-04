/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test netcdf dataset in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestNcMLStrides extends TestCase {

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

  public void testStride() throws Exception {
    System.out.println("ncfile opened = " + location + "\n" + ncfile);
    Variable time = ncfile.findVariable("time");

    Array<Integer> all = (Array<Integer>) time.readArray();
    for (int i = 0; i < all.getSize(); i++) {
      assertThat(all.get(i)).isEqualTo(i + 1);
    }

    testStride("0:13:3");

    for (int i = 1; i < 12; i++)
      testStride("0:13:" + i);
  }

  private void testStride(String stride) throws IOException, InvalidRangeException {
    Variable time = ncfile.findVariable("time");
    Array<Integer> all = (Array<Integer>) time.readArray();

    Array<Integer> correct = Arrays.section(all, new Section(stride));
    Array<Integer> data = (Array<Integer>) time.readArray(new Section(stride));
    Index ci = correct.getIndex();
    Index di = data.getIndex();
    for (int i = 0; i < data.getSize(); i++) {
      assertWithMessage(stride + " index " + i + " = " + data.get(di.set(i)) + " != " + correct.get(ci.set(i)))
              .that(data.get(di.set(i)))
              .isEqualTo(ci.set(i));
    }
  }
}
