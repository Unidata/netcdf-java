package ucar.nc2.ncml;

import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jdom2.Element;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
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

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
  }

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
    result.add(new Object[] {datadir + "formats/dmsp/F14200307192230.n.OIS", false}); //

    result.add(new Object[] {datadir + "formats/nexrad/level2/6500KHGX20000610_000110", false});
    result.add(new Object[] {datadir + "formats/nexrad/level2/Level2_KYUX_20060527_2335.ar2v", true});

    result.add(new Object[] {datadir + "conventions/nuwg/ocean.nc", true});

    // try everything from these directories
    try {
      addFromScan(result, TestDir.cdmUnitTestDir + "formats/netcdf4/",
          new NotFileFilter(new SuffixFileFilter(new String[] {".cdl", ".nc5"})), false);
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
  boolean showFiles = true;
  boolean compareData = false;

  public TestNcmlWriteAndCompareShared(String location, boolean compareData) throws IOException {
    this.durl = DatasetUrl.findDatasetUrl(location);
    this.compareData = compareData;
  }

  // String location;
  DatasetUrl durl;

  int fail = 0;
  int success = 0;

  @Test
  public void compareNcML() throws IOException {
    compareNcML(true, true, true);
    compareNcML(true, false, false);
    compareNcML(false, true, false);
    compareNcML(false, false, true);
    compareNcML(false, false, false);
  }

  public void compareNcML(boolean useRecords, boolean explicit, boolean openDataset) throws IOException {
    if (compareData)
      useRecords = false;

    if (showFiles) {
      System.out.println("-----------");
      System.out.println("  input filename= " + durl.trueurl);
    }

    NetcdfFile org;
    if (openDataset)
      org = NetcdfDataset.openDataset(durl.trueurl, false, null);
    else
      org = NetcdfDataset.acquireFile(durl, null);

    if (useRecords)
      org.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // create a file and write it out
    int pos = durl.trueurl.lastIndexOf("/");
    String ncmlOut = tempFolder.newFile().getAbsolutePath();
    if (showFiles)
      System.out.println(" output filename= " + ncmlOut);

    try {
      NcMLWriter ncmlWriter = new NcMLWriter();
      Element netcdfElement;

      if (explicit) {
        netcdfElement = ncmlWriter.makeExplicitNetcdfElement(org, null);
      } else {
        netcdfElement = ncmlWriter.makeNetcdfElement(org, null);
      }

      ncmlWriter.writeToFile(netcdfElement, new File(ncmlOut));
    } catch (IOException ioe) {
      // ioe.printStackTrace();
      assert false : ioe.getMessage();
    }

    // read it back in
    NetcdfFile copy;
    if (openDataset)
      copy = NetcdfDataset.openDataset(ncmlOut, false, null);
    else
      copy = NetcdfDataset.acquireFile(DatasetUrl.findDatasetUrl(ncmlOut), null);

    if (useRecords)
      copy.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    try {
      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, compareData);
      boolean ok = mind.compare(org, copy, new CompareNetcdf2.Netcdf4ObjectFilter(), false, false, compareData);
      if (!ok) {
        fail++;
        System.out.printf("--Compare %s, useRecords=%s explicit=%s openDataset=%s compareData=%s %n", durl.trueurl,
            useRecords, explicit, openDataset, compareData);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK (useRecords=%s explicit=%s openDataset=%s compareData=%s)%n",
            durl.trueurl, useRecords, explicit, openDataset, compareData);
        success++;
      }
      Assert.assertTrue(durl.trueurl, ok);
    } finally {
      org.close();
      copy.close();
    }
  }

}
