/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package opendap.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.dods.DodsNetcdfFile;
import ucar.nc2.dods.RC;
import ucar.nc2.write.CDLWriter;
import ucar.unidata.util.test.TestDir;
import java.util.ArrayList;
import java.util.List;

public class TestGroups extends UnitTestCommon {
  boolean usegroups = RC.getUseGroups();
  String testserver = null;
  List<Testcase> testcases = null;

  public TestGroups() {
    super("DAP Group tests");
    // Check if user specified server.
    testserver = TestDir.dap2TestServer;
    defineTestcases();
  }

  void defineTestcases() {
    String threddsRoot = getThreddsroot();
    testcases = new ArrayList<>();
    if (false) {// use this arm to do debugging
      testcases.add(new Testcase("External user provided group example",
          "http://" + testserver + "/thredds/dodsC/testdods/K1VHR.nc",
          "file://" + threddsRoot + "/opendap/src/test/data/baselinemisc/K1VHR.cdl"));
    } else {

      testcases.add(new Testcase("Simple multiple groups", "dods://" + testserver + "/dts/group.test1", "netcdf dods://"
          + testserver
          + "/dts/group.test1 {\ngroup: g1 {\nvariables:\nint i32;\n}\ngroup: g2 {\nvariables:\nfloat f32;\n}\n}\n"));
      testcases.add(new Testcase("Duplicate variable names in different groups",
          "dods://" + testserver + "/dts/group.test2", "netcdf dods://" + testserver
              + "/dts/group.test2 {\ngroup: g1 {\nvariables:\nint i32;\n}\ngroup: g2 {\nvariables:\nint i32;\n}\n}\n"));
      testcases.add(new Testcase("Duplicate coordinate variable names in Grid",
          "dods://" + testserver + "/dts/docExample", "netcdf dods://" + testserver
              + "/dts/docExample {\n dimensions:\n lat = 180;\n lon = 360;\n time = 404;\n variables:\n double lat(lat=180);\n :fullName = \"latitude\";\n :units = \"degrees North\";\n double lon(lon=360);\n :fullName = \"longitude\";\n :units = \"degrees East\";\n double time(time=404);\n :units = \"seconds\";\n int sst(time=404, lat=180, lon=360);\n :_CoordinateAxes = \"time lat lon \";\n :fullName = \"Sea Surface Temperature\";\n :units = \"degrees centigrade\";\n}\n"));
    }
  }

  @Test
  @Ignore("Needs usegroups")
  public void testGroup() throws Exception {
    // Run with usegroups == true
    assertWithMessage("usegroups must be on").that(usegroups).isTrue();
    System.out.println("TestGroups:");
    for (Testcase testcase : testcases) {
      System.out.println("url: " + testcase.url);
      doOne(testcase);
    }
  }

  void doOne(TestMisc.Testcase testcase) throws Exception {
    Formatter cdl = new Formatter();
    try (DodsNetcdfFile ncfile = DodsNetcdfFile.builder().build(testcase.url, null)) {
      CDLWriter.writeCDL(ncfile, cdl, false, null);
      String captured = cdl.toString();
      assertThat(captured).isEqualTo(baseline(testcase));
    }
  }

}
