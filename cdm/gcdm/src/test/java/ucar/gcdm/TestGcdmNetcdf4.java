/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmNetcdf4 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String gcdmPrefix = "gcdm://localhost:16111/";

  private static final Predicate<Object[]> filesToSkip = new Predicate<Object[]>() {
    @Override
    public boolean test(Object[] filenameParam) {
      // these files are removed because they cause an OutOfMemoryError
      // todo: why do these cause an OutOfMemoryError?
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
    this.gcdmUrl = gcdmPrefix + filename;
  }

  @Test
  public void doOne() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Formatter formatter = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, gcdmFile, formatter, true, false, true);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }
}
