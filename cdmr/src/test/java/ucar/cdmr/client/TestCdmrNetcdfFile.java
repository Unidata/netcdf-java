/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Formatter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.Array;
import ucar.array.StructureData;
import ucar.ma2.DataType;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NotJenkins;

/** Test {@link CdmrNetcdfFile} */
@RunWith(Parameterized.class)
@Category({NeedsExternalResource.class, NotJenkins.class}) // Needs CmdrServer to be started up
public class TestCdmrNetcdfFile {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalFromTestDataDir, new SuffixFileFilter(".nc"), result, true);
      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);

      // result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/bufr/userExamples/WMO_v16_3-10-61.bufr"});

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String cdmrUrl;

  public TestCdmrNetcdfFile(String filename) {
    this.filename = filename.replace("\\", "/");

    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + this.filename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestCdmrNetcdfFile %s%n", filename);
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        CdmrNetcdfFile cdmrFile = CdmrNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      // Just the header
      Formatter errlog = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, cdmrFile, errlog, false, false, false);
      if (!ok) {
        System.out.printf("FAIL %s %s%n", cdmrUrl, errlog);
      }
      assertThat(ok).isTrue();

      for (Variable v : ncfile.getVariables()) {
        if (v.getDataType() == DataType.SEQUENCE) {
          System.out.printf("  read sequence %s %s%n", v.getDataType(), v.getShortName());
          Sequence s = (Sequence) v;
          StructureDataIterator orgSeq = s.getStructureIterator(-1);
          Sequence copyv = (Sequence) cdmrFile.findVariable(v.getFullName());
          Iterator<StructureData> array = copyv.iterator();
          Formatter f = new Formatter();
          boolean ok1 = CompareArrayToMa2.compareSequence(f, v.getShortName(), orgSeq, array);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;

        } else {
          ucar.ma2.Array org = v.read();
          Variable cdmrVar = cdmrFile.findVariable(v.getFullName());
          Array<?> array = v.readArray();
          System.out.printf("  check %s %s%n", v.getDataType(), v.getNameAndDimensions());
          Formatter f = new Formatter();
          boolean ok1 = CompareArrayToMa2.compareData(f, v.getShortName(), org, array, false, true);
          if (!ok1) {
            System.out.printf("%s%n", f);
          }
          ok &= ok1;
        }
      }
      assertThat(ok).isTrue();
    }
  }

}
