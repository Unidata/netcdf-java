/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.nc2.write.NetcdfFormatWriter.Builder;
import static org.junit.Assert.assertEquals;

/** Test writing data and reading slices of it. */
public class TestSlice {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DATA_VARIABLE = "data";
  private static final int DIM_T = 10;
  private static final int DIM_ALT = 5;
  private static final int DIM_LAT = 123;
  private static final int DIM_LON = 234;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String filePath;

  @Before
  public void setUp() throws IOException, InvalidRangeException {
    filePath = tempFolder.newFile().getAbsolutePath();

    Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filePath);
    writerb.addDimension("t", DIM_T);
    writerb.addDimension("alt", DIM_ALT);
    writerb.addDimension("lat", DIM_LAT);
    writerb.addDimension("lon", DIM_LON);
    writerb.addVariable(DATA_VARIABLE, DataType.FLOAT, "t alt lat lon");

    try (NetcdfFormatWriter writer = writerb.build()) {
      writer.write(DATA_VARIABLE, createData());
    }
  }

  private Array createData() {
    ArrayFloat.D4 values = new ArrayFloat.D4(DIM_T, DIM_ALT, DIM_LAT, DIM_LON);
    for (int i = 0; i < DIM_T; i++) {
      for (int j = 0; j < DIM_ALT; j++) {
        for (int k = 0; k < DIM_LAT; k++) {
          for (int l = 0; l < DIM_LON; l++) {
            values.set(i, j, k, l, i + j);
          }
        }
      }
    }
    return values;
  }

  @Test
  public void testSlice1() throws IOException, InvalidRangeException {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced = var.slice(0, 3);
      sliced.read();

      int[] shape = sliced.getShape();
      assertEquals(3, shape.length);
      assertEquals(DIM_ALT, shape[0]);
      assertEquals(DIM_LAT, shape[1]);
      assertEquals(DIM_LON, shape[2]);

      assertEquals("alt lat lon", sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice2() throws IOException, InvalidRangeException {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced = var.slice(1, 3);
      sliced.read();

      int[] shape = sliced.getShape();
      assertEquals(3, shape.length);
      assertEquals(DIM_T, shape[0]);
      assertEquals(DIM_LAT, shape[1]);
      assertEquals(DIM_LON, shape[2]);

      assertEquals("t lat lon", sliced.getDimensionsString());
    }
  }

  @Test
  public void testSlice3() throws IOException, InvalidRangeException, ucar.array.InvalidRangeException {
    try (NetcdfFile file = NetcdfFiles.open(filePath)) {
      Variable var = file.findVariable(DATA_VARIABLE);
      Variable sliced1 = var.slice(0, 3);
      Variable sliced2 = sliced1.slice(0, 3);

      int[] shape = sliced2.getShape();
      assertEquals(2, shape.length);
      assertEquals(DIM_LAT, shape[0]);
      assertEquals(DIM_LON, shape[1]);

      assertEquals("lat lon", sliced2.getDimensionsString());

      ucar.array.Array<?> org = var.readArray(new ucar.array.Section("3,3,:,:"));
      ucar.array.Array<?> data = sliced2.readArray();
      CompareNetcdf2.compareData(DATA_VARIABLE, org, data);
    }
  }
}
