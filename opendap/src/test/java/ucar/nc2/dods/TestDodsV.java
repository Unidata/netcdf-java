package ucar.nc2.dods;

import java.io.IOException;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import org.junit.Test;

public class TestDodsV {

  ////////////////////////////////////////////////////////////////////////////////
  private static void doit(String urlName) throws IOException, DAP2Exception {
    System.out.println("DODSV read =" + urlName);
    try (DConnect2 dodsConnection = new DConnect2(urlName, true)) {

      // get the DDS
      DDS dds = dodsConnection.getDDS();
      dds.print(System.out);
      DodsV root = DodsV.parseDDS(dds);

      // get the DAS
      DAS das = dodsConnection.getDAS();
      das.print(System.out);
      root.parseDAS(das);

      // show the dodsV tree
      root.show(System.out, "");
    }
  }

  @Test
  public void testStuff() throws IOException, DAP2Exception {
    // doit("http://localhost:8080/thredds/dodsC/ncdodsTest/conventions/zebra/SPOL_3Volumes.nc");
    doit("http://iridl.ldeo.columbia.edu/SOURCES/.CAYAN/dods");
  }



}
