/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.conv;

import java.util.Optional;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
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
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.CoordSystemBuilder;
import ucar.nc2.util.CancelTask;

/**
 * HDF5-EOS AURA OMI
 *
 * @author John
 * @since 12/26/12
 * @see "http://aura.gsfc.nasa.gov/instruments/omi.html"
 */
public class HdfEosOmiConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "HDF5-EOS-OMI";

  private HdfEosOmiConvention(NetcdfDataset.Builder datasetBuilder) {
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
      if (!ncfile.getFileTypeId().equals("HDF5-EOS")) {
        return false;
      }

      String typeName = ncfile.getRootGroup().findAttributeString(CF.FEATURE_TYPE, null);
      if (typeName == null) {
        return false;
      }
      if (!typeName.equals(FeatureType.GRID.toString()) && !typeName.equals(FeatureType.SWATH.toString())) {
        return false;
      }

      /*
       * group: HDFEOS {
       *
       * group: ADDITIONAL {
       *
       * group: FILE_ATTRIBUTES {
       * :InstrumentName = "OMI";
       * :ProcessLevel = "3e";
       * :GranuleMonth = 12; // int
       * :GranuleDay = 14; // int
       * :GranuleYear = 2005; // int
       * :GranuleDayOfYear = 348; // int
       * :TAI93At0zOfGranule = 4.08672005E8; // double
       * :PGEVersion = "0.9.26";
       * :StartUTC = "2005-12-13T18:00:00.000000Z";
       * :EndUTC = "2005-12-15T06:00:00.000000Z";
       * :Period = "Daily";
       * }
       * }
       */
      Attribute instName = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@InstrumentName");
      if (instName == null || !instName.getStringValue().equals("OMI")) {
        return false;
      }

      Attribute level = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@ProcessLevel");
      if (level == null) {
        return false;
      }

      return !(!level.getStringValue().startsWith("2") && !level.getStringValue().startsWith("3"));
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder) {
      return new HdfEosOmiConvention(datasetBuilder);
    }
  }

  /*
   * 
   * // level3
   * group: HDFEOS {
   * 
   * group: GRIDS {
   * 
   * group: OMI_Column_Amount_O3 {
   * dimensions:
   * XDim = 1440;
   * YDim = 720;
   * 
   * group: Data_Fields {
   * variables:
   * float ColumnAmountO3(YDim=720, XDim=1440);
   * :_FillValue = -1.2676506E30f; // float
   * :Units = "DU";
   * :Title = "Best Total Ozone Solution";
   * :UniqueFieldDefinition = "TOMS-OMI-Shared";
   * :ScaleFactor = 1.0; // double
   * :Offset = 0.0; // double
   * :ValidRange = 50.0f, 700.0f; // float
   * :MissingValue = -1.2676506E30f; // float
   * :_ChunkSize = 180, 180; // int
   * 
   * float Reflectivity331(YDim=720, XDim=1440);
   * :_FillValue = -1.2676506E30f; // float
   * :Units = "%";
   * :Title = "Effective Surface Reflectivity at 331 nm";
   * :UniqueFieldDefinition = "TOMS-OMI-Shared";
   * :ScaleFactor = 1.0; // double
   * :Offset = 0.0; // double
   * :ValidRange = -15.0f, 115.0f; // float
   * :MissingValue = -1.2676506E30f; // float
   * :_ChunkSize = 180, 180; // int
   * 
   * float UVAerosolIndex(YDim=720, XDim=1440);
   * :_FillValue = -1.2676506E30f; // float
   * :Units = "NoUnits";
   * :Title = "UV Aerosol Index";
   * :UniqueFieldDefinition = "TOMS-OMI-Shared";
   * :ScaleFactor = 1.0; // double
   * :Offset = 0.0; // double
   * :ValidRange = -30.0f, 30.0f; // float
   * :MissingValue = -1.2676506E30f; // float
   * :_ChunkSize = 180, 180; // int
   * 
   * }
   * // group attributes:
   * :GCTPProjectionCode = 0; // int
   * :Projection = "Geographic";
   * :GridOrigin = "Center";
   * :NumberOfLongitudesInGrid = 1440; // int
   * :NumberOfLatitudesInGrid = 720; // int
   * }
   * }
   * }
   */
  public void augmentDataset(CancelTask cancelTask) {
    rootGroup.findGroupNested("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES").ifPresent(nested -> {
      Attribute levelAtt = nested.getAttributeContainer().findAttribute("ProcessLevel");
      if (levelAtt == null) {
        return;
      }
      int level = levelAtt.getStringValue().startsWith("2") ? 2 : 3;
      if (level == 3) {
        augmentDataset3();
      }
    });
  }

  private void augmentDataset3() {
    Optional<Group.Builder> gridso = rootGroup.findGroupNested("/HDFEOS/GRIDS");
    gridso.ifPresent(grids -> {
      for (Group.Builder g2 : grids.gbuilders) {
        Attribute gctp = g2.getAttributeContainer().findAttribute("GCTPProjectionCode");
        if (gctp == null || !gctp.getNumericValue().equals(0)) {
          continue;
        }

        Attribute nlon = g2.getAttributeContainer().findAttribute("NumberOfLongitudesInGrid");
        Attribute nlat = g2.getAttributeContainer().findAttribute("NumberOfLatitudesInGrid");
        if (nlon == null || nlon.isString() || nlat == null || nlat.isString()) {
          continue;
        }

        datasetBuilder.replaceCoordinateAxis(g2, makeLonCoordAxis(g2, nlon.getNumericValue().intValue(), "XDim"));
        datasetBuilder.replaceCoordinateAxis(g2, makeLatCoordAxis(g2, nlat.getNumericValue().intValue(), "YDim"));

        for (Group.Builder g3 : g2.gbuilders) {
          for (Variable.Builder vb : g3.vbuilders) {
            vb.addAttribute(new Attribute(_Coordinate.Axes, "lat lon"));
          }
        }
      }
    });
  }

  private CoordinateAxis.Builder makeLatCoordAxis(Group.Builder g2, int n, String dimName) {
    CoordinateAxis.Builder v = CoordinateAxis1D.builder().setName("lat").setDataType(DataType.FLOAT)
        .setParentGroupBuilder(g2).setDimensionsByName(dimName).setUnits(CDM.LAT_UNITS).setDesc("latitude");

    double incr = 180.0 / n;
    v.setAutoGen(-90.0 + 0.5 * incr, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    return v;
  }

  private CoordinateAxis.Builder makeLonCoordAxis(Group.Builder g2, int n, String dimName) {
    CoordinateAxis.Builder v = CoordinateAxis1D.builder().setName("lon").setDataType(DataType.FLOAT)
        .setParentGroupBuilder(g2).setDimensionsByName(dimName).setUnits(CDM.LON_UNITS).setDesc("longitude");

    double incr = 360.0 / n;
    v.setAutoGen(-180.0 + 0.5 * incr, incr);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    return v;
  }
}
