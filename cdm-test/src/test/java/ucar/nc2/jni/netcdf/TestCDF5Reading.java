package ucar.nc2.jni.netcdf;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.iosp.NCheader;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NeedsContentRoot;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test Reading of CDF-5 files using JNI netcdf-4 iosp
 */
@Category(NeedsContentRoot.class)
public class TestCDF5Reading extends UnitTestCommon {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ArrayFloat.D1 BASELINE;

  static {
    BASELINE = new ArrayFloat.D1(3);
    BASELINE.set(0, -3.4028235E38F);
    BASELINE.set(1, 3.4028235E38F);
    BASELINE.set(2, Float.NEGATIVE_INFINITY);
  }

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
  }

  @Test
  public void testReadSubsection() throws IOException, InvalidRangeException {
    String location = canonjoin(TestDir.cdmTestDataDir, "thredds/public/testdata/nc_test_cdf5.nc");
    try (RandomAccessFile raf = RandomAccessFile.acquire(location)) {
      // Verify that this is a netcdf-5 file
      int format = NCheader.checkFileType(raf);
      Assert.assertTrue("Fail: file format is not CDF-5", format == NCheader.NC_FORMAT_64BIT_DATA);
    }
    try (NetcdfFile jni = openJni(location)) {
      jni.setLocation(location + " (jni)");
      Array data = read(jni, "f4", "0:2");
      if (prop_visual) {
        String dump = NCdumpW.toString(data);
        logger.debug(dump);
        String testresult = dump.replace('r', ' ').replace('\n', ' ').trim();
        visual("CDF Read", testresult);
      }
      Assert.assertTrue(String.format("***Fail: data mismatch"), MAMath.nearlyEquals(data, BASELINE));
      System.err.println("***Pass");
    }
  }

  private Array read(NetcdfFile ncfile, String vname, String section) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable(vname);
    assert v != null;
    return v.read(section);
  }

  private NetcdfFile openJni(String location) throws IOException {
    Nc4Iosp iosp = new Nc4Iosp(NetcdfFileWriter.Version.netcdf4);
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
    RandomAccessFile raf = new RandomAccessFile(location, "r");
    iosp.open(raf, ncfile, null);
    return ncfile;
  }

}
