/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Just open all the files in the selected directory. */
@Category(NeedsCdmUnitTest.class)
public class TestReadFormats {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static int countGood = 0;
  private static int countFail = 0;

  private List<String> failFiles = new ArrayList<>();

  @Test
  public void testReadFormats() throws IOException {
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/gini", null, this::doRead);
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/fysat", null, this::doRead);
    System.out.printf("Good=%d Fail=%d%n", countGood, countFail);
    if (countFail > 0) {
      System.out.printf("Failed Files%n");
      for (String f : failFiles) {
        System.out.printf("  %s%n", f);
      }
    }
    assert countFail == 0 : "Failed = " + countFail;
  }

  private int doRead(String name) {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(name, null)) {
      System.out.printf("  GOOD on %s == %s%n", name, ncfile.getFileTypeId());
      countGood++;
      return 1;
    } catch (Throwable t) {
      System.out.printf("  FAIL on %s == %s%n", name, t.getMessage());
      failFiles.add(name);
      t.printStackTrace();
      countFail++;
      return 0;
    }
  }

}
