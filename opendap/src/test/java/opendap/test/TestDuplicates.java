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

import java.util.Formatter;
import org.junit.Test;
import ucar.nc2.dods.DodsNetcdfFile;
import ucar.nc2.write.CDLWriter;
import ucar.unidata.util.test.TestDir;
import java.util.ArrayList;
import java.util.List;

public class TestDuplicates extends UnitTestCommon {
  public TestDuplicates() {
    setTitle("DAP duplicate names tests");
  }

  @Test
  public void testDuplicates() throws Exception {
    // Check if we are running against remote or localhost, or what.
    String testserver = TestDir.dap2TestServer;

    List<Testcase> results = new ArrayList<>();
    results.add(new Testcase("Top and field vars have same names", "http://" + testserver + "/dts/structdupname",
        String.format("netcdf dods://%s/dts/structdupname {%n" + "  variables:%n" + "    int time;%n" + "%n"
            + "    Structure {%n" + "      float time;%n" + "    } record;%n" + "%n" + "}%n", testserver)));
    results.add(new Testcase("TestFailure", "http://" + testserver + "/dts/simplestruct",
        String.format("netcdf dods://%s/dts/simplestruct {%n" + "  variables:%n" + "    Structure {%n"
            + "      int i32;%n" + "    } types;%n" + "%n" + "}%n", testserver)));

    for (Testcase result : results) {
      System.out.println("TestDuplicates: " + result.url);
      try (DodsNetcdfFile ncfile = DodsNetcdfFile.builder().build(result.url, null)) {
        Formatter cdl = new Formatter();
        CDLWriter.writeCDL(ncfile, cdl, false, null);

        assertThat(cdl.toString()).isEqualTo(baseline(result));

        // Dump the output for visual comparison
        if (System.getProperty("visual") != null) {
          System.out.println("Testing " + result.title + " visual:");
          System.out.println("---------------");
          System.out.print(cdl.toString());
          System.out.println("---------------");
        }
      }
    }
  }


}
