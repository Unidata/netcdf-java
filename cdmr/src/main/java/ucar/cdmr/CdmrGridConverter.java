package ucar.cdmr;

import ucar.cdmr.client.CdmrGrid;
import ucar.cdmr.client.CdmrGridDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.grid.*;
import ucar.nc2.internal.grid.GridCS;
import ucar.unidata.geoloc.Projection;

/** Convert between CdmrGrid Protos and GridDataset objects. */
public class CdmrGridConverter {

  public static FeatureType convertFeatureType(CdmrGridProto.FeatureType proto) {
    switch (proto) {
      case Gridded:
        return FeatureType.GRID;
    }
    throw new IllegalArgumentException();
  }

  public static AxisType convertAxisType(CdmrGridProto.AxisType proto) {
    if (proto == null) {
      return null;
    }
    return AxisType.getType(proto.name());
  }

  public static CdmrGridProto.AxisType convertAxisType(AxisType axis) {
    if (axis == null) {
      return null;
    }
    return CdmrGridProto.AxisType.valueOf(axis.name());
  }

  public static GridAxis.Spacing convertAxisSpacing(CdmrGridProto.AxisSpacing proto) {
    if (proto == null) {
      return null;
    }
    return GridAxis.Spacing.valueOf(proto.name());
  }

  public static CdmrGridProto.AxisSpacing convertAxisSpacing(GridAxis.Spacing spacing) {
    if (spacing == null) {
      return null;
    }
    return CdmrGridProto.AxisSpacing.valueOf(spacing.name());
  }

  public static GridAxis.DependenceType convertAxisDependenceType(CdmrGridProto.DependenceType proto) {
    if (proto == null) {
      return null;
    }
    return GridAxis.DependenceType.valueOf(proto.name());
  }

  public static CdmrGridProto.DependenceType convertAxisDependenceType(GridAxis.DependenceType dtype) {
    if (dtype == null) {
      return null;
    }
    return CdmrGridProto.DependenceType.valueOf(dtype.name());
  }

  public static void decodeDataset(CdmrGridProto.GridDataset proto, CdmrGridDataset.Builder builder) {
    for (CdmrGridProto.GridAxis axis : proto.getGridAxesList()) {
      builder.addGridAxis(decodeGridAxis(axis));
    }
    for (CdmrGridProto.CoordinateTransform transform : proto.getCoordTransformsList()) {
      builder.addCoordTransform(decodeCoordTransform(transform));
    }
    for (CdmrGridProto.GridCoordinateSystem coordsys : proto.getCoordSysList()) {
      builder.addCoordSys(decodeCoordSys(coordsys));
    }
    for (CdmrGridProto.Grid grid : proto.getGridsList()) {
      builder.addGrid(decodeGrid(grid));
    }
  }

  public static GridAxis.Builder<?> decodeGridAxis(CdmrGridProto.GridAxis proto) {
    CdmrGridProto.GridAxisType gridAxisType = proto.getGridAxisType();

    GridAxis.Builder<?> axisb;
    switch (gridAxisType) {
      case Axis1D:
        axisb = GridAxis1D.builder();
        break;
      case Axis1DTime:
        axisb = GridAxis1DTime.builder();
        break;
      case TimeOffserRegular:
        axisb = GridAxisOffsetTimeRegular.builder();
        break;
      default:
        throw new UnsupportedOperationException();
    }

    axisb.setName(proto.getName());
    axisb.setDescription(proto.getDescription());
    axisb.setUnits(proto.getUnits());
    axisb.setAxisType(convertAxisType(proto.getAxisType()));
    axisb.setAttributes(CdmrConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));
    axisb.setSpacing(convertAxisSpacing(proto.getSpacing()));
    axisb.setDependenceType(convertAxisDependenceType(proto.getDependenceType()));
    axisb.setDependsOn(proto.getDependsOnList());

    if (axisb instanceof GridAxis1D.Builder) {
      GridAxis1D.Builder<?> axis1b = (GridAxis1D.Builder<?>) axisb;
      axis1b.setRegular(proto.getNcoords(), proto.getStartValue(), proto.getEndValue(), proto.getResolution());

      if (proto.getValuesCount() > 0) {
        axis1b.setValues(proto.getValuesList());
      }
  }

    if (axisb instanceof GridAxis1DTime.Builder) {
      GridAxis1DTime.Builder<?> axis1b = (GridAxis1DTime.Builder<?>) axisb;
      axis1b.setDateUnits(proto.getDateUnits());
    }

    if (axisb instanceof GridAxisOffsetTimeRegular.Builder) {
      GridAxisOffsetTimeRegular.Builder<?> axisReg = (GridAxisOffsetTimeRegular.Builder<?>) axisb;
      axisReg.setRuntimeAxisName(proto.getRuntimeAxisName());
      axisReg.setHourOffsets(proto.getHourOffsetsList());
      axisReg.setMidpointsBounds(proto.getShapeList(), proto.getMidpointsList(), proto.getBoundsList());
    }

    return axisb;
  }

  public static CoordinateTransform decodeCoordTransform(CdmrGridProto.CoordinateTransform proto) {
    CoordinateTransform.Builder<?> builder;
    if (proto.getIsHoriz()) {
      builder = ProjectionCT.builder();
    } else {
      builder = VerticalCT.builder();
    }
    builder.setCtvAttributes(CdmrConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));

    // TODO remove dependence on ds. Maybe only Projections, not VerticalCT ?
    // CoordinateTransform.Builder<?> makeCoordinateTransform(NetcdfDataset ds, AttributeContainer ctv,
    //      Formatter parseInfo, Formatter errInfo)

    return builder.build();
  }

  public static GridCS.Builder<?> decodeCoordSys(CdmrGridProto.GridCoordinateSystem proto) {
    GridCS.Builder<?> builder = GridCS.builder();
    builder.setName(proto.getName());
    builder.setAxisNames(proto.getAxisNamesList());
    builder.setTransformNames(proto.getTransformNamesList());

    return builder;
  }

  public static CdmrGrid.Builder decodeGrid(CdmrGridProto.Grid proto) {
    return CdmrGrid.builder().setProto(proto);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static CdmrGridProto.GridDataset encodeDataset(GridDataset org) {
    CdmrGridProto.GridDataset.Builder builder = CdmrGridProto.GridDataset.newBuilder();
    builder.setName(org.getName());
    builder.setLocation(org.getLocation());
    builder.setFeatureType(CdmrGridProto.FeatureType.Gridded);
    builder.addAllAttributes(CdmrConverter.encodeAttributes(org.attributes()));

    for (GridAxis axis : org.getGridAxes()) {
      builder.addGridAxes(encodeGridAxis(axis));
    }

    for (CoordinateTransform transform : org.getTransforms()) {
      builder.addCoordTransforms(encodeCoordTransform(transform));
    }
    for (GridCoordinateSystem coordsys : org.getGridCoordinateSystems()) {
      builder.addCoordSys(encodeCoordSys(coordsys));
    }
    for (Grid grid : org.getGrids()) {
      builder.addGrids(encodeGrid(grid));
    }

    return builder.build();
  }

  public static CdmrGridProto.GridCoordinateSystem encodeCoordSys(GridCoordinateSystem csys) {
    CdmrGridProto.GridCoordinateSystem.Builder builder = CdmrGridProto.GridCoordinateSystem.newBuilder();
    builder.setName(csys.getName());
    for (GridAxis axis : csys.getGridAxes()) {
      builder.addAxisNames(axis.getName());
    }

    Projection proj = csys.getHorizCoordSystem().getProjection();
    if (proj != null) {
      builder.addTransformNames(proj.getName());
    }

    return builder.build();
  }

  public static CdmrGridProto.CoordinateTransform encodeCoordTransform(CoordinateTransform transform) {
    CdmrGridProto.CoordinateTransform.Builder builder = CdmrGridProto.CoordinateTransform.newBuilder();
    builder.setName(transform.getName());
    builder.setIsHoriz(transform.getTransformType() == TransformType.Projection);
    builder.addAllAttributes(CdmrConverter.encodeAttributes(transform.getCtvAttributes()));

    return builder.build();
  }

  public static CdmrGridProto.GridAxis encodeGridAxis(GridAxis axis) {
    CdmrGridProto.GridAxis.Builder builder = CdmrGridProto.GridAxis.newBuilder();

    builder.setName(axis.getName());
    builder.setDescription(axis.getDescription());
    builder.setUnits(axis.getUnits());
    builder.setAxisType(convertAxisType(axis.getAxisType()));
    builder.addAllAttributes(CdmrConverter.encodeAttributes(axis.attributes()));
    builder.setSpacing(convertAxisSpacing(axis.getSpacing()));
    builder.setDependenceType(convertAxisDependenceType(axis.getDependenceType()));
    builder.addAllDependsOn(axis.getDependsOn());

    if (axis instanceof GridAxis1D) {
      GridAxis1D axis1 = (GridAxis1D) axis;
      builder.setNcoords(axis1.getNcoords());
      builder.setStartValue(axis1.getStartValue());
      builder.setEndValue(axis1.getEndValue());
      builder.setResolution(axis1.getResolution());

      double[] values = axis1.getValues();
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          builder.addValues(values[i]);
        }
      }
    }

    if (axis instanceof GridAxis1DTime) {
      GridAxis1DTime axis1t = (GridAxis1DTime) axis;
      builder.setDateUnits(axis1t.getTimeHelper().getUdUnit());
    }

    if (axis instanceof GridAxisOffsetTimeRegular) {
      GridAxisOffsetTimeRegular axisReg = (GridAxisOffsetTimeRegular) axis;
      builder.setRuntimeAxisName(axisReg.getRunTimeAxis().getName());
      for (int hour : axisReg.getHourOffsets()) {
        builder.addHourOffsets(hour);
      }
      for (double mid : axisReg.getCoordsAsArray()) {
        builder.addMidpoints(mid);
      }
      for (double bound : axisReg.getCoordBoundsAsArray()) {
        builder.addBounds(bound);
      }
      for (int shape : axisReg.getCoordsAsArray().getShape()) {
        builder.addShape(shape);
      }
    }

    return builder.build();
  }

  public static CdmrGridProto.Grid encodeGrid(Grid grid) {
    CdmrGridProto.Grid.Builder builder = CdmrGridProto.Grid.newBuilder();
    builder.setName(grid.getName());
    builder.setDescription(grid.getDescription());
    builder.setUnits(grid.getUnits());
    builder.setDataType(CdmrConverter.convertDataType(grid.getDataType()));
    builder.addAllAttributes(CdmrConverter.encodeAttributes(grid.attributes()));
    builder.setCoordSys(grid.getCoordinateSystem().getName());
    builder.setHasMissing(grid.hasMissing());

    return builder.build();
  }

}
