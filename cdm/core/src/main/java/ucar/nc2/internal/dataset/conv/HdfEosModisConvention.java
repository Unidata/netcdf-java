/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.io.IOException;
import java.util.Optional;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.iosp.hdf4.HdfEos;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.projection.Sinusoidal;

/**
 * HDF4-EOS TERRA MODIS
 *
 * @author caron
 * @see "https://lpdaac.usgs.gov/products/modis_overview"
 * @since 2/23/13
 */
public class HdfEosModisConvention extends CoordSystemBuilder {
  private static final String CRS = "Projection";
  private static final String DATA_GROUP = "Data_Fields";
  private static final String DIMX_NAME = "XDim";
  private static final String DIMY_NAME = "YDim";
  private static final String TIME_NAME = "time";
  private static final String CONVENTION_NAME = "HDF4-EOS-MODIS";

  private HdfEosModisConvention(NetcdfDataset.Builder datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  public static class Factory implements CoordSystemBuilderFactory {

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(NetcdfFile ncfile) {
      if (!ncfile.getFileTypeId().equals("HDF4-EOS")) {
        return false;
      }

      String typeName = ncfile.getRootGroup().attributes().findAttValueIgnoreCase(CF.FEATURE_TYPE, null);
      if (typeName == null) {
        return false;
      }
      if (!typeName.equals(FeatureType.GRID.toString()) && !typeName.equals(FeatureType.SWATH.toString())) {
        return false;
      }

      return checkGroup(ncfile.getRootGroup());
    }

    private boolean checkGroup(Group g) {
      Variable crs = g.findVariable(HdfEos.HDFEOS_CRS);
      Group dataG = g.findGroupLocal(DATA_GROUP);
      if (crs != null && dataG != null) {
        Attribute att = crs.findAttribute(HdfEos.HDFEOS_CRS_Projection);
        if (att == null) {
          return false;
        }
        if (!att.getStringValue().equals("GCTP_SNSOID") && !att.getStringValue().equals("GCTP_GEO")) {
          return false;
        }
        return !(dataG.findDimensionLocal(DIMX_NAME) == null || dataG.findDimensionLocal(DIMY_NAME) == null);
      }

      for (Group ng : g.getGroups()) {
        if (checkGroup(ng)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new HdfEosModisConvention(datasetBuilder);
    }
  }

  /*
   * group: MODIS_Grid_16DAY_250m_500m_VI {
   * variables:
   * short _HDFEOS_CRS;
   * :Projection = "GCTP_SNSOID";
   * :UpperLeftPointMtrs = -2.0015109354E7, 1111950.519667; // double
   * :LowerRightMtrs = -1.8903158834333E7, 0.0; // double
   * :ProjParams = 6371007.181, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double
   * :SphereCode = "-1";
   *
   *
   * group: Data_Fields {
   * dimensions:
   * YDim = 4800;
   * XDim = 4800;
   * variables:
   * short 250m_16_days_NDVI(YDim=4800, XDim=4800);
   * ...
   *
   */
  private boolean addTimeCoord;

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    addTimeCoord = addTimeCoordinate();
    augmentGroup(rootGroup);
    rootGroup.addAttribute(new Attribute(CDM.CONVENTIONS, "CF-1.0"));
  }

  private boolean addTimeCoordinate() {
    // add time coordinate by parsing the filename, of course.
    CalendarDate cd = parseFilenameForDate(datasetBuilder.orgFile.getLocation());
    if (cd == null) {
      return false;
    }

    rootGroup.addAttribute(new Attribute("_MODIS_Date", cd.toString()));

    // add the time dimension
    rootGroup.addDimension(new Dimension(TIME_NAME, 1));

    // add the coordinate variable
    String units = "seconds since " + cd;
    CoordinateAxis.Builder timeCoord = CoordinateAxis1D.builder().setName(TIME_NAME).setDataType(DataType.DOUBLE)
        .setDimensionsByName("").setUnits(units).setDesc("time coordinate");
    timeCoord.setAutoGen(0, 0);
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    datasetBuilder.replaceCoordinateAxis(rootGroup, timeCoord);

    return true;
  }

  private CalendarDate parseFilenameForDate(String filename) {
    // filename MOD13Q1.A2000065.h11v04.005.2008238031620.hdf
    String[] tokes = filename.split("\\.");
    if (tokes.length < 2) {
      return null;
    }
    if (tokes[1].length() < 8) {
      return null;
    }
    String want = tokes[1];
    String yearS = want.substring(1, 5);
    String jdayS = want.substring(5, 8);
    try {
      int year = Integer.parseInt(yearS);
      int jday = Integer.parseInt(jdayS);
      return CalendarDate.withDoy(null, year, jday, 0, 0, 0);
    } catch (Exception e) {
      return null;
    }
  }

  private void augmentGroup(Group.Builder g) {
    Optional<Variable.Builder<?>> crs = g.findVariableLocal(HdfEos.HDFEOS_CRS);
    if (crs != null) {
      augmentGroupWithProjectionInfo(g);
    }

    for (Group.Builder nested : g.gbuilders) {
      augmentGroup(nested);
    }
  }

  private void augmentGroupWithProjectionInfo(Group.Builder g) {
    Optional<Group.Builder> dataGopt = g.findGroupLocal(DATA_GROUP);
    if (!dataGopt.isPresent()) {
      return;
    }
    Group.Builder dataG = dataGopt.get();

    Optional<Dimension> dimXopt = dataG.findDimensionLocal(DIMX_NAME);
    Optional<Dimension> dimYopt = dataG.findDimensionLocal(DIMY_NAME);
    if (!dimXopt.isPresent() || !dimYopt.isPresent()) {
      return;
    }
    Dimension dimX = dimXopt.get();
    Dimension dimY = dimYopt.get();

    g.findVariableLocal(HdfEos.HDFEOS_CRS).ifPresent(crs -> {
      Attribute projAtt = crs.getAttributeContainer().findAttribute(HdfEos.HDFEOS_CRS_Projection);
      if (projAtt == null) {
        return;
      }
      Attribute upperLeft = crs.getAttributeContainer().findAttribute(HdfEos.HDFEOS_CRS_UpperLeft);
      Attribute lowerRight = crs.getAttributeContainer().findAttribute(HdfEos.HDFEOS_CRS_LowerRight);
      Attribute projParams = crs.getAttributeContainer().findAttribute(HdfEos.HDFEOS_CRS_ProjParams);
      if (upperLeft == null || lowerRight == null) {
        return;
      }

      double minX = upperLeft.getNumericValue(0).doubleValue();
      double minY = upperLeft.getNumericValue(1).doubleValue();

      double maxX = lowerRight.getNumericValue(0).doubleValue();
      double maxY = lowerRight.getNumericValue(1).doubleValue();

      boolean hasProjection = false;
      String coordinates = null;
      ProjectionCT ct;
      if (projAtt.getStringValue().equals("GCTP_SNSOID")) {
        hasProjection = true;
        ct = makeSinusoidalProjection(CRS, projParams);
        VariableDS.Builder crss = makeCoordinateTransformVariable(ct);
        crss.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
        dataG.addVariable(crss);

        datasetBuilder.replaceCoordinateAxis(dataG, makeCoordAxis(dataG, DIMX_NAME, dimX.getLength(), minX, maxX, true));
        datasetBuilder.replaceCoordinateAxis(dataG, makeCoordAxis(dataG, DIMY_NAME, dimY.getLength(), minY, maxY, false));
        coordinates = addTimeCoord ? TIME_NAME + " " + DIMX_NAME + " " + DIMY_NAME : DIMX_NAME + " " + DIMY_NAME;

      } else if (projAtt.getStringValue().equals("GCTP_GEO")) {
        datasetBuilder.replaceCoordinateAxis(dataG,
            makeLatLonCoordAxis(dataG, dimX.getLength(), minX * 1e-6, maxX * 1e-6, true));
        datasetBuilder.replaceCoordinateAxis(dataG,
            makeLatLonCoordAxis(dataG, dimY.getLength(), minY * 1e-6, maxY * 1e-6, false));
        coordinates = addTimeCoord ? TIME_NAME + " Lat Lon" : "Lat Lon";
      }

      for (Variable.Builder v : dataG.vbuilders) {
        if (v.getRank() != 2) {
          continue;
        }
        if (!v.getDimensionName(0).equals(dimY.getShortName())) {
          continue;
        }
        if (!v.getDimensionName(1).equals(dimX.getShortName())) {
          continue;
        }

        if (coordinates != null) {
          v.addAttribute(new Attribute(CF.COORDINATES, coordinates));
        }

        if (hasProjection) {
          v.addAttribute(new Attribute(CF.GRID_MAPPING, CRS));
        }
      }
    });

  }

  /*
   * The UpperLeftPointMtrs is in projection coordinates, and identifies the very upper left corner of the upper left
   * pixel of the image data
   * • The LowerRightMtrs identifies the very lower right corner of the lower right pixel of the image data. These
   * projection coordinates are the only metadata that accurately reflect the extreme corners of the gridded image
   */
  private CoordinateAxis.Builder makeCoordAxis(Group.Builder dataG, String name, int n, double start, double end, boolean isX) {
    CoordinateAxis.Builder vb = CoordinateAxis1D.builder().setName(name).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(dataG).setDimensionsByName(name)
        .setUnits("km").setDesc(isX ? "x coordinate" : "y coordinate");

    double incr = (end - start) / n;
    vb.setAutoGen(start * .001, incr * .001); // km
    vb.addAttribute(new Attribute(_Coordinate.AxisType, isX ? AxisType.GeoX.name() : AxisType.GeoY.name()));
    return vb;
  }

  /*
   * group: MOD_Grid_MOD17A3 {
   * variables:
   * short _HDFEOS_CRS;
   * :Projection = "GCTP_GEO";
   * :UpperLeftPointMtrs = -1.8E8, 9.0E7; // double
   * :LowerRightMtrs = 1.8E8, -9.0E7; // double
   */
  private CoordinateAxis.Builder makeLatLonCoordAxis(Group.Builder dataG, int n, double start, double end, boolean isLon) {
    String name = isLon ? AxisType.Lon.toString() : AxisType.Lat.toString();
    String dimName = isLon ? DIMX_NAME : DIMY_NAME;

    CoordinateAxis.Builder v = CoordinateAxis1D.builder().setName(name).setDataType(DataType.DOUBLE)
        .setParentGroupBuilder(dataG).setDimensionsByName(dimName).setUnits(isLon ? "degrees_east" : "degrees_north");

    double incr = (end - start) / n;
    v.setAutoGen(start, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, name));
    return v;
  }

  /*
   * 1 5 7 8
   * 16 PGSd_SNSOID Sphere CentMer FE FN
   */
  private ProjectionCT makeSinusoidalProjection(String name, Attribute projParams) {
    double radius = projParams.getNumericValue(0).doubleValue();
    double centMer = projParams.getNumericValue(4).doubleValue();
    double falseEast = projParams.getNumericValue(6).doubleValue();
    double falseNorth = projParams.getNumericValue(7).doubleValue();

    Sinusoidal proj = new Sinusoidal(centMer, falseEast * .001, falseNorth * .001, radius * .001);
    return new ProjectionCT(name, "FGDC", proj);
  }
}
