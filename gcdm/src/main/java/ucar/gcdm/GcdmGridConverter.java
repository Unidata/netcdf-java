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

import java.util.Formatter;

/** Convert between GcdmGrid Protos and GridDataset objects. */
public class GcdmGridConverter {

  public static FeatureType convertFeatureType(GcdmGridProto.GridDataset.FeatureType proto) {
    switch (proto) {
      case Gridded:
        return FeatureType.GRID;
    }
    throw new IllegalArgumentException();
  }

  public static AxisType convertAxisType(GcdmGridProto.GridAxis.AxisType proto) {
    if (proto == null) {
      return null;
    }
    return AxisType.getType(proto.name());
  }

  public static GcdmGridProto.GridAxis.AxisType convertAxisType(AxisType axis) {
    if (axis == null) {
      return null;
    }
    return GcdmGridProto.GridAxis.AxisType.valueOf(axis.name());
  }

  public static GridAxis.Spacing convertAxisSpacing(GcdmGridProto.GridAxis.AxisSpacing proto) {
    if (proto == null) {
      return null;
    }
    return GridAxis.Spacing.valueOf(proto.name());
  }

  public static GcdmGridProto.GridAxis.AxisSpacing convertAxisSpacing(GridAxis.Spacing spacing) {
    if (spacing == null) {
      return null;
    }
    return GcdmGridProto.GridAxis.AxisSpacing.valueOf(spacing.name());
  }

  public static GridAxis.DependenceType convertAxisDependenceType(GcdmGridProto.GridAxis.DependenceType proto) {
    if (proto == null) {
      return null;
    }
    return GridAxis.DependenceType.valueOf(proto.name());
  }

  public static GcdmGridProto.GridAxis.DependenceType convertAxisDependenceType(GridAxis.DependenceType dtype) {
    if (dtype == null) {
      return null;
    }
    return GcdmGridProto.GridAxis.DependenceType.valueOf(dtype.name());
  }

  public static void decodeDataset(GcdmGridProto.GridDataset proto, GcdmGridDataset.Builder builder, Formatter errlog) {
    for (GcdmGridProto.GridAxis axis : proto.getGridAxesList()) {
      builder.addGridAxis(decodeGridAxis(axis));
    }
    for (GcdmGridProto.GridCoordinateSystem coordsys : proto.getCoordSysList()) {
      builder.addCoordSys(decodeCoordSys(coordsys, errlog));
    }
    for (GcdmGridProto.Grid grid : proto.getGridsList()) {
      builder.addGrid(decodeGrid(grid));
    }
  }

  public static GridAxis.Builder<?> decodeGridAxis(GcdmGridProto.GridAxis proto) {
    GcdmGridProto.GridAxis.GridAxisType gridAxisType = proto.getGridAxisType();

    GridAxis.Builder<?> axisb;
    switch (gridAxisType) {
      case Axis1D:
        axisb = GridAxis1D.builder();
        break;
      case Axis1DTime:
        axisb = GridAxis1DTime.builder();
        break;
      case TimeOffsetRegular:
        axisb = GridAxisOffsetTimeRegular.builder();
        break;
      default:
        throw new UnsupportedOperationException();
    }

    axisb.setName(proto.getName());
    axisb.setDescription(proto.getDescription());
    axisb.setUnits(proto.getUnits());
    axisb.setAxisType(convertAxisType(proto.getAxisType()));
    axisb.setAttributes(GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));
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
    return ucar.nc2.internal.dataset.transform.horiz.ProjectionFactory.makeProjection(ctv, proto.getGeoUnits(), errlog);
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
    builder.setFeatureType(GcdmGridProto.GridDataset.FeatureType.Gridded);
    builder.addAllAttributes(GcdmConverter.encodeAttributes(org.attributes()));

    for (GridAxis axis : org.getGridAxes()) {
      builder.addGridAxes(encodeGridAxis(axis));
    }
    for (GridCoordinateSystem coordsys : org.getGridCoordinateSystems()) {
      builder.addCoordSys(encodeCoordSys(coordsys));
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
      builder.setGeoUnits(geoUnits);
    }
    builder.addAllAttributes(GcdmConverter.encodeAttributes(projection.getProjectionAttributes()));

    return builder.build();
  }

  public static GcdmGridProto.GridAxis encodeGridAxis(GridAxis axis) {
    GcdmGridProto.GridAxis.Builder builder = GcdmGridProto.GridAxis.newBuilder();

    if (axis instanceof GridAxis1DTime) {
      builder.setGridAxisType(GcdmGridProto.GridAxis.GridAxisType.Axis1DTime);
    } else if (axis instanceof GridAxisOffsetTimeRegular) {
      builder.setGridAxisType(GcdmGridProto.GridAxis.GridAxisType.TimeOffsetRegular);
    } else if (axis instanceof GridAxis2D) {
      builder.setGridAxisType(GcdmGridProto.GridAxis.GridAxisType.Axis2D);
    } else {
      builder.setGridAxisType(GcdmGridProto.GridAxis.GridAxisType.Axis1D);
    }

    builder.setName(axis.getName());
    builder.setDescription(axis.getDescription());
    builder.setUnits(axis.getUnits());
    builder.setAxisType(convertAxisType(axis.getAxisType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(axis.attributes()));
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
        for (double value : values) {
          builder.addValues(value);
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

  public static GcdmGridProto.Grid encodeGrid(Grid grid) {
    GcdmGridProto.Grid.Builder builder = GcdmGridProto.Grid.newBuilder();
    builder.setName(grid.getName());
    builder.setDescription(grid.getDescription());
    builder.setUnits(grid.getUnits());
    builder.setDataType(GcdmConverter.convertDataType(grid.getArrayType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(grid.attributes()));
    builder.setCoordSys(grid.getCoordinateSystem().getName());
    builder.setHasMissing(grid.hasMissing());

    return builder.build();
  }

  public static GcdmGridProto.GridReferencedArray encodeGridReferencedArray(GridReferencedArray geoArray) {
    GcdmGridProto.GridReferencedArray.Builder builder = GcdmGridProto.GridReferencedArray.newBuilder();
    builder.setGridName(geoArray.gridName());
    builder.setCsSubset(encodeCoordSys(geoArray.csSubset()));
    builder.setData(GcdmConverter.encodeData(geoArray.arrayType(), geoArray.data()));
    return builder.build();
  }

}
