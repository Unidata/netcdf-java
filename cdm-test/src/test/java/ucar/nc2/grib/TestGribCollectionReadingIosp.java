/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.IndexFn;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Misc;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * Read data through the Grib iosp (NetcdfDataset).
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionReadingIosp {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadBest() throws Exception {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    String covName = "Best/Temperature_height_above_ground";
    logger.debug("open {} var={}", endpoint, covName);

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(endpoint)) {
      assertThat(ds).isNotNull();
      Variable v = ds.findVariable(covName);
      assertThat(v).isNotNull();
      assertThat(v).isInstanceOf(VariableDS.class);

      Variable time = ds.findVariable("Best/time3");
      Array timeData = time.readArray();
      System.out.printf("timeData = %s%n", NcdumpArray.printArray(timeData));

      Array<Float> data = (Array<Float>) v.readArray(new Section("30,0,:,:")); // Time coord : 180 ==
                                                                               // 2014-10-31T12:00:00Z

      float first = data.getScalar();
      assertThat(first).isWithin(1e-6f).of(300.33002f);

      IndexFn indexfn = IndexFn.builder(data.getShape()).build();
      float last = data.get(indexfn.odometer(data.getSize() - 1));
      assertThat(last).isWithin(1e-6f).of(279.49f);
    }
  }

  @Test
  public void testReadMrutpTimeRange() throws Exception {
    // read more than one time coordinate at a time in a MRUTP, no vertical
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.getRootGroup().findVariableLocal("Pressure_surface");
      assertThat(v).isNotNull();
      Array<Float> data = (Array<Float>) v.readArray(new Section("0:1,50,50"));
      assertThat(data).isNotNull();
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getSize()).isEqualTo(2);
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      float[] expect = new float[] {103031.914f, 103064.164f};
      int count = 0;
      for (float val : data) {
        assertThat(val).isWithin(1e-6f).of(expect[count++]);
      }
    }
  }

  @Test
  public void testReadMrutpTimeRangeWithSingleVerticalLevel() throws Exception {
    // read more than one time coordinate at a time in a MRUTP, with vertical
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.getRootGroup().findVariableLocal("Relative_humidity_sigma");
      assertThat(v).isNotNull();
      Array<Float> data = (Array<Float>) v.readArray(new Section("0:1, 0, 50, 50"));
      assertThat(data).isNotNull();
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getSize()).isEqualTo(2);
      for (float val : data) {
        assertThat(val).isNotNaN();
      }
      float[] expect = new float[] {68.0f, 74.0f};
      int count = 0;
      for (float val : data) {
        assertThat(val).isWithin(Misc.defaultMaxRelativeDiffFloat).of(expect[count++]);
      }
    }
  }

  @Test
  public void testReadMrutpTimeRangeWithMultipleVerticalLevel() throws Exception {
    // read more than one time coordinate at a time in a MRUTP. multiple verticals
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      Variable v = ds.getRootGroup().findVariableLocal("Relative_humidity_isobaric");
      assertThat(v).isNotNull();
      Array<Float> data = (Array<Float>) v.readArray(new Section("0:1, 10:20:2, 50, 50"));
      assertThat(data).isNotNull();
      assertThat(data.getRank()).isEqualTo(4);
      assertThat(data.getSize()).isEqualTo(12);
      for (float val : data) {
        assertThat(val).isNotNaN();
      }
      float[] expect = new float[] {57.8f, 53.1f, 91.3f, 85.5f, 80.0f, 69.3f, 32.8f, 41.8f, 88.9f, 81.3f, 70.9f, 70.6f};
      int count = 0;
      for (float val : data) {
        assertThat(val).isWithin(Misc.defaultMaxRelativeDiffFloat).of(expect[count++]);
      }
    }
  }

}
