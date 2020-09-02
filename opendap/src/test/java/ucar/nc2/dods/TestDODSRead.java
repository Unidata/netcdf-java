/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import java.util.ArrayList;
import java.util.List;
import opendap.test.TestSources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.IOException;

/**
 * Test nc2 dods in the JUnit framework.
 * Open and read various test datasets from the dts server.
 */
@RunWith(Parameterized.class)
public class TestDODSRead {
  public static boolean showFile = true, showFileDebug = true;

  @Parameterized.Parameters(name = "{0}")
  static public List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {"test.01", ""}); // scalars
    result.add(new Object[] {"test.02", ""}); // 1D arrays
    result.add(new Object[] {"test.03", ""}); // 3D arrays
    result.add(new Object[] {"test.04", ""}); // Structure with scalars
    result.add(new Object[] {"test.05", ""}); // nested Structures with scalars
    result.add(new Object[] {"test.06", ""}); // Grids
    result.add(new Object[] {"test.06a", ""}); // Grids
    result.add(new Object[] {"test.07", ""}); // Structure
    result.add(new Object[] {"test.07a", ""}); // Structure
    result.add(new Object[] {"test.21", ""}); // Structure with multidim fields
    result.add(new Object[] {"test.22", ""}); // array of structures
    result.add(new Object[] {"test.50", ""}); // array of structures with nested scalar structure
    result.add(new Object[] {"test.53", ""}); // array of structures with nested scalar structure
    result.add(new Object[] {"test.vs5", ""}); // structure array

    return result;
  }

  @Parameterized.Parameter(0)
  public String url;

  @Parameterized.Parameter(1)
  public String CE;

  @Test
  public void run() throws IOException {
    open(url);
  }

  public static DodsNetcdfFile open(String name) throws IOException {
    String filename = TestSources.XURL1 + "/" + name;
    return openAbs(filename);
  }

  public static DodsNetcdfFile openAbs(String filename) throws IOException {
    System.out.println("TestDODSRead = " + filename);
    DodsNetcdfFile dodsfile = DodsNetcdfFile.builder().build(filename, null);
    if (showFileDebug)
      System.out.println(dodsfile.getDetailInfo());
    if (showFile)
      System.out.println(dodsfile.toString());
    return dodsfile;
  }

}
