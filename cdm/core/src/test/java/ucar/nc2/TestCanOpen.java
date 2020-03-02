/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ucar.unidata.util.test.TestDir;

@RunWith(Parameterized.class)
public class TestCanOpen {

  private final String filename;

  public TestCanOpen(String filename) {
    this.filename = filename;
  }

  @Parameters
  public static List<String> files() {
    List<String> files = Arrays.asList("example1.nc", "testSpecialAttributes.nc4", "chunked.h5");

    return files.stream().map(relFile -> TestDir.cdmLocalTestDataDir + relFile).collect(Collectors.toList());
  }

  @Test
  public void testCanOpen() throws IOException {
    assertThat(NetcdfFiles.canOpen(filename)).isTrue();
  }

}
