package ucar.nc2.dods;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DConnect2;
import opendap.dap.DDS;
import org.junit.Test;
import ucar.unidata.io.http.ReadFromUrl;

public class TestDodsV {

  @Test
  public void test() throws IOException, DAP2Exception {
    doOne("http://localhost:8080/dts/1day");
  }

  private static void doOne(String urlName) throws IOException, DAP2Exception {
    System.out.println("DODSV read =" + urlName);
    try (DConnect2 dodsConnection = new DConnect2(urlName, true)) {

      // get the DDS
      DDS dds = dodsConnection.getDDS();
      String actualDDS = dds.toString();
      String expectedDDS = ReadFromUrl.readURLcontents(urlName + ".dds");
      assertThat(actualDDS).isEqualTo(expectedDDS);

      // get the DAS
      DAS das = dodsConnection.getDAS();
      String actualDAS = das.toString();
      String expectedDAS = ReadFromUrl.readURLcontents(urlName + ".das");
      assertThat(actualDAS).isEqualTo(expectedDAS);

      DodsV root = DodsV.parseDDS(dds);
      root.parseDAS(das);

      // show the dodsV tree
      root.show(System.out, "");
    }
  }



}
