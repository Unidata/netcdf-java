package ucar.nc2.ncml;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.unidata.util.test.CompareNcml;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * TestWrite NcML, read back and compare with original.
 *
 * This is identical to TestNcmlWriteAndCompareLocal, except that we're using shared datasets.
 *
 * @author caron
 * @since 11/2/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNcmlWriteAndCompareShared {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    String datadir = TestDir.cdmUnitTestDir;

    List<Object[]> result = new ArrayList<>(500);

    // result.add(new Object[]{datadir + "formats/netcdf4/tst/test_enum_type.nc", false});
    result.add(new Object[] {datadir + "conventions/atd-radar/rgg.20020411.000000.lel.ll.nc", false});
    result.add(new Object[] {datadir + "conventions/atd-radar/SPOL_3Volumes.nc", false});
    result.add(new Object[] {datadir + "conventions/awips/19981109_1200.nc", false});
    result.add(new Object[] {datadir + "conventions/cf/ccsm2.nc", false}); //
    result.add(new Object[] {datadir + "conventions/coards/cldc.mean.nc", false});
    result.add(new Object[] {datadir + "conventions/csm/o3monthly.nc", false});
    result.add(new Object[] {datadir + "conventions/gdv/OceanDJF.nc", false});
    result.add(new Object[] {datadir + "conventions/gief/coamps.wind_uv.nc", false});
    result.add(new Object[] {datadir + "conventions/mars/temp_air_01082000.nc", true});
    result.add(new Object[] {datadir + "conventions/nuwg/eta.nc", false});
    result.add(new Object[] {datadir + "conventions/nuwg/ocean.nc", true});
    result.add(new Object[] {datadir + "conventions/wrf/wrfout_v2_Lambert.nc", false});

    result.add(new Object[] {datadir + "formats/grib2/eta2.wmo", false}); //
    result.add(new Object[] {datadir + "formats/grib2/ndfd.wmo", false}); //

    result.add(new Object[] {datadir + "formats/gini/n0r_20041013_1852-compress", false}); //
    result.add(new Object[] {datadir + "formats/gini/ntp_20041206_2154", true}); //

    result.add(new Object[] {datadir + "formats/nexrad/level2/6500KHGX20000610_000110", false});
    result.add(new Object[] {datadir + "formats/nexrad/level2/Level2_KYUX_20060527_2335.ar2v", true});

    result.add(new Object[] {datadir + "conventions/nuwg/ocean.nc", true});

    // try everything from these directories
    try {
      addFromScan(result, TestDir.cdmUnitTestDir + "formats/netcdf4/",
          new NotFileFilter(new SuffixFileFilter(new String[] {".cdl", ".nc5", ".gbx9"})), false);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  // FIXME: This method sucks: it doesn't fail when dirName can't be read.
  static void addFromScan(final List<Object[]> list, String dirName, FileFilter ff, final boolean compareData)
      throws IOException {
    TestDir.actOnAll(dirName, ff, new TestDir.Act() {
      public int doAct(String filename) throws IOException {
        list.add(new Object[] {filename, compareData});
        return 1;
      }
    }, true);
  }

  /////////////////////////////////////////////////////////////
  private final boolean showFiles = true;
  private final boolean compareData;
  private final DatasetUrl durl;

  public TestNcmlWriteAndCompareShared(String location, boolean compareData) throws IOException {
    this.durl = DatasetUrl.findDatasetUrl(location);
    this.compareData = compareData;
  }

  @Test
  public void compareNcML() throws IOException {
    new CompareNcml(tempFolder, durl, compareData, showFiles);
  }

}
