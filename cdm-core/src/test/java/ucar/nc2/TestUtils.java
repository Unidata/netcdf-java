/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.*;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Static utilities for testing */
public class TestUtils {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** read all data, make sure variable metadata matches the array */
  public static void testReadData(NetcdfFile ncfile, boolean showStatus) {
    try {
      for (Variable v : ncfile.getVariables()) {
        testVarMatchesData(v, showStatus);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      fail();
    }

    if (showStatus)
      logger.debug("**** testReadData done on {}", ncfile.getLocation());
  }

  public static void testVarMatchesData(Variable v, boolean showStatus) throws IOException {
    Array<?> data = v.readArray();
    assertThat(data.getSize()).isEqualTo(v.getSize());

    assertThat(data.getRank()).isEqualTo(v.getRank());
    int[] dataShape = data.getShape();
    int[] varShape = v.getShape();
    for (int i = 0; i < data.getRank(); i++) {
      assertThat(dataShape[i]).isEqualTo(varShape[i]);
    }

    if (showStatus)
      logger.debug("**** testReadData done on {}", v.getFullName());
  }

  public static Group makeDummyGroup() {
    return Group.builder().setName("").build();
  }
}
