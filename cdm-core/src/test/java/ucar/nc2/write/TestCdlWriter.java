/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CDLWriter} */
public class TestCdlWriter {
  // see cdm-core/src/test/data/cdl/test_atomic_types.cdl
  @Test
  public void testAtomicTypes() throws IOException {
    try (
        NetcdfFile ncfile = NetcdfDatasets.openFile(TestDir.cdmLocalTestDataDir + "/hdf5/test_atomic_types.nc", null)) {
      Formatter out = new Formatter();
      CDLWriter.writeCDL(ncfile, out, true, null);
      String cdl = out.toString();

      assertThat(cdl).contains("byte v8;");
      assertThat(cdl).contains("ubyte vu8;");
      assertThat(cdl).contains("short v16;");
      assertThat(cdl).contains("ushort vu16;");
      assertThat(cdl).contains("int v32;");
      assertThat(cdl).contains("uint vu32;");
      assertThat(cdl).contains("int64 v64;");
      assertThat(cdl).contains("uint64 vu64;");
      assertThat(cdl).contains("float vf;");
      assertThat(cdl).contains("double vd;");
      assertThat(cdl).contains("char vc;");
      assertThat(cdl).contains("string vs;");
    }
  }


}
