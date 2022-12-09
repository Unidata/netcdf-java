/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmNetcdf4 {

  private static final Predicate<Object[]> filesToSkip = new Predicate<Object[]>() {
    @Override
    public boolean test(Object[] filenameParam) {
      // these files are removed because they cause an OutOfMemeoryError
      // todo: why do these cause an OutOfMemeoryError?
      String fname = (String) filenameParam[0];
      return !(fname.endsWith("/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4")
          || fname.endsWith("/tiling.nc4"));
    }
  };

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      FileFilter ff = new SuffixFileFilter(".nc4");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4", ff, result, false);
      result = result.stream().filter(filesToSkip).collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmNetcdf4(String filename) {
    this.filename = filename;
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + filename;
  }

  @Test
  public void doOne() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToArray.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }
}
