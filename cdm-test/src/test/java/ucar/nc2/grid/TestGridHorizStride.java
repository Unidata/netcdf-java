/* Copyright Unidata */
package ucar.nc2.grid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by John on 9/11/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGridHorizStride {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBestStride() throws IOException, ucar.array.InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "TwoD/Ozone_Mixing_Ratio_isobaric";
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(covName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordSystem();
      assertThat(hcs).isNotNull();

      GridReferencedArray geoArray = coverage.getReader().setHorizStride(2).read();
      GridCoordinateSystem gcs2 = geoArray.csSubset();
      assertThat(gcs2).isNotNull();
      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));

      GridHorizCoordinateSystem hcs2 = gcs2.getHorizCoordSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", java.util.Arrays.toString(hcs2.getShape()));

      int[] expectedShape = geoArray.data().getShape();
      int n = expectedShape.length;
      expectedShape[n - 1] = (expectedShape[n - 1] + 1) / 2;
      expectedShape[n - 2] = (expectedShape[n - 2] + 1) / 2;

      assertThat(hcs2.getShape()).isEqualTo(expectedShape);
    }
  }

  @Test
  public void TestGribCurvilinearHorizStride() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2"; // GRIB
                                                                                                         // Curvilinear
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.isProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);
      CoverageCoordSys csys = coverage.getCoordSys();
      int[] csysShape = csys.getShape();
      System.out.printf("csys shape = %s%n", Arrays.toString(csysShape));

      Formatter errLog = new Formatter();
      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).setHorizStride(2);
      Optional<CoverageCoordSys> opt = csys.subset(params, errLog);
      if (!opt.isPresent()) {
        System.out.printf("err=%s%n", errLog.toString());
        assert false;
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      int[] subsetShape = subsetCoordSys.getShape();
      System.out.printf("csysSubset shape = %s%n", Arrays.toString(subsetShape));

      int n = csysShape.length;
      csysShape[n - 1] = (csysShape[n - 1] + 1) / 2;
      csysShape[n - 2] = (csysShape[n - 2] + 1) / 2;

      Assert.assertArrayEquals(csysShape, subsetShape);

      ///////////////////////////
      GeoReferencedArray geo = coverage.readData(params);
      System.out.printf("CoordSysForData shape=%s%n", Arrays.toString(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      System.out.printf("data shape=%s%n", Arrays.toString(data.getShape()));
      Assert.assertArrayEquals(csysShape, data.getShape());
    }
  }
}
