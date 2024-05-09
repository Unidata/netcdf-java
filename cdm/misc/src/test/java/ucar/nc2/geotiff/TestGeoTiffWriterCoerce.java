/*
 * Copyright (c) 1998-2024 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geotiff;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * GeotiffWriter writing geotiffs for coerced data types
 *
 * A huge limitation of this test is that the Geotiff class does not have a proper data reader.
 * It can only read the header and metadata. Therefore, it is very difficult to know for sure
 * if the data was written out correctly.
 *
 * The key difference between this test and TestGeoTiffWriter is that this set of tests uses
 * a newer API that allows the user to expcliitly set the data type of the output file while
 * the other test exercises an older API that forces the data type to be Floats.
 *
 * @author Ben Root
 * @since 5/9/2024
 */
@RunWith(Parameterized.class)
public class TestGeoTiffWriterCoerce {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}-{1}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {DataType.BYTE, FieldType.SBYTE});
    result.add(new Object[] {DataType.SHORT, FieldType.SSHORT});
    result.add(new Object[] {DataType.INT, FieldType.SLONG});
    result.add(new Object[] {DataType.FLOAT, FieldType.FLOAT});
    result.add(new Object[] {DataType.UBYTE, FieldType.BYTE});
    result.add(new Object[] {DataType.USHORT, FieldType.SHORT});
    result.add(new Object[] {DataType.UINT, FieldType.LONG});
    // The source data is signed integer, so when dtype is null, it should infer that. 
    result.add(new Object[] {null, FieldType.SLONG});
    return result;
  }

  private DataType dtype;
  private FieldType expectedFieldType;

  public TestGeoTiffWriterCoerce(DataType dtype, FieldType expectedFieldType) {
    this.dtype = dtype;
    this.expectedFieldType = expectedFieldType;
  }

  @Test
  public void testWriteCoverage() throws IOException, InvalidRangeException {
    String gridOut = tempFolder.newFile().getAbsolutePath();
    // This file has data that ranges from 0 to 255. It'll coerce just fine for
    // most datatypes except for BYTE, which overflows.
    String filename = "src/test/data/ucar/nc2/geotiff/categorical.nc";
    String field = "drought";
    FeatureType type = FeatureType.GRID;

    Array dtArray;
    float missingVal;
    MAMath.MinMax minmax;

    // Float, signed, unsigned
    int expectedSampleFormat = expectedFieldType.code >= 6 ? expectedFieldType.code == 11 ? 3 : 2 : 1;

    try (GridDataset dataset = GridDataset.open(filename)) {
      GridDatatype grid = dataset.findGridByName(field);
      assert grid != null;
      final GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;
      int rtindex = -1;
      int tindex = -1;
      CoordinateAxis1D timeAxis = gcs.getTimeAxis1D();
      assert timeAxis != null;
      tindex = (int) timeAxis.getSize() - 1; // last one
      dtArray = grid.readDataSlice(rtindex, -1, tindex, 0, -1, -1);
      minmax = grid.getMinMaxSkipMissingData(dtArray);
      missingVal = (float) minmax.min - 1.f;

      try (GeotiffWriter writer = new GeotiffWriter(gridOut)) {
        writer.writeGrid(dataset, grid, dtArray, false, dtype);
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

      IFDEntry sMinTag = geotiff.findTag(Tag.SMinSampleValue);
      Assert.assertNotNull(sMinTag);
      Assert.assertEquals(expectedFieldType, sMinTag.type);
      Assert.assertEquals(1, sMinTag.count);
      if (dtype != null && dtype.isFloatingPoint()) {
        Assert.assertEquals(minmax.min, (float) sMinTag.valueD[0], 0.0);
      } else {
        Assert.assertEquals((int) minmax.min, sMinTag.value[0]);
      }

      IFDEntry sMaxTag = geotiff.findTag(Tag.SMaxSampleValue);
      Assert.assertNotNull(sMaxTag);
      Assert.assertEquals(expectedFieldType, sMaxTag.type);
      Assert.assertEquals(1, sMaxTag.count);
      if (dtype != null && dtype.isFloatingPoint()) {
        Assert.assertEquals(minmax.max, (float) sMaxTag.valueD[0], 0.0);
      } else if (dtype == DataType.BYTE) {
        // This should have overflowed since 255 is outside the range of signed bytes,
        // so, we'll force the same overflow here and compare that.
        Assert.assertEquals((byte) minmax.max, (byte) sMaxTag.value[0]);
      } else {
        Assert.assertEquals((int) minmax.max, sMaxTag.value[0]);
      }

      IFDEntry noDataTag = geotiff.findTag(Tag.GDALNoData);
      if (dtype != null && dtype.isFloatingPoint()) {
        // Technically, it would be ambiguous what was intended due to the
        // backwards compatibility shim, so FLOATS still get NoData tags.
        Assert.assertNotNull(noDataTag);
        Assert.assertEquals(String.valueOf(missingVal), noDataTag.valueS);
      } else {
        Assert.assertNull(noDataTag);
      }

      IFDEntry sTypeTag = geotiff.findTag(Tag.SampleFormat);
      Assert.assertNotNull(sTypeTag);
      Assert.assertEquals(expectedSampleFormat, sTypeTag.value[0]);

      IFDEntry sSizeTag = geotiff.findTag(Tag.BitsPerSample);
      Assert.assertNotNull(sSizeTag);
      Assert.assertEquals(expectedFieldType.size * 8, sSizeTag.value[0]);

      String gridOut2 = tempFolder.newFile().getAbsolutePath();

      GeoReferencedArray covArray;
      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        assert cc != null;
        CoverageCollection gcd = cc.findCoverageDataset(type);
        Assert.assertNotNull(type.toString(), gcd);

        int pos = field.indexOf("/");
        String covName = (pos > 0) ? field.substring(pos + 1) : field;

        Coverage coverage = gcd.findCoverage(covName);
        Assert.assertNotNull(covName, coverage);
        CoverageCoordAxis1D z = (CoverageCoordAxis1D) coverage.getCoordSys().getZAxis();
        SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true);
        if (z != null)
          params.set(SubsetParams.vertCoord, z.getCoordMidpoint(0));
        covArray = coverage.readData(params);

        try (GeotiffWriter writer = new GeotiffWriter(gridOut2)) {
          writer.writeGrid(covArray, false, dtype);
        }
      }

      // Sanity check
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
