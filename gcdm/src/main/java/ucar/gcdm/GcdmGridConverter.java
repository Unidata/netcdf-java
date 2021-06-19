package ucar.gcdm;

import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.gcdm.client.GcdmGrid;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.grid.*;
import ucar.nc2.internal.grid.GridCS;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;
import java.util.Formatter;

/** Convert between GcdmGrid Protos and GridDataset objects. */
public class GcdmGridConverter {

  public static FeatureType convertFeatureType(GcdmGridProto.CdmFeatureType proto) {
    switch (proto) {
      case CDM_FEATURE_TYPE_GRIDDED:
        return FeatureType.GRID;
    }
    throw new IllegalArgumentException();
  }

  @Nullable
  public static AxisType convertAxisType(GcdmGridProto.CdmAxisType proto) {
    AxisType axisType = null;
    if (proto != null) {
      switch (proto) {
        case CDM_AXIS_TYPE_RUN_TIME: // 1
          axisType = AxisType.RunTime;
          break;
        case CDM_AXIS_TYPE_ENSEMBLE:
          axisType = AxisType.Ensemble; // 2
          break;
        case CDM_AXIS_TYPE_TIME: // 3
          axisType = AxisType.Time;
          break;
        case CDM_AXIS_TYPE_GEO_X: // 4
          axisType = AxisType.GeoX;
          break;
        case CDM_AXIS_TYPE_GEO_Y: // 5
          axisType = AxisType.GeoY;
          break;
        case CDM_AXIS_TYPE_GEO_Z: // 6
          axisType = AxisType.GeoZ;
          break;
        case CDM_AXIS_TYPE_LAT: // 7
          axisType = AxisType.Lat;
          break;
        case CDM_AXIS_TYPE_LON: // 8
          axisType = AxisType.Lon;
          break;
        case CDM_AXIS_TYPE_HEIGHT: // 9
          axisType = AxisType.Height;
          break;
        case CDM_AXIS_TYPE_PRESSURE: // 10
          axisType = AxisType.Pressure;
          break;
        case CDM_AXIS_TYPE_TIME_OFFSET: // 11
          axisType = AxisType.TimeOffset;
          break;
        case CDM_AXIS_TYPE_UNSPECIFIED: // 0
          throw new UnsupportedOperationException("CDM Axis Type is UNSPECIFIED. Cannot convert to AxisType.");
        default:
          // I am not sure we'll get here if the enum value is not defined in the version of the proto files used
          // to generate the stub code. but, just in case...
          throw new UnsupportedOperationException("CdmAxisType not understood.");
      }
    }
    return axisType;
  }

  public static GcdmGridProto.CdmAxisType convertAxisType(AxisType axis) {
    GcdmGridProto.CdmAxisType cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_UNSPECIFIED;
    if (axis != null) {
      switch (axis) {
        case RunTime: // 0
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_RUN_TIME;
          break;
        case Ensemble: // 1
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_ENSEMBLE;
          break;
        case Time: // 2
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_TIME;
          break;
        case GeoX: // 3
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_GEO_X;
          break;
        case GeoY: // 4
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_GEO_Y;
          break;
        case GeoZ: // 5
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_GEO_Z;
          break;
        case Lat: // 6
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_LAT;
          break;
        case Lon: // 7
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_LON;
          break;
        case Height: // 8
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_HEIGHT;
          break;
        case Pressure: // 9
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_PRESSURE;
          break;
        case TimeOffset: // 14
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_TIME_OFFSET;
          break;
        case RadialAzimuth: // 10
        case RadialDistance: // 11
        case RadialElevation: // 12
        case Spectral: // 13
        case Dimension: // 15
        case SimpleGeometryX: // 16
        case SimpleGeometryY: // 17
        case SimpleGeometryZ: // 18
        case SimpleGeometryID: // 19
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_UNSPECIFIED;
          break;
        default:
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_UNSPECIFIED;
          break;
      }
    }
    // todo: LOOK - should we catch case of CDM_AXIS_TYPE_UNSPECIFIED and throw
    // an error to prevent sending a bad message?
    return cdmAxisType;
  }

  @Nullable
  public static GridAxis.Spacing convertAxisSpacing(GcdmGridProto.GridAxisSpacing proto) {
    GridAxis.Spacing gridAxisSpacing = null;
    if (proto != null) {
      switch (proto) {
        case GRID_AXIS_SPACING_REGULAR_POINT: // 1
          gridAxisSpacing = GridAxis.Spacing.regularPoint;
          break;
        case GRID_AXIS_SPACING_IRREGULAR_POINT: // 2
          gridAxisSpacing = GridAxis.Spacing.irregularPoint;
          break;
        case GRID_AXIS_SPACING_REGULAR_INTERVAL: // 3
          gridAxisSpacing = GridAxis.Spacing.regularInterval;
          break;
        case GRID_AXIS_SPACING_CONTIGUOUS_INTERVAL: // 4
          gridAxisSpacing = GridAxis.Spacing.contiguousInterval;
          break;
        case GRID_AXIS_SPACING_DISCONTIGUOUS_INTERVAL: // 5
          gridAxisSpacing = GridAxis.Spacing.discontiguousInterval;
          break;
        case GRID_AXIS_SPACING_UNSPECIFIED: // 0
          throw new UnsupportedOperationException(
              "CDM Axis Spacing is UNSPECIFIED. Cannot convert to GridAxis.Spacing.");
      }
    }
    return gridAxisSpacing;
  }

  public static GcdmGridProto.GridAxisSpacing convertAxisSpacing(GridAxis.Spacing spacing) {
    GcdmGridProto.GridAxisSpacing gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_UNSPECIFIED;
    if (spacing != null) {
      switch (spacing) {
        case regularPoint: // 0
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_REGULAR_POINT;
          break;
        case irregularPoint: // 1
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_IRREGULAR_POINT;
          break;
        case regularInterval: // 2
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_REGULAR_INTERVAL;
          break;
        case contiguousInterval: // 3
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_CONTIGUOUS_INTERVAL;
          break;
        case discontiguousInterval: // 4
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_DISCONTIGUOUS_INTERVAL;
          break;
        default:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_UNSPECIFIED;
          break;
      }
    }
    // todo: LOOK - should we catch case of GcdmGridProto.GridAxisSpacing.CDM_AXIS_SPACING_UNSPECIFIED and throw
    // an error to prevent sending a bad message?
    return gridAxisSpacing;
  }

  @Nullable
  public static GridAxis.DependenceType convertAxisDependenceType(GcdmGridProto.GridAxisDependenceType proto) {
    GridAxis.DependenceType dependenceType = null;
    if (proto != null) {
      switch (proto) {
        case GRID_AXIS_DEPENDENCE_TYPE_INDEPENDENT: // 1
          dependenceType = GridAxis.DependenceType.independent;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_DEPENDENT: // 2
          dependenceType = GridAxis.DependenceType.dependent;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_SCALAR: // 3
          dependenceType = GridAxis.DependenceType.scalar;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_TWO_D: // 4
          dependenceType = GridAxis.DependenceType.twoD;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_FMRC_REG: // 5
          dependenceType = GridAxis.DependenceType.fmrcReg;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_DIMENSION: // 6
          dependenceType = GridAxis.DependenceType.dimension;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED: // 0
          throw new UnsupportedOperationException(
              "Grid Axis Dependence Type is UNSPECIFIED. Cannot convert to GridAxis.DependenceType");
      }
    }
    return dependenceType;
  }

  public static GcdmGridProto.GridAxisDependenceType convertAxisDependenceType(GridAxis.DependenceType dtype) {
    GcdmGridProto.GridAxisDependenceType gridAxisDependenceType =
        GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED;
    if (dtype != null) {
      switch (dtype) {
        case independent: // 0
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_INDEPENDENT;
          break;
        case dependent: // 1
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_DEPENDENT;
          break;
        case scalar: // 2
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_SCALAR;
          break;
        case twoD: // 3
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_TWO_D;
          break;
        case fmrcReg: // 4
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_FMRC_REG;
          break;
        case dimension: // 5
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_DIMENSION;
          break;
        default:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED;
          break;
      }
    }
    // todo: LOOK - should we catch case of GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED and throw
    // an error to prevent sending a bad message?
    return gridAxisDependenceType;
  }

  public static void decodeDataset(GcdmGridProto.GridDataset proto, GcdmGridDataset.Builder builder, Formatter errlog) {
    for (GcdmGridProto.GridAxis axis : proto.getGridAxesList()) {
      builder.addGridAxis(decodeGridAxis(axis));
    }
    for (GcdmGridProto.GridCoordinateSystem coordsys : proto.getCoordSystemsList()) {
      builder.addCoordSys(decodeCoordSys(coordsys, errlog));
    }
    for (GcdmGridProto.Grid grid : proto.getGridsList()) {
      builder.addGrid(decodeGrid(grid));
    }
  }

  public static GridAxis.Builder<?> decodeGridAxis(GcdmGridProto.GridAxis proto) {
    GcdmGridProto.GridAxisType gridAxisType = proto.getGridAxisType();

    GridAxis.Builder<?> axisb;
    switch (gridAxisType) {
      case GRID_AXIS_TYPE_AXIS_1D: // 1
        axisb = GridAxis1D.builder();
        break;
      case GRID_AXIS_TYPE_AXIS_1D_TIME: // 2
        axisb = GridAxis1DTime.builder();
        break;
      case GRID_AXIS_TYPE_TIME_OFFSET_REGULAR: // 3
        axisb = GridAxisOffsetTimeRegular.builder();
        break;
      case GRID_AXIS_TYPE_UNSPECIFIED: // 0
        throw new UnsupportedOperationException("Grid Axis Type is UNSPECIFIED. Cannot decode.");
      default:
        throw new UnsupportedOperationException();
    }

    axisb.setName(proto.getName());
    axisb.setDescription(proto.getDescription());
    axisb.setUnits(proto.getUnit());
    axisb.setAxisType(convertAxisType(proto.getCdmAxisType()));
    axisb.setAttributes(GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));
    axisb.setSpacing(convertAxisSpacing(proto.getSpacing()));
    axisb.setDependenceType(convertAxisDependenceType(proto.getDependenceType()));
    axisb.setDependsOn(proto.getDependsOnList());

    if (axisb instanceof GridAxis1D.Builder) {
      GridAxis1D.Builder<?> axis1b = (GridAxis1D.Builder<?>) axisb;
      axis1b.setRegular(proto.getNcoord(), proto.getStartValue(), proto.getEndValue(), proto.getResolution());

      if (proto.getValuesCount() > 0) {
        axis1b.setValues(proto.getValuesList());
      }
    }

    if (axisb instanceof GridAxis1DTime.Builder) {
      GridAxis1DTime.Builder<?> axis1b = (GridAxis1DTime.Builder<?>) axisb;
      axis1b.setDateUnits(proto.getDateUnit());
    }

    if (axisb instanceof GridAxisOffsetTimeRegular.Builder) {
      GridAxisOffsetTimeRegular.Builder<?> axisReg = (GridAxisOffsetTimeRegular.Builder<?>) axisb;
      axisReg.setRuntimeAxisName(proto.getRuntimeAxisName());
      axisReg.setMinutesOffsets(proto.getHourOffsetsList());
      axisReg.setMidpointsBounds(proto.getShapesList(), proto.getMidpointsList(), proto.getBoundsList());
    }

    return axisb;
  }

  public static GridCS.Builder<?> decodeCoordSys(GcdmGridProto.GridCoordinateSystem proto, Formatter errlog) {
    GridCS.Builder<?> builder = GridCS.builder();
    builder.setName(proto.getName());
    builder.setFeatureType(FeatureType.GRID);
    builder.setAxisNames(proto.getAxisNamesList());
    builder.setProjection(decodeProjection(proto.getProjection(), errlog));

    return builder;
  }

  public static GcdmGrid.Builder decodeGrid(GcdmGridProto.Grid proto) {
    return GcdmGrid.builder().setProto(proto);
  }

  public static Projection decodeProjection(GcdmGridProto.Projection proto, Formatter errlog) {
    AttributeContainer ctv = GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList());
    return ucar.nc2.internal.dataset.transform.horiz.ProjectionFactory.makeProjection(ctv, proto.getGeoUnit(), errlog);
  }

  public static GridReferencedArray decodeGridReferencedArray(GcdmGridProto.GridReferencedArray proto,
      ImmutableList<GridAxis> axes) {
    Formatter errlog = new Formatter();
    GridCS.Builder<?> cs = decodeCoordSys(proto.getCsSubset(), errlog);
    Array<Number> data = GcdmConverter.decodeData(proto.getData());
    return GridReferencedArray.create(proto.getGridName(), data.getArrayType(), data, cs.build(axes));
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static GcdmGridProto.GridDataset encodeDataset(GridDataset org) {
    GcdmGridProto.GridDataset.Builder builder = GcdmGridProto.GridDataset.newBuilder();
    builder.setName(org.getName());
    builder.setLocation(org.getLocation());
    builder.setFeatureType(GcdmGridProto.CdmFeatureType.CDM_FEATURE_TYPE_GRIDDED);
    builder.addAllAttributes(GcdmConverter.encodeAttributes(org.attributes()));

    for (GridAxis axis : org.getGridAxes()) {
      builder.addGridAxes(encodeGridAxis(axis));
    }
    for (GridCoordinateSystem coordsys : org.getGridCoordinateSystems()) {
      builder.addCoordSystems(encodeCoordSys(coordsys));
    }
    for (Grid grid : org.getGrids()) {
      builder.addGrids(encodeGrid(grid));
    }

    return builder.build();
  }

  public static GcdmGridProto.GridCoordinateSystem encodeCoordSys(GridCoordinateSystem csys) {
    GcdmGridProto.GridCoordinateSystem.Builder builder = GcdmGridProto.GridCoordinateSystem.newBuilder();
    builder.setName(csys.getName());
    for (GridAxis axis : csys.getGridAxes()) {
      builder.addAxisNames(axis.getName());
    }
    GridHorizCoordinateSystem horizCS = csys.getHorizCoordSystem();
    builder.setProjection(encodeProjection(horizCS.getProjection(), horizCS.getGeoUnits()));

    return builder.build();
  }

  public static GcdmGridProto.Projection encodeProjection(Projection projection, String geoUnits) {
    GcdmGridProto.Projection.Builder builder = GcdmGridProto.Projection.newBuilder();
    builder.setName(projection.getName());
    if (geoUnits != null) {
      builder.setGeoUnit(geoUnits);
    }
    builder.addAllAttributes(GcdmConverter.encodeAttributes(projection.getProjectionAttributes()));

    return builder.build();
  }

  public static GcdmGridProto.GridAxis encodeGridAxis(GridAxis axis) {
    GcdmGridProto.GridAxis.Builder builder = GcdmGridProto.GridAxis.newBuilder();

    if (axis instanceof GridAxis1DTime) {
      builder.setGridAxisType(GcdmGridProto.GridAxisType.GRID_AXIS_TYPE_AXIS_1D_TIME);
    } else if (axis instanceof GridAxisOffsetTimeRegular) {
      builder.setGridAxisType(GcdmGridProto.GridAxisType.GRID_AXIS_TYPE_TIME_OFFSET_REGULAR);
    } else if (axis instanceof GridAxis2D) {
      builder.setGridAxisType(GcdmGridProto.GridAxisType.GRID_AXIS_TYPE_AXIS_2D);
    } else {
      builder.setGridAxisType(GcdmGridProto.GridAxisType.GRID_AXIS_TYPE_AXIS_1D);
    }

    builder.setName(axis.getName());
    builder.setDescription(axis.getDescription());
    builder.setUnit(axis.getUnits());
    builder.setCdmAxisType(convertAxisType(axis.getAxisType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(axis.attributes()));
    builder.setSpacing(convertAxisSpacing(axis.getSpacing()));
    builder.setDependenceType(convertAxisDependenceType(axis.getDependenceType()));
    builder.addAllDependsOn(axis.getDependsOn());

    if (axis instanceof GridAxis1D) {
      GridAxis1D axis1 = (GridAxis1D) axis;
      builder.setNcoord(axis1.getNcoords());
      builder.setStartValue(axis1.getStartValue());
      builder.setEndValue(axis1.getEndValue());
      builder.setResolution(axis1.getResolution());

      double[] values = axis1.getValues();
      if (values != null) {
        for (double value : values) {
          builder.addValues(value);
        }
      }
    }

    if (axis instanceof GridAxis1DTime) {
      GridAxis1DTime axis1t = (GridAxis1DTime) axis;
      builder.setDateUnit(axis1t.getTimeHelper().getUdUnit());
    }

    if (axis instanceof GridAxisOffsetTimeRegular) {
      GridAxisOffsetTimeRegular axisReg = (GridAxisOffsetTimeRegular) axis;
      builder.setRuntimeAxisName(axisReg.getRunTimeAxis().getName());
      for (int hour : axisReg.getMinuteOffsets()) {
        builder.addHourOffsets(hour);
      }
      for (double mid : axisReg.getCoordsAsArray()) {
        builder.addMidpoints(mid);
      }
      for (double bound : axisReg.getCoordBoundsAsArray()) {
        builder.addBounds(bound);
      }
      for (int shape : axisReg.getCoordsAsArray().getShape()) {
        builder.addShapes(shape);
      }
    }

    return builder.build();
  }

  public static GcdmGridProto.Grid encodeGrid(Grid grid) {
    GcdmGridProto.Grid.Builder builder = GcdmGridProto.Grid.newBuilder();
    builder.setName(grid.getName());
    builder.setDescription(grid.getDescription());
    builder.setUnit(grid.getUnits());
    builder.setDataType(GcdmConverter.convertDataType(grid.getArrayType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(grid.attributes()));
    builder.setCoordSystem(grid.getCoordinateSystem().getName());
    builder.setHasMissing(grid.hasMissing());

    return builder.build();
  }

  public static GcdmGridProto.GridReferencedArray encodeGridReferencedArray(GridReferencedArray geoArray) {
    GcdmGridProto.GridReferencedArray.Builder builder = GcdmGridProto.GridReferencedArray.newBuilder();
    builder.setGridName(geoArray.gridName());
    builder.setCsSubset(encodeMaterializedCoordSys(geoArray.getMaterializedCoordinateSystem()));
    builder.setData(GcdmConverter.encodeData(geoArray.arrayType(), geoArray.data()));
    return builder.build();
  }

  public static GcdmGridProto.GridCoordinateSystem encodeMaterializedCoordSys(MaterializedCoordinateSystem csys) {
    GcdmGridProto.GridCoordinateSystem.Builder builder = GcdmGridProto.GridCoordinateSystem.newBuilder();
    builder.setName(csys.getName());
    for (GridAxis axis : csys.getGridAxes()) {
      builder.addAxisNames(axis.getName());
    }
    GridHorizCoordinateSystem horizCS = csys.getHorizCoordSystem();
    builder.setProjection(encodeProjection(horizCS.getProjection(), horizCS.getGeoUnits()));

    return builder.build();
  }


}
