/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import java.util.Formatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.lang.invoke.MethodHandles;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ucar.nc2.internal.util.DiskCache2} */
public class TestDiskCache2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBasic() {
    DiskCache2 cache = DiskCache2.getDefault();
    System.out.printf("cache.getRootDirectory = %s%n", cache.getRootDirectory());
    File file = cache.getFile("gfs.t00z.master.grbf00.10m.uv.grib2"); // not exist
    System.out.printf("canWrite= %s%n", file.canWrite());
    assertThat(file.canWrite()).isFalse();

    Formatter f = new Formatter();
    cache.showCache(f);
    System.out.printf("cache.getRootDirectory = %s%n", f.toString());
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testReletivePath() throws Exception {
    String org = System.getProperty("user.dir");
    try {
      System.setProperty("user.dir", TestDir.cdmUnitTestDir);
      System.out.printf("user.dir = %s%n", System.getProperty("user.dir"));
      File pwd = new File(System.getProperty("user.dir"));

      String filename = "transforms/albers.ncml";
      File rel2 = new File(pwd, filename);
      System.out.printf("abs = %s%n", rel2.getCanonicalFile());
      assertThat(rel2.exists()).isTrue();
      assertThat(rel2.canWrite()).isTrue();
    } finally {
      System.setProperty("user.dir", org);
    }
  }

}
