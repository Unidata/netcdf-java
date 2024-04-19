/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.apache.commons.io.FileUtils;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * GeoTiffWriter2 writing geotiffs
 *
 * @author caron
 * @since 7/31/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGeoTiffWriter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}-{3}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    for (int greyscale = 0; greyscale < 2; greyscale++) {
      result.add(
          new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4",
              FeatureType.GRID, "Temperature_sigma", greyscale == 1}); // SRC // TP
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4", FeatureType.GRID,
          "Pressure_surface", greyscale == 1}); // TP
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
          "Best/Soil_temperature_depth_below_surface_layer", greyscale == 1}); // TwoD Best
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.FMRC,
          "TwoD/Soil_temperature_depth_below_surface_layer", greyscale == 1}); // TwoD

      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID, "Temperature",
          greyscale == 1});
    }

    return result;
  }

  private String filename, field;
  private FeatureType type;
  private boolean greyscale;

  public TestGeoTiffWriter(String filename, FeatureType type, String field, boolean greyscale) {
    this.filename = filename;
    this.type = type;
    this.field = field;
    this.greyscale = greyscale;
  }

  @Test
  public void testWriteCoverage() throws IOException, InvalidRangeException {
    String gridOut = tempFolder.newFile().getAbsolutePath();
    logger.debug("geotiff read grid {} ({}) from {} write {} greyscale {}", field, type, filename, gridOut, greyscale);

    Array dtArray;
    float missingVal;
    MAMath.MinMax minmax;
    try (GridDataset gds = GridDataset.open(filename)) {
      GridDatatype grid = gds.findGridByName(field);
      assert grid != null;
      int rtindex = -1;
      int tindex = -1;
      CoordinateAxis timeAxis = grid.getCoordinateSystem().getTimeAxis();
      if (timeAxis instanceof CoordinateAxis2D) {
        int[] shape = timeAxis.getShape();
        rtindex = shape[0] - 1;
        tindex = shape[1] - 1;
      } else {
        CoordinateAxis rtimeAxis = grid.getCoordinateSystem().getRunTimeAxis();
        if (rtimeAxis != null)
          rtindex = (int) rtimeAxis.getSize() - 1; // last one
        timeAxis = grid.getCoordinateSystem().getTimeAxis1D();
        if (timeAxis != null)
          tindex = (int) timeAxis.getSize() - 1; // last one
      }
      dtArray = grid.readDataSlice(rtindex, -1, tindex, 0, -1, -1);
      minmax = grid.getMinMaxSkipMissingData(dtArray);
      missingVal = (float) minmax.min - 1.f;

      try (GeotiffWriter writer = new GeotiffWriter(gridOut)) {
        writer.writeGrid(gds, grid, dtArray, greyscale);
      }
    }

    // read it back in
    try (GeoTiff geotiff = new GeoTiff(gridOut)) {
      geotiff.read();
      logger.debug("{}", geotiff.showInfo());

      IFDEntry photoTag = geotiff.findTag(Tag.PhotometricInterpretation);
      Assert.assertNotNull(photoTag);
      Assert.assertEquals(1, photoTag.count);
      Assert.assertEquals(1, photoTag.value[0]);

      if (!greyscale) {
        IFDEntry sMinTag = geotiff.findTag(Tag.SMinSampleValue);
        Assert.assertNotNull(sMinTag);
        Assert.assertEquals(1, sMinTag.count);
        // In the Writer, the min/max values are cast to floats before encoding
        Assert.assertEquals(minmax.min, (float) sMinTag.valueD[0], 0.0);

        IFDEntry sMaxTag = geotiff.findTag(Tag.SMaxSampleValue);
        Assert.assertNotNull(sMaxTag);
        Assert.assertEquals(1, sMaxTag.count);
        Assert.assertEquals(minmax.max, (float) sMaxTag.valueD[0], 0.0);

        IFDEntry noDataTag = geotiff.findTag(Tag.GDALNoData);
        Assert.assertNotNull(noDataTag);
        Assert.assertEquals(String.valueOf(missingVal), noDataTag.valueS);
      } else {
        IFDEntry sMinTag = geotiff.findTag(Tag.SMinSampleValue);
        Assert.assertNull(sMinTag);

        IFDEntry sMaxTag = geotiff.findTag(Tag.SMaxSampleValue);
        Assert.assertNull(sMaxTag);

        IFDEntry noDataTag = geotiff.findTag(Tag.GDALNoData);
        Assert.assertNull(noDataTag);
      }

      String gridOut2 = tempFolder.newFile().getAbsolutePath();
      logger.debug("geotiff2 read coverage {} write {} greyscale {}", filename, gridOut2, greyscale);

      GeoReferencedArray covArray;
      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        assert cc != null;
        CoverageCollection gcd = cc.findCoverageDataset(type);
        Assert.assertNotNull(type.toString(), gcd);

        int pos = field.indexOf("/");
        String covName = (pos > 0) ? field.substring(pos + 1) : field;

        Coverage coverage = gcd.findCoverage(covName);
        CoverageCoordAxis1D z = (CoverageCoordAxis1D) coverage.getCoordSys().getZAxis();
        SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true);
        if (z != null)
          params.set(SubsetParams.vertCoord, z.getCoordMidpoint(0));
        Assert.assertNotNull(covName, coverage);
        covArray = coverage.readData(params);

        try (GeotiffWriter writer = new GeotiffWriter(gridOut2)) {
          writer.writeGrid(covArray, greyscale);
        }
      }

      CompareNetcdf2.compareData(field, dtArray, covArray.getData());

      // read it back in
      try (GeoTiff geotiff2 = new GeoTiff(gridOut2)) {
        geotiff2.read();
        logger.debug("{}", geotiff2.showInfo());

        Formatter out = new Formatter();
        CompareNetcdf2.compareLists(geotiff.getTags(), geotiff2.getTags(), out);
        logger.debug("{}", out.toString());
      }

      // compare file s are equal
      File file1 = new File(gridOut);
      File file2 = new File(gridOut2);

      assert FileUtils.contentEquals(file1, file2);
    }
  }
}
