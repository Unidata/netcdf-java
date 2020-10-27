/* Copyright Unidata */
package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Test opening file and its index. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestGribIndex {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String gridTestDir = "../grib/src/test/data/index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    // everything in this directory gives "Bad GRIB2 record"... "No records found in files", plus indexes are GRIB1
    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/ecmwf/bcs/BCS10090000100903001", false, false});
    return result;
  }

  String filename;
  boolean isGrib1;
  boolean fail;

  public TestGribIndex(String filename, boolean isGrib1, boolean fail) {
    this.filename = filename;
    this.isGrib1 = isGrib1;
    this.fail = fail;
  }

  // @Test
  public void testOpenGrib() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      assertThat(ncfile.getFileTypeId()).isEqualTo(isGrib1 ? "Grib1" : "Grib2");
    }
  }

  // @Test
  public void testReadIndexProto() {
    String indexFilename = filename + ".gbx9";
    if (isGrib1) {
      Grib1Index reader = new Grib1Index();
      boolean ok = reader.readIndex(indexFilename, -1, CollectionUpdateType.never);
      assertTrue(ok || fail);
    } else {
      Grib2Index reader = new Grib2Index();
      boolean ok = reader.readIndex(indexFilename, -1, CollectionUpdateType.never);
      assertTrue(ok || fail);
    }
  }
}

