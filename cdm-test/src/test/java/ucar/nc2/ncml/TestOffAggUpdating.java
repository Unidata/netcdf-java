/*
 * Copyright (c) 1998-2022 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCacheable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Updating aggregation
 *
 * @author caron
 * @since Jul 24, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggUpdating {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String dir = TestDir.cdmUnitTestDir + "agg/updating";
  private static final String location = "test/location.ncml";
  private static final Path extraFile = Paths.get(dir, "extra.nc");
  private static final int expectedSizeOfTime = 12;
  private static final int expectedSizeOfTimeWithExtraFile = 18;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String getNcml() {
    return "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
        + "       <aggregation dimName='time' type='joinExisting' recheckEvery='1 msec'>\n"
        + "         <scan location='" + tempFolder.getRoot().toString() + "' suffix='*.nc' />\n"
        + "         <variable name='depth'>\n" + "           <attribute name='coordinates' value='lon lat'/>\n"
        + "         </variable>\n" + "         <variable name='wvh'>\n"
        + "           <attribute name='coordinates' value='lon lat'/>\n" + "         </variable>\n"
        + "       </aggregation>\n" + "       <attribute name='Conventions' type='String' value='CF-1.0'/>\n"
        + "</netcdf>";
  }

  @Before
  public void setup() throws IOException {
    Files.copy(Paths.get(dir, "ds1.nc"), Paths.get(tempFolder.getRoot().toString(), "ds1.nc"));
    Files.copy(Paths.get(dir, "ds2.nc"), Paths.get(tempFolder.getRoot().toString(), "ds2.nc"));
  }

  @Test
  public void testUpdateSync() throws IOException {
    // open the agg
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(getNcml()), location, null);
    check(ncfile, expectedSizeOfTime);

    addExtraFile();

    // reread
    ncfile.syncExtend();
    check(ncfile, expectedSizeOfTimeWithExtraFile);

    ncfile.close();
  }

  @Test
  public void testUpdateLastModified() throws IOException {
    // open the agg
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(getNcml()), location, null);
    long start = ncfile.getLastModified();

    addExtraFile();

    // reread
    long end = ncfile.getLastModified();
    assertThat(end).isGreaterThan(start);

    // again
    long end2 = ncfile.getLastModified();
    assertThat(end).isEqualTo(end2);

    ncfile.close();
  }

  @Test
  public void testUpdateCache() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);

    // open the agg
    NetcdfFile ncfile = NetcdfDatasets.acquireDataset(new NcmlStringFileFactory(), durl, null, -1, null, null);

    check(ncfile, expectedSizeOfTime);

    addExtraFile();

    // reread
    ncfile.syncExtend();
    check(ncfile, expectedSizeOfTimeWithExtraFile);

    ncfile.close();
  }

  private void check(NetcdfFile ncfile, int expectedSizeOfTime) {
    final Variable time = ncfile.findVariable("time");
    assertThat((Object) time).isNotNull();
    logger.debug(" time= {}", time.getNameAndDimensions());
    assertThat(time.getSize()).isEqualTo(expectedSizeOfTime);

    final Variable eta = ncfile.findVariable("eta");
    assertThat((Object) eta).isNotNull();
    assertThat(eta.getRank()).isEqualTo(3);
  }

  private class NcmlStringFileFactory implements ucar.nc2.util.cache.FileFactory {

    @Override
    public FileCacheable open(DatasetUrl durl, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return NcMLReader.readNcML(new StringReader(getNcml()), durl.trueurl, null);
    }
  }

  private void addExtraFile() throws IOException {
    Files.copy(extraFile, Paths.get(tempFolder.getRoot().toString(), "extra.nc"));
  }
}

