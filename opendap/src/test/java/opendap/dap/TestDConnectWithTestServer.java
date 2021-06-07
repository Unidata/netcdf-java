package opendap.dap;

import com.google.common.net.UrlEscapers;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class TestDConnectWithTestServer {
  private static final String testUrl =
      "https://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/Best";
  private static final String testCE = "time[0:42]";

  @Test
  public void testPlain() throws IOException, DAP2Exception {

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream output = new PrintStream(bytes);

    DConnect2 dc2 = new DConnect2(testUrl);
    DataDDS datadds = dc2.getData(testCE);
    bytes.reset();
    datadds.print(output);
    datadds.printVal(output);

    String result = new String(bytes.toByteArray());
    System.out.println(result);
  }

  // @Test
  public void testEscaped() throws IOException, DAP2Exception {

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream output = new PrintStream(bytes);

    DConnect2 dc2 = new DConnect2(testUrl);
    DataDDS datadds = dc2.getData(UrlEscapers.urlFragmentEscaper().escape(testCE));
    bytes.reset();
    datadds.print(output);
    datadds.printVal(output);

    String result = new String(bytes.toByteArray());
    System.out.println(result);
  }
}

/*
 * DConnect2.openConnection
 * https://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/Best.dods?time%5B0:42%5D
 * Dataset {
 * Float64 time[time = 43];
 * } grib/NCEP/GFS/CONUS_80km/Best;
 * Float64 time[time = 43] = {0.0, 6.0, 12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0, 66.0, 72.0, 78.0, 84.0,
 * 90.0, 96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0,
 * 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0, 234.0, 240.0, 246.0, 252.0};
 * 
 * 
 * DConnect2.openConnection
 * https://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/Best.dods?time[0:42]
 * Dataset {
 * Float64 time[time = 43];
 * } grib/NCEP/GFS/CONUS_80km/Best;
 * Float64 time[time = 43] = {0.0, 6.0, 12.0, 18.0, 24.0, 30.0, 36.0, 42.0, 48.0, 54.0, 60.0, 66.0, 72.0, 78.0, 84.0,
 * 90.0, 96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0,
 * 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0, 234.0, 240.0, 246.0, 252.0};
 */
