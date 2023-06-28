/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;
import static ucar.gcdm.TestUtils.gcdmPrefix;

import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGcdmReadSection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  public static void before() {
    // make sure to fetch data through gcdm every time
    Variable.permitCaching = false;
  }

  @AfterClass
  public static void after() {
    Variable.permitCaching = true;
  }

  @Test
  public void testReadSection() throws Exception {
    String localFilename = "../../dap4/src/test/data/resources/nctestfiles/test_atomic_array.nc";
    String varName = "vs"; // Variable is non-numeric so values are not read when header is
    String[] expectedValues = {"hello\tworld", "\r\n", "Καλημέα", "abc"};
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      Array array = gcdmVar.read();
      assertThat(array.getSize()).isEqualTo(expectedValues.length);
      assertThat(array.getObject(0)).isEqualTo(expectedValues[0]);
      assertThat(array.getObject(1)).isEqualTo(expectedValues[1]);
      assertThat(array.getObject(2)).isEqualTo(expectedValues[2]);
      assertThat(array.getObject(3)).isEqualTo(expectedValues[3]);

      Array subset1 = gcdmVar.read("0:0, 0:1");
      assertThat(subset1.getSize()).isEqualTo(2);
      assertThat(subset1.getObject(0)).isEqualTo(expectedValues[0]);
      assertThat(subset1.getObject(1)).isEqualTo(expectedValues[1]);

      Array subset2 = gcdmVar.read("1:1, 0:1");
      assertThat(subset2.getSize()).isEqualTo(2);
      assertThat(subset2.getObject(0)).isEqualTo(expectedValues[2]);
      assertThat(subset2.getObject(1)).isEqualTo(expectedValues[3]);

      Array subset3 = gcdmVar.read("0:1, 1:1");
      assertThat(subset3.getSize()).isEqualTo(2);
      assertThat(subset3.getObject(0)).isEqualTo(expectedValues[1]);
      assertThat(subset3.getObject(1)).isEqualTo(expectedValues[3]);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testReadSectionOfArrayStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    String varName = "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001";
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      assertThat(gcdmVar.read().getSize()).isEqualTo(720);
      assertThat(gcdmVar.read("0:130").getSize()).isEqualTo(131);
      assertThat(gcdmVar.read("718:719").getSize()).isEqualTo(2);
    }
  }
}
