/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.gcdm.client.GcdmGrid;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.gcdm.client.GcdmVerticalTransform;
import ucar.nc2.AttributeContainer;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisInterval;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridHorizCurvilinear;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.nc2.internal.dataset.transform.horiz.ProjectionFactory;
import ucar.nc2.internal.grid.GridTimeCS;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Convert between GcdmGrid Protos and GridDataset objects. */
public class GcdmGridConverter {

  public static GcdmGridProto.GridDataset encodeGridDataset(GridDataset org) {
    GcdmGridProto.GridDataset.Builder builder = GcdmGridProto.GridDataset.newBuilder();
    builder.setName(org.getName());
    builder.setLocation(org.getLocation());
    builder.setFeatureType(convertFeatureType(org.getFeatureType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(org.attributes()));

    for (GridAxis<?> axis : org.getGridAxes()) {
      builder.addGridAxes(encodeGridAxis(axis));
    }

    Set<GridHorizCoordinateSystem> hsyss = new HashSet<>();
    Set<GridTimeCoordinateSystem> tsyss = new HashSet<>();
    Set<VerticalTransform> vts = new HashSet<>();
    for (GridCoordinateSystem coordsys : org.getGridCoordinateSystems()) {
      builder.addCoordSystems(encodeCoordinateSystem(coordsys));
      hsyss.add(coordsys.getHorizCoordinateSystem());
      if (coordsys.getTimeCoordinateSystem() != null) {
        tsyss.add(coordsys.getTimeCoordinateSystem());
      }
      if (coordsys.getVerticalTransform() != null) {
        vts.add(coordsys.getVerticalTransform());
      }
    }

    for (GridHorizCoordinateSystem hsys : hsyss) {
      builder.addHorizCoordSystems(encodeHorizCS(hsys));
    }
    for (GridTimeCoordinateSystem tsys : tsyss) {
      builder.addTimeCoordSystems(encodeTimeCS(tsys));
    }
    for (VerticalTransform vt : vts) {
      builder.addVerticalTransform(encodeVerticalTransform(vt));
    }
    for (Grid grid : org.getGrids()) {
      builder.addGrids(encodeGrid(grid));
    }

    return builder.build();
  }

  public static void decodeGridDataset(GcdmGridProto.GridDataset proto, GcdmGridDataset.Builder builder,
      Formatter errlog) {
    builder.setProto(proto);

    for (GcdmGridProto.GridAxis axisp : proto.getGridAxesList()) {
      builder.addGridAxis(decodeGridAxis(axisp));
    }

    Map<Integer, GridHorizCoordinateSystem> hsys = new HashMap<>();
    for (GcdmGridProto.GridHorizCoordinateSystem coordsys : proto.getHorizCoordSystemsList()) {
      GridHorizCoordinateSystem hcs;
      if (coordsys.getIsCurvilinear()) {
        hcs = decodeHorizCurvililinear(coordsys);
      } else {
        hcs = decodeHorizCS(coordsys, builder.axes, errlog);
      }
      hsys.put(coordsys.getId(), hcs);
    }

    Map<Integer, GridTimeCoordinateSystem> tsys = new HashMap<>();
    for (GcdmGridProto.GridTimeCoordinateSystem coordsys : proto.getTimeCoordSystemsList()) {
      tsys.put(coordsys.getId(), decodeTimeCS(coordsys, builder.axes));
    }

    Map<Integer, GcdmVerticalTransform> vtMap = new HashMap<>();
    for (GcdmGridProto.VerticalTransform vtp : proto.getVerticalTransformList()) {
      GcdmVerticalTransform vt = decodeVerticalTransform(vtp);
      builder.addVerticalTransform(vt);
      vtMap.put(vt.getId(), vt);
    }

    Decoder csysDecoder = new Decoder(builder.axes, tsys, hsys, vtMap);
    for (GcdmGridProto.GridCoordinateSystem coordsys : proto.getCoordSystemsList()) {
      builder.addCoordSys(csysDecoder.decodeCoordinateSystem(coordsys, errlog));
    }

    for (GcdmGridProto.Grid grid : proto.getGridsList()) {
      builder.addGrid(decodeGrid(grid));
    }
  }

  static class Decoder {
    List<GridAxis<?>> axes;
    Map<Integer, GridTimeCoordinateSystem> tsys;
    Map<Integer, GridHorizCoordinateSystem> hsys;
    Map<Integer, GcdmVerticalTransform> vts;

    Decoder(List<GridAxis<?>> axes, Map<Integer, GridTimeCoordinateSystem> tsys,
        Map<Integer, GridHorizCoordinateSystem> hsys, Map<Integer, GcdmVerticalTransform> vts) {
      this.axes = axes;
      this.tsys = tsys;
      this.hsys = hsys;
      this.vts = vts;
    }

    public GridCoordinateSystem decodeCoordinateSystem(GcdmGridProto.GridCoordinateSystem proto, Formatter errlog) {
      boolean error = false;
      ArrayList<GridAxis<?>> caxes = new ArrayList<>();

      GridHorizCoordinateSystem wantHcs = this.hsys.get(proto.getHorizCoordinatesId());
      if (wantHcs == null) {
        errlog.format("Cant find GridHorizCoordinateSystem %d for GridCoordinateSystem %s%n",
            proto.getHorizCoordinatesId(), proto.getName());
        error = true;
      }

      for (String axisName : proto.getAxisNamesList()) {
        Optional<GridAxis<?>> want = this.axes.stream().filter(a -> a.getName().equals(axisName)).findFirst();
        if (want.isEmpty()) {
          if (wantHcs.isCurvilinear() && !wantHcs.hasAxis(axisName)) {
            errlog.format("Cant find axis named %s%n", axisName);
            error = true;
          }
        } else {
          caxes.add(want.get());
        }
      }

      if (wantHcs != null && wantHcs.isCurvilinear()) {
        caxes.add(wantHcs.getYHorizAxis());
        caxes.add(wantHcs.getXHorizAxis());
      }

      GridTimeCoordinateSystem wantTcs = null;
      if (proto.getTimeCoordinatesId() != 0) {
        wantTcs = this.tsys.get(proto.getTimeCoordinatesId());
        if (wantTcs == null) {
          errlog.format("Cant find GridTimeCoordinateSystem %d for GridCoordinateSystem %s%n",
              proto.getTimeCoordinatesId(), proto.getName());
          error = true;
        }
      }

      VerticalTransform vt = null;
      if (proto.getVerticalTransformId() != 0) {
        vt = this.vts.get(proto.getVerticalTransformId());
        if (vt == null) {
          errlog.format("Cant find VerticalTransform %d for GridCoordinateSystem %s%n", proto.getVerticalTransformId(),
              proto.getName());
          error = true;
        }
      }

      if (error) {
        throw new RuntimeException(errlog.toString());
      }

      return new GridCoordinateSystem(caxes, wantTcs, vt, wantHcs);
    }
  }

  public static GcdmGridProto.GridCoordinateSystem encodeCoordinateSystem(GridCoordinateSystem csys) {
    GcdmGridProto.GridCoordinateSystem.Builder builder = GcdmGridProto.GridCoordinateSystem.newBuilder();
    builder.setName(csys.getName());
    for (GridAxis<?> axis : csys.getGridAxes()) {
      builder.addAxisNames(axis.getName());
    }
    GridHorizCoordinateSystem horizCS = csys.getHorizCoordinateSystem();
    builder.setHorizCoordinatesId(horizCS.hashCode());
    GridTimeCoordinateSystem timeCS = csys.getTimeCoordinateSystem();
    if (timeCS != null) {
      builder.setTimeCoordinatesId(timeCS.hashCode());
    }
    if (csys.getVerticalTransform() != null) {
      builder.setVerticalTransformId(csys.getVerticalTransform().hashCode());
    }
    return builder.build();
  }

  public static GcdmGridProto.GridAxis encodeGridAxis(GridAxis<?> axis) {
    GcdmGridProto.GridAxis.Builder builder = GcdmGridProto.GridAxis.newBuilder();

    builder.setName(axis.getName());
    builder.setDescription(axis.getDescription());
    builder.setUnits(axis.getUnits());
    builder.setCdmAxisType(convertAxisType(axis.getAxisType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(axis.attributes()));
    builder.setSpacing(convertAxisSpacing(axis.getSpacing()));
    builder.setDependenceType(convertAxisDependenceType(axis.getDependenceType()));
    builder.addAllDependsOn(axis.getDependsOn());

    builder.setNcoords(axis.getNominalSize());
    builder.setResolution(axis.getResolution());

    if (axis instanceof GridAxisPoint) {
      builder.setIsInterval(false);
      // sneaky way to get at the private data
      GridAxisPoint.Builder<?> axisPoint = ((GridAxisPoint) axis).toBuilder();
      builder.setStartValue(axisPoint.startValue);
      if (axisPoint.values != null) {
        for (double value : axisPoint.values) {
          builder.addValues(value);
        }
      }
      if (axisPoint.edges != null) {
        for (double value : axisPoint.edges) {
          builder.addEdges(value);
        }
      }
    }

    if (axis instanceof GridAxisInterval) {
      builder.setIsInterval(true);
      // sneaky way to get at the private data
      GridAxisInterval.Builder<?> axisInterval = ((GridAxisInterval) axis).toBuilder();
      builder.setStartValue(axisInterval.startValue);
      if (axisInterval.values != null) {
        for (double value : axisInterval.values) {
          builder.addValues(value);
        }
      }
    }
    return builder.build();
  }

  public static GridAxis<?> decodeGridAxis(GcdmGridProto.GridAxis proto) {
    boolean isInterval = proto.getIsInterval();

    if (isInterval) {
      GridAxisInterval.Builder<?> axisb = GridAxisInterval.builder();
      axisb.setName(proto.getName());
      axisb.setDescription(proto.getDescription());
      axisb.setUnits(proto.getUnits());
      axisb.setAxisType(convertAxisType(proto.getCdmAxisType()));
      axisb.setAttributes(GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));
      axisb.setSpacing(convertAxisSpacing(proto.getSpacing()));
      axisb.setDependenceType(convertAxisDependenceType(proto.getDependenceType()));
      axisb.setDependsOn(proto.getDependsOnList());
      axisb.setResolution(proto.getResolution());
      axisb.setNcoords(proto.getNcoords());
      axisb.setStartValue(proto.getStartValue());

      if (proto.getValuesCount() > 0) {
        double[] values = new double[proto.getValuesCount()];
        for (int i = 0; i < proto.getValuesCount(); i++) {
          values[i] = proto.getValues(i);
        }
        axisb.setValues(values);
      }
      return axisb.build();

    } else {

      GridAxisPoint.Builder<?> axisb = GridAxisPoint.builder();
      axisb.setName(proto.getName());
      axisb.setDescription(proto.getDescription());
      axisb.setUnits(proto.getUnits());
      axisb.setAxisType(convertAxisType(proto.getCdmAxisType()));
      axisb.setAttributes(GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList()));
      axisb.setSpacing(convertAxisSpacing(proto.getSpacing()));
      axisb.setDependenceType(convertAxisDependenceType(proto.getDependenceType()));
      axisb.setDependsOn(proto.getDependsOnList());
      axisb.setResolution(proto.getResolution());
      axisb.setNcoords(proto.getNcoords());
      axisb.setStartValue(proto.getStartValue());

      if (proto.getValuesCount() > 0) {
        double[] values = new double[proto.getValuesCount()];
        for (int i = 0; i < proto.getValuesCount(); i++) {
          values[i] = proto.getValues(i);
        }
        axisb.setValues(values);
      }
      if (proto.getEdgesCount() > 0) {
        double[] edges = new double[proto.getEdgesCount()];
        for (int i = 0; i < proto.getEdgesCount(); i++) {
          edges[i] = proto.getValues(i);
        }
        axisb.setEdges(edges);
      }
      return axisb.build();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static GcdmGridProto.GridHorizCoordinateSystem encodeHorizCS(GridHorizCoordinateSystem horizCS) {
    GcdmGridProto.GridHorizCoordinateSystem.Builder builder = GcdmGridProto.GridHorizCoordinateSystem.newBuilder();
    builder.setProjection(encodeProjection(horizCS.getProjection(), horizCS.getGeoUnits()));
    builder.setXaxisName(horizCS.getXHorizAxis().getName());
    builder.setYaxisName(horizCS.getYHorizAxis().getName());
    builder.setIsCurvilinear(horizCS.isCurvilinear());
    builder.setId(horizCS.hashCode());
    if (horizCS.isCurvilinear()) {
      encodeHorizCurvililinear((GridHorizCurvilinear) horizCS, builder);
    }
    return builder.build();
  }

  private static void encodeHorizCurvililinear(GridHorizCurvilinear horizCurvilinear,
      GcdmGridProto.GridHorizCoordinateSystem.Builder builder) {
    builder.setXaxis(encodeGridAxis(horizCurvilinear.getXHorizAxis()));
    builder.setYaxis(encodeGridAxis(horizCurvilinear.getYHorizAxis()));
    builder.setLatEdges(GcdmConverter.encodeData(ArrayType.DOUBLE, horizCurvilinear.getLatEdges()));
    builder.setLonEdges(GcdmConverter.encodeData(ArrayType.DOUBLE, horizCurvilinear.getLonEdges()));
  }

  public static GridHorizCoordinateSystem decodeHorizCS(GcdmGridProto.GridHorizCoordinateSystem horizCS,
      List<GridAxis<?>> axes, Formatter errlog) {
    GridAxisPoint xaxis = (GridAxisPoint) findAxis(horizCS.getXaxisName(), axes);
    GridAxisPoint yaxis = (GridAxisPoint) findAxis(horizCS.getYaxisName(), axes);
    Projection projection = decodeProjection(horizCS.getProjection(), errlog);
    return new GridHorizCoordinateSystem(xaxis, yaxis, projection);
  }

  public static GridHorizCoordinateSystem decodeHorizCurvililinear(
      GcdmGridProto.GridHorizCoordinateSystem horizCurvilinear) {
    GridAxisPoint xaxis = (GridAxisPoint) decodeGridAxis(horizCurvilinear.getXaxis());
    GridAxisPoint yaxis = (GridAxisPoint) decodeGridAxis(horizCurvilinear.getYaxis());
    Array<Double> latEdge = GcdmConverter.decodeData(horizCurvilinear.getLatEdges());
    Array<Double> lonEdge = GcdmConverter.decodeData(horizCurvilinear.getLonEdges());

    return GridHorizCurvilinear.createFromEdges(xaxis, yaxis, latEdge, lonEdge);
  }

  public static GcdmGridProto.GridTimeCoordinateSystem encodeTimeCS(GridTimeCoordinateSystem timeCS) {
    GcdmGridProto.GridTimeCoordinateSystem.Builder builder = GcdmGridProto.GridTimeCoordinateSystem.newBuilder();
    builder.setType(convertTimeType(timeCS.getType()));
    builder.setCalendarDateUnit(timeCS.getRuntimeDateUnit().toString());
    builder.setTimeAxisName(timeCS.getTimeOffsetAxis(0).getName()); // ??
    if (timeCS.getRunTimeAxis() != null) {
      builder.setRuntimeAxisName(timeCS.getRunTimeAxis().getName());
    }
    builder.setId(timeCS.hashCode());

    if (timeCS.getType() == GridTimeCoordinateSystem.Type.OffsetRegular) {
      GridAxisPoint runtime = timeCS.getRunTimeAxis();
      for (int runidx = 0; runidx < runtime.getNominalSize(); runidx++) {
        CalendarDate runtimeDate = timeCS.getRuntimeDate(runidx);
        int hour = runtimeDate.getHourOfDay();
        int minutes = runtimeDate.getMinuteOfHour();
        int minutesFrom0z = 60 * hour + minutes;
        System.out.printf("  %d: minutesFrom0z= %d%n", runidx, minutesFrom0z);
        builder.putRegular(minutesFrom0z, encodeGridAxis(timeCS.getTimeOffsetAxis(runidx)));
      }
    }

    if (timeCS.getType() == GridTimeCoordinateSystem.Type.OffsetIrregular) {
      for (int runidx = 0; runidx < timeCS.getRunTimeAxis().getNominalSize(); runidx++) {
        builder.addIrregular(encodeGridAxis(timeCS.getTimeOffsetAxis(runidx)));
      }
    }
    return builder.build();
  }

  public static GridTimeCoordinateSystem decodeTimeCS(GcdmGridProto.GridTimeCoordinateSystem proto,
      List<GridAxis<?>> axes) {
    GridAxis<?> timeAxis = findAxis(proto.getTimeAxisName(), axes);
    GridAxisPoint runtimeAxis = (GridAxisPoint) findAxis(proto.getRuntimeAxisName(), axes);
    CalendarDateUnit dateUnit = CalendarDateUnit.fromUdunitString(null, proto.getCalendarDateUnit()).orElseThrow();

    Map<Integer, GridAxis<?>> timeOffsetMap = new HashMap<>();
    for (Map.Entry<Integer, GcdmGridProto.GridAxis> entry : proto.getRegularMap().entrySet()) {
      timeOffsetMap.put(entry.getKey(), decodeGridAxis(entry.getValue()));
    }
    List<GridAxis<?>> timeOffsets =
        proto.getIrregularList().stream().map(a -> decodeGridAxis(a)).collect(Collectors.toList());

    return GridTimeCS.create(convertTimeType(proto.getType()), runtimeAxis, timeAxis, dateUnit, timeOffsetMap,
        timeOffsets);
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

  @Nullable
  public static Projection decodeProjection(GcdmGridProto.Projection proto, Formatter errlog) {
    AttributeContainer ctv = GcdmConverter.decodeAttributes(proto.getName(), proto.getAttributesList());
    return ProjectionFactory.makeProjection(ctv, proto.getGeoUnit(), errlog);
  }

  public static GcdmGridProto.VerticalTransform encodeVerticalTransform(VerticalTransform vt) {
    GcdmGridProto.VerticalTransform.Builder builder = GcdmGridProto.VerticalTransform.newBuilder();
    builder.setId(vt.hashCode());
    builder.setName(vt.getName());
    builder.setCtvName(vt.getCtvName());
    builder.setUnits(vt.getUnitString());
    return builder.build();
  }

  public static GcdmVerticalTransform decodeVerticalTransform(GcdmGridProto.VerticalTransform proto) {
    return new GcdmVerticalTransform(proto.getId(), proto.getName(), proto.getCtvName(), proto.getUnits());
  }

  public static GcdmGridProto.Grid encodeGrid(Grid grid) {
    GcdmGridProto.Grid.Builder builder = GcdmGridProto.Grid.newBuilder();
    builder.setName(grid.getName());
    builder.setDescription(grid.getDescription());
    builder.setUnit(grid.getUnits());
    builder.setArrayType(GcdmConverter.convertDataType(grid.getArrayType()));
    builder.addAllAttributes(GcdmConverter.encodeAttributes(grid.attributes()));
    builder.setCoordinateSystemName(grid.getCoordinateSystem().getName());
    return builder.build();
  }

  public static GcdmGrid.Builder decodeGrid(GcdmGridProto.Grid proto) {
    return GcdmGrid.builder().setProto(proto);
  }

  @Nullable
  private static GridAxis<?> findAxis(String axisName, List<GridAxis<?>> axes) {
    return axes.stream().filter(a -> a.getName().equals(axisName)).findFirst().orElse(null);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////
  // data fetching
  public static GcdmGridProto.GridReferencedArray encodeGridReferencedArray(GridReferencedArray geoArray) {
    GcdmGridProto.GridReferencedArray.Builder builder = GcdmGridProto.GridReferencedArray.newBuilder();
    builder.setGridName(geoArray.gridName());
    builder.setMaterializedCoordinateSystem(encodeMaterializedCoordSys(geoArray.getMaterializedCoordinateSystem()));
    builder.setData(GcdmConverter.encodeData(geoArray.arrayType(), geoArray.data()));
    return builder.build();
  }

  public static GridReferencedArray decodeGridReferencedArray(GcdmGridProto.GridReferencedArray proto,
      Formatter errlog) {
    MaterializedCoordinateSystem.Builder cs =
        decodeMaterializedCoordSys(proto.getMaterializedCoordinateSystem(), errlog);
    Array<Number> data = GcdmConverter.decodeData(proto.getData());
    return GridReferencedArray.create(proto.getGridName(), data.getArrayType(), data, cs.build());
  }

  public static GcdmGridProto.MaterializedCoordinateSystem encodeMaterializedCoordSys(
      MaterializedCoordinateSystem csys) {
    GcdmGridProto.MaterializedCoordinateSystem.Builder builder =
        GcdmGridProto.MaterializedCoordinateSystem.newBuilder();
    for (GridAxis<?> axis : csys.getGridAxes()) {
      builder.addAxes(encodeGridAxis(axis));
    }
    builder.setHorizCoordinateSystem(encodeHorizCS(csys.getHorizCoordinateSystem()));
    if (csys.getTimeCoordSystem() != null) {
      builder.setTimeCoordinateSystem(encodeTimeCS(csys.getTimeCoordSystem()));
    }

    return builder.build();
  }

  static MaterializedCoordinateSystem.Builder decodeMaterializedCoordSys(
      GcdmGridProto.MaterializedCoordinateSystem proto, Formatter errlog) {
    MaterializedCoordinateSystem.Builder builder = MaterializedCoordinateSystem.builder();

    ArrayList<GridAxis<?>> axes = new ArrayList<>();
    for (GcdmGridProto.GridAxis paxis : proto.getAxesList()) {
      axes.add(decodeGridAxis(paxis));
    }
    builder.setHorizCoordSys(decodeHorizCS(proto.getHorizCoordinateSystem(), axes, errlog));
    builder.setTimeCoordSys(decodeTimeCS(proto.getTimeCoordinateSystem(), axes));
    return builder;
  }


  ////////////////////////////////////////////////////////////////////////////////////
  // convert enums

  public static GridTimeCoordinateSystem.Type convertTimeType(GcdmGridProto.GridTimeType proto) {
    switch (proto) {
      case GRID_TIME_TYPE_OBSERVATION:
        return GridTimeCoordinateSystem.Type.Observation;
      case GRID_TIME_TYPE_SINGLE_RUNTIME:
        return GridTimeCoordinateSystem.Type.SingleRuntime;
      case GRID_TIME_TYPE_OFFSET:
        return GridTimeCoordinateSystem.Type.Offset;
      case GRID_TIME_TYPE_OFFSET_REGULAR:
        return GridTimeCoordinateSystem.Type.OffsetRegular;
      case GRID_TIME_TYPE_OFFSET_IRREGULAR:
        return GridTimeCoordinateSystem.Type.OffsetIrregular;
    }
    throw new IllegalArgumentException();
  }

  public static GcdmGridProto.GridTimeType convertTimeType(GridTimeCoordinateSystem.Type org) {
    switch (org) {
      case Observation:
        return GcdmGridProto.GridTimeType.GRID_TIME_TYPE_OBSERVATION;
      case SingleRuntime:
        return GcdmGridProto.GridTimeType.GRID_TIME_TYPE_SINGLE_RUNTIME;
      case Offset:
        return GcdmGridProto.GridTimeType.GRID_TIME_TYPE_OFFSET;
      case OffsetRegular:
        return GcdmGridProto.GridTimeType.GRID_TIME_TYPE_OFFSET_REGULAR;
      case OffsetIrregular:
        return GcdmGridProto.GridTimeType.GRID_TIME_TYPE_OFFSET_IRREGULAR;
    }
    throw new IllegalArgumentException();
  }

  public static FeatureType convertFeatureType(GcdmGridProto.CdmFeatureType proto) {
    switch (proto) {
      case CDM_FEATURE_TYPE_GRIDDED:
        return FeatureType.GRID;
      case CDM_FEATURE_TYPE_CURVILINEAR:
        return FeatureType.CURVILINEAR;
    }
    throw new IllegalArgumentException();
  }

  public static GcdmGridProto.CdmFeatureType convertFeatureType(FeatureType org) {
    switch (org) {
      case GRID:
        return GcdmGridProto.CdmFeatureType.CDM_FEATURE_TYPE_GRIDDED;
      case CURVILINEAR:
        return GcdmGridProto.CdmFeatureType.CDM_FEATURE_TYPE_CURVILINEAR;
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
        case CDM_AXIS_TYPE_DIMENSION: // 12
          axisType = AxisType.Dimension;
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
        case TimeOffset: // 11
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_TIME_OFFSET;
          break;
        case Dimension: // 12
          cdmAxisType = GcdmGridProto.CdmAxisType.CDM_AXIS_TYPE_DIMENSION;
          break;
        case RadialAzimuth: // 10
        case RadialDistance: // 11
        case RadialElevation: // 12
        case Spectral: // 13
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
  public static GridAxisSpacing convertAxisSpacing(GcdmGridProto.GridAxisSpacing proto) {
    GridAxisSpacing gridAxisSpacing = null;
    if (proto != null) {
      switch (proto) {
        case GRID_AXIS_SPACING_REGULAR_POINT: // 1
          gridAxisSpacing = GridAxisSpacing.regularPoint;
          break;
        case GRID_AXIS_SPACING_IRREGULAR_POINT: // 2
          gridAxisSpacing = GridAxisSpacing.irregularPoint;
          break;
        case GRID_AXIS_SPACING_NOMINAL_POINT: // 6
          gridAxisSpacing = GridAxisSpacing.nominalPoint;
          break;
        case GRID_AXIS_SPACING_REGULAR_INTERVAL: // 3
          gridAxisSpacing = GridAxisSpacing.regularInterval;
          break;
        case GRID_AXIS_SPACING_CONTIGUOUS_INTERVAL: // 4
          gridAxisSpacing = GridAxisSpacing.contiguousInterval;
          break;
        case GRID_AXIS_SPACING_DISCONTIGUOUS_INTERVAL: // 5
          gridAxisSpacing = GridAxisSpacing.discontiguousInterval;
          break;
        case GRID_AXIS_SPACING_UNSPECIFIED: // 0
          throw new UnsupportedOperationException(
              "CDM Axis Spacing is UNSPECIFIED. Cannot convert to GridAxisSpacing.");
      }
    }
    return gridAxisSpacing;
  }

  public static GcdmGridProto.GridAxisSpacing convertAxisSpacing(GridAxisSpacing spacing) {
    GcdmGridProto.GridAxisSpacing gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_UNSPECIFIED;
    if (spacing != null) {
      switch (spacing) {
        case regularPoint:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_REGULAR_POINT;
          break;
        case irregularPoint:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_IRREGULAR_POINT;
          break;
        case nominalPoint:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_NOMINAL_POINT;
          break;
        case regularInterval:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_REGULAR_INTERVAL;
          break;
        case contiguousInterval:
          gridAxisSpacing = GcdmGridProto.GridAxisSpacing.GRID_AXIS_SPACING_CONTIGUOUS_INTERVAL;
          break;
        case discontiguousInterval:
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
  public static GridAxisDependenceType convertAxisDependenceType(GcdmGridProto.GridAxisDependenceType proto) {
    GridAxisDependenceType dependenceType = null;
    if (proto != null) {
      switch (proto) {
        case GRID_AXIS_DEPENDENCE_TYPE_INDEPENDENT: // 1
          dependenceType = GridAxisDependenceType.independent;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_DEPENDENT: // 2
          dependenceType = GridAxisDependenceType.dependent;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_SCALAR: // 3
          dependenceType = GridAxisDependenceType.scalar;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_TWO_D: // 4
          dependenceType = GridAxisDependenceType.twoD;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_FMRC_REG: // 5
          dependenceType = GridAxisDependenceType.fmrcReg;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_DIMENSION: // 6
          dependenceType = GridAxisDependenceType.dimension;
          break;
        case GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED: // 0
          throw new UnsupportedOperationException(
              "Grid Axis Dependence Type is UNSPECIFIED. Cannot convert to GridAxisDependenceType");
      }
    }
    return dependenceType;
  }

  public static GcdmGridProto.GridAxisDependenceType convertAxisDependenceType(GridAxisDependenceType dtype) {
    GcdmGridProto.GridAxisDependenceType gridAxisDependenceType =
        GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_UNSPECIFIED;
    if (dtype != null) {
      switch (dtype) {
        case independent:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_INDEPENDENT;
          break;
        case dependent:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_DEPENDENT;
          break;
        case scalar:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_SCALAR;
          break;
        case twoD:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_TWO_D;
          break;
        case fmrcReg:
          gridAxisDependenceType = GcdmGridProto.GridAxisDependenceType.GRID_AXIS_DEPENDENCE_TYPE_FMRC_REG;
          break;
        case dimension:
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
}
