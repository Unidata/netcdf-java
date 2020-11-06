/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.internal.dataset.CoordTransformFactory;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.util.Parameter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Test Horizontal projections. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestProjections {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static String testDir = TestDir.cdmUnitTestDir + "transforms/";
  private static LatLonPoint testPoint = LatLonPoint.create(0, 145.0);

  @Parameterized.Parameters(name = "{0}-{1}")
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][] {

        {testDir + "Sigma_LC.nc", "Lambert_Conformal", "Temperature", LambertConformal.class, null},

        {testDir + "LambertAzimuth.nc", "grid_mapping0", "VIL", LambertAzimuthalEqualArea.class, null},

        {testDir + "PolarStereographic.nc", "Polar_Stereographic", "D2_O3", Stereographic.class, null},

        {testDir + "Polar_Stereographic2.nc", null, "dpd-Surface0", Stereographic.class, null},

        {testDir + "Base_month.nc", null, "D2_SO4", Stereographic.class, null},

        {testDir + "Mercator.grib1", "Mercator_Projection", "Temperature_isobaric", Mercator.class, null},

        {testDir + "Eumetsat.VerticalPerspective.grb", "SpaceViewPerspective_Projection", "Pixel_scene_type",
            MSGnavigation.class, testPoint},

        {testDir + "sinusoidal/MOD13Q1.A2008033.h12v04.005.2008051065305.hdf",
            "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/Projection",
            "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/250m_16_days_NDVI", Sinusoidal.class, testPoint},

        {testDir + "heiko/topo_stere_sphere.nc", "projection_stere", "air_temperature_2m", Stereographic.class, null},

        {testDir + "heiko/topo_stere_WGS.nc", "projection_stere", "air_temperature_2m",
            ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection.class, null},

        {testDir + "heiko/topo_utm_sphere.nc", "projection_tmerc", "air_temperature_2m",
            ucar.unidata.geoloc.projection.TransverseMercator.class, null},

        {testDir + "heiko/topo_utm_WGS.nc", "projection_tmerc", "air_temperature_2m",
            ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection.class, null},

        {testDir + "rotatedPole/snow.DMI.ecctrl.v5.ncml", "rotated_pole", "snow", RotatedPole.class, null},

        {testDir + "melb-small_LCEA.nc", "lambert_cylindrical_equal_area", "Band1",
            CylindricalEqualAreaProjection.class, testPoint},

        {testDir + "melb-small_AZE.nc", "azimuthal_equidistant", "Band1", EquidistantAzimuthalProjection.class,
            LatLonPoint.create(-37, 145.0)},

        // :sweep_angle_axis = "x";
        // :longitude_of_projection_origin = -75.0; covers western hemisphere
        {testDir + "geostationary/IT_ABI-L2-CMIPF-M3C16_G16_s2005155201500_e2005155203700_c2014058132255.nc",
            "goes_imager_projection", "CMI", Geostationary.class, LatLonPoint.create(-37, -45.0)},

        // check to make sure map coordinates in microradians handled
        // https://github.com/Unidata/thredds/issues/1008
        {testDir + "geostationary/GOES16_FullDisk_20180205_060047_0.47_6km_0.0S_75.0W.nc4", "fixedgrid_projection",
            "Sectorized_CMI", Geostationary.class, LatLonPoint.create(40, -105)},

        {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_0000.grib2.ncx4",
            "LatLon_Projection", "Absolute_vorticity_isobaric", LatLonProjection.class, testPoint}};

    return Arrays.asList(data);
  }


  String filename;
  String ctvName;
  String varName;
  Class projClass;
  LatLonPoint testPt;

  public TestProjections(String filename, String ctvName, String varName, Class projClass, LatLonPoint testPt) {
    this.filename = filename;
    this.ctvName = ctvName;
    this.varName = varName;
    this.projClass = projClass;
    this.testPt = testPt;
  }

  @Test
  public void testOneProjection() throws IOException {
    System.out.printf("Open %s %n", filename);
    try (NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Variable ctv = null;
      if (ctvName != null) {
        ctv = ncd.findVariable(ctvName);
        assertThat(ctv).isNotNull();
        logger.debug(" dump of ctv = {}", ctv);
      }

      VariableDS v = (VariableDS) ncd.findVariable(varName);
      assertThat(v).isNotNull();

      List<CoordinateSystem> cList = v.getCoordinateSystems();
      assertThat(cList).isNotNull();
      assertThat(cList.size()).isEqualTo(1);
      CoordinateSystem csys = cList.get(0);

      List<CoordinateTransform> pList = new ArrayList<>();
      List<CoordinateTransform> tList = csys.getCoordinateTransforms();
      assertThat(tList).isNotNull();
      for (CoordinateTransform ct : tList) {
        if (ct.getTransformType() == TransformType.Projection)
          pList.add(ct);
      }
      assertThat(pList.size()).isEqualTo(1);
      CoordinateTransform ct = pList.get(0);
      assertThat(ct.getTransformType()).isEqualTo(TransformType.Projection);
      assertThat(ct instanceof ProjectionCT).isTrue();

      ProjectionCT vct = (ProjectionCT) ct;
      Projection proj = vct.getProjection();
      assertThat(proj).isNotNull();
      assertThat(projClass.isInstance(proj)).isTrue();

      if (projClass != RotatedPole.class) {
        logger.debug("Projection Parameters");
        boolean found = false;
        double radius = 0.0;
        for (Parameter p : proj.getProjectionParameters()) {
          logger.debug("{}", p);
          if (p.getName().equals(CF.EARTH_RADIUS)) {
            found = true;
            radius = p.getNumericValue();
          }
          if (p.getName().equals(CF.SEMI_MAJOR_AXIS)) {
            found = true;
            radius = p.getNumericValue();
          }
        }

        assertThat(found).isTrue();
        assertThat(radius).isGreaterThan(10000);
      }

      VariableDS ctvSyn = CoordTransformFactory.makeDummyTransformVariable(ncd, ct);
      logger.debug(" dump of equivilent ctv = {}", ctvSyn);

      if (ctv != null) {
        Formatter f = new Formatter();
        CompareNetcdf2.checkContains("CoordTransBuilder", ImmutableList.copyOf(ctv.attributes()),
            ImmutableList.copyOf(ctvSyn.attributes()), f);
        logger.debug(f.toString());
      }

      if (testPt != null) {
        ProjectionPoint pt = proj.latLonToProj(testPt);
        assert pt != null;
        assert !Double.isNaN(pt.getX());
        assert !Double.isNaN(pt.getY());
      }

    }
  }
}
