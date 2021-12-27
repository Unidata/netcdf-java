/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.grib.SectionIterable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.collection.GribArrayReader;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridHorizCurvilinear;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GridGribHorizHelper {
  final GribCollectionImmutable gribCollection;
  final GdsHorizCoordSys hcs;
  final boolean isCurvilinearOrthogonal;
  final ArrayList<GribCollectionImmutable.VariableIndex> vars;
  final GridAxisPoint xaxis;
  final GridAxisPoint yaxis;
  @Nullable
  final Map<Grib2Utils.LatLonCoordType, CurvilinearCoordinates> ccMap;
  final GridHorizCoordinateSystem ghcs;

  GridGribHorizHelper(GribCollectionImmutable gribCollection, GdsHorizCoordSys hcs, boolean isCurvilinearOrthogonal,
      List<GribCollectionImmutable.VariableIndex> vars) throws InvalidRangeException, IOException {
    this.gribCollection = gribCollection;
    this.hcs = hcs;
    this.isCurvilinearOrthogonal = isCurvilinearOrthogonal;
    this.vars = new ArrayList<>(vars);

    if (isCurvilinearOrthogonal) {
      // for curvilinear these are nominal, not real
      xaxis = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName(AxisType.Lon.name()).setUnits("")
          .setDescription("fake 1d xaxis for curvilinear grid").setRegular(hcs.nx, 0.0, 1.0)
          .setSpacing(GridAxisSpacing.regularPoint).build();
      yaxis = GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName(AxisType.Lat.name()).setUnits("")
          .setDescription("fake 1d yaxis for curvilinear grid").setRegular(hcs.ny, 0.0, 1.0)
          .setSpacing(GridAxisSpacing.regularPoint).build();

    } else { // not curvilinear

      if (hcs.isLatLon()) { // latlon
        xaxis = GridAxisPoint.builder().setName(Grib.LON_AXIS).setAxisType(AxisType.Lon).setUnits(CDM.LON_UNITS)
            .setRegular(hcs.nx, hcs.startx, hcs.dx).build();

        if (hcs.hasGaussianLats()) {
          yaxis = GridAxisPoint.builder().setName(Grib.LAT_AXIS).setAxisType(AxisType.Lat).setUnits(CDM.LAT_UNITS)
              .setValues(hcs.getGaussianLatsArray()).setSpacing(GridAxisSpacing.irregularPoint)
              .addAttribute(new Attribute(CDM.GAUSSIAN, "true")).build();
        } else {
          yaxis = GridAxisPoint.builder().setName(Grib.LAT_AXIS).setAxisType(AxisType.Lat).setUnits(CDM.LAT_UNITS)
              .setRegular(hcs.ny, hcs.starty, hcs.dy).build();
        }

      } else { // regular projection coordinates
        xaxis = GridAxisPoint.builder().setName(Grib.XAXIS).setAxisType(AxisType.GeoX).setUnits("km")
            .setDescription(CF.PROJECTION_X_COORDINATE).setRegular(hcs.nx, hcs.startx, hcs.dx).build();
        yaxis = GridAxisPoint.builder().setName(Grib.YAXIS).setAxisType(AxisType.GeoY).setUnits("km")
            .setDescription(CF.PROJECTION_Y_COORDINATE).setRegular(hcs.ny, hcs.starty, hcs.dy).build();
      }
    }

    this.ccMap = identifyCurvilinearCoordinates();
    this.ghcs = new GridHorizCoordinateSystem(xaxis, yaxis, hcs.proj);

    if (this.ccMap != null) {
      for (CurvilinearCoordinates cc : this.ccMap.values()) {
        cc.makeHorizCS(xaxis, yaxis);
      }
    }
  }

  private class CurvilinearCoordinates {
    final Grib2Utils.LatLon2DCoord ll2d;
    GribCollectionImmutable.VariableIndex lat;
    GribCollectionImmutable.VariableIndex lon;
    GridHorizCurvilinear horizCS;

    CurvilinearCoordinates(Grib2Utils.LatLon2DCoord ll2d) {
      this.ll2d = ll2d;
    }

    void addCoordinate(Grib2Utils.LatLon2DCoord ll2d, GribCollectionImmutable.VariableIndex vi) {
      if (ll2d.getAxisType() == AxisType.Lat) {
        if (this.lat != null) {
          throw new IllegalStateException();
        } else {
          this.lat = vi;
        }
      } else {
        if (this.lon != null) {
          throw new IllegalStateException();
        } else {
          this.lon = vi;
        }
      }
    }

    void makeHorizCS(GridAxisPoint xaxis, GridAxisPoint yaxis) throws InvalidRangeException, IOException {
      int[] shape = new int[] {hcs.ny, hcs.nx};
      Section section = new Section(shape);
      SectionIterable want = new SectionIterable(section, shape);
      Array<Number> latdata = (Array<Number>) GribArrayReader.factory(gribCollection, lat).readData(want);
      Array<Number> londata = (Array<Number>) GribArrayReader.factory(gribCollection, lon).readData(want);

      this.horizCS = GridHorizCurvilinear.create(xaxis, yaxis, latdata, londata);
    }
  }

  private Map<Grib2Utils.LatLonCoordType, CurvilinearCoordinates> identifyCurvilinearCoordinates() {
    if (!isCurvilinearOrthogonal) {
      return null;
    }

    // identify the variables that are actually coordinates
    List<GribCollectionImmutable.VariableIndex> remove = new ArrayList<>();
    Map<Grib2Utils.LatLonCoordType, CurvilinearCoordinates> result = new HashMap<>();
    for (GribCollectionImmutable.VariableIndex vindex : this.vars) {
      Grib2Utils.LatLon2DCoord ll2d =
          Grib2Utils.getLatLon2DcoordType(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
      if (ll2d == null) {
        continue;
      }

      CurvilinearCoordinates find =
          result.computeIfAbsent(ll2d.getCoordType(), (k) -> new CurvilinearCoordinates(ll2d));
      find.addCoordinate(ll2d, vindex);
      remove.add(vindex);
    }

    // remove found coordinates
    for (GribCollectionImmutable.VariableIndex vindex : remove) {
      vars.remove(vindex);
    }

    return result;
  }

  List<GridAxis<?>> getHorizAxes() {
    return ImmutableList.of(xaxis, yaxis);
  }

  List<GribCollectionImmutable.VariableIndex> getVariables() {
    return ImmutableList.copyOf(vars);
  }

  GridHorizCoordinateSystem getHorizCs(GribCollectionImmutable.VariableIndex vindex) {
    if (ccMap == null) {
      return ghcs;
    }
    Grib2Utils.LatLonCoordType type = Grib2Utils.getLatLon2DcoordType(vindex.makeVariableDescription());
    if (type == null) {
      throw new IllegalStateException();
    }
    CurvilinearCoordinates find = ccMap.get(type);
    if (find == null) {
      throw new IllegalStateException();
    }
    return find.horizCS;
  }
}
