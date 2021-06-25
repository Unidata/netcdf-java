/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.unidata.io.RandomAccessFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GribGridDataset implements GridDataset {
  private static final Logger logger = LoggerFactory.getLogger(GribGridDataset.class);

  /**
   * Open GribCollection as a GridDataset.
   *
   * @param errLog if is grib but error, add error message to this log.
   * @return empty if not a GribCollection or on error.
   *         LOOK optional prevents being in a try-with-resource
   */
  public static Optional<GribGridDataset> open(String endpoint, Formatter errLog) throws IOException {
    GribCollectionImmutable gc;

    if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    // try to fail fast
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(endpoint, "r");
      // LOOK how do you pass in a non-standard FeatureCollectionConfig ? Or is that only when you are creating?
      gc = GribCdmIndex.openGribCollectionFromRaf(raf, new FeatureCollectionConfig(), CollectionUpdateType.nocheck,
          logger);

      if (gc == null) {
        raf.close();
        return Optional.empty();
      }

      /*
       * LOOK here is the issue of multiple groups. How to handle? FeatureDatasetCoverage had baked in multiple
       * // LOOK FeatureCollection ==> GridDataset.
       * List<GribGridDataset> datasets = new ArrayList<>();
       * for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
       * for (GribCollectionImmutable.GroupGC group : ds.getGroups()) {
       * GribGridDataset gribCov = new GribGridDataset(gc, ds, group);
       * datasets.add(gribCov);
       * }
       * }
       */
      GribGridDataset result = new GribGridDataset(gc, null, null);
      return Optional.of(result);

    } catch (IOException ioe) {
      throw ioe; // propagate up
    } catch (Throwable t) {
      logger.error("GribCoverageDataset.open failed", t);
      errLog.format("%s", t.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.Dataset dataset;
  private final GribCollectionImmutable.GroupGC group;
  private final GribGridHorizCoordinateSystem horizCoordinateSystem;
  private final Map<Integer, GridAxis> gridAxes; // <index, GridAxis>
  private final Map<Integer, GribGridCoordinateSystem> gridCoordinateSystems; // <hash, GribGridCoordinateSystem>
  private final Map<Integer, GribGridTimeCoordinateSystem> timeCoordinateSystems; // <hash,
                                                                                  // GribGridTimeCoordinateSystem>
  private final List<GribGrid> grids;

  private final boolean isLatLon;
  private final boolean isCurvilinearOrthogonal;

  public GribGridDataset(GribCollectionImmutable gribCollection, @Nullable GribCollectionImmutable.Dataset dataset,
      @Nullable GribCollectionImmutable.GroupGC group) {
    Preconditions.checkNotNull(gribCollection);
    this.gribCollection = gribCollection;
    this.dataset = (dataset != null) ? dataset : gribCollection.getDataset(0);
    Preconditions.checkNotNull(this.dataset);
    this.group = (group != null) ? group : this.dataset.getGroup(0);
    Preconditions.checkNotNull(this.group);
    boolean isGrib1 = gribCollection.isGrib1;

    // A GribGridDataset has a unique GridHorizCoordinateSystem LOOK true?
    GdsHorizCoordSys hcs = this.group.getGdsHorizCoordSys();
    this.isLatLon = hcs.isLatLon();
    this.isCurvilinearOrthogonal =
        !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());
    this.horizCoordinateSystem = new GribGridHorizCoordinateSystem(hcs);

    // Each Coordinate becomes a GridAxis
    HashMap<Integer, CoordAndAxis> coordIndexMap = new HashMap<>(); // <index, CoordAndAxis>
    this.gridAxes = new HashMap<>(); // <index, GridAxis>
    int coordIndex = 0;
    for (Coordinate coord : this.group.getCoordinates()) {
      CoordAndAxis coordAndAxis = GribGridAxis.create(this.dataset.getType(), coord);
      gridAxes.put(coordIndex, coordAndAxis.axis);
      coordIndexMap.put(coordIndex, coordAndAxis);
      coordIndex++;
    }

    // Each unique index list in the VariableIndex becomes a coordinate system
    HashMap<Integer, Iterable<Integer>> uniqueIndexList = new HashMap<>(); // <hash, List<index>>
    for (GribCollectionImmutable.VariableIndex vi : this.group.getVariables()) {
      int hash = makeHash(vi.getCoordinateIndex());
      uniqueIndexList.put(hash, vi.getCoordinateIndex());
    }
    this.timeCoordinateSystems = new HashMap<>(); // <hash, GribGridTimeCoordinateSystem>
    this.gridCoordinateSystems = new HashMap<>(); // <hash, GribGridCoordinateSystem>
    for (Iterable<Integer> indexList : uniqueIndexList.values()) {
      int hash = makeHash(indexList);
      this.gridCoordinateSystems.put(hash, makeCoordinateSystem(indexList, coordIndexMap));
    }

    // Each VariableIndex becomes a grid
    this.grids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex vi : this.group.getVariables()) {
      GribGridCoordinateSystem ggcs = this.gridCoordinateSystems.get(makeHash(vi.getCoordinateIndex()));
      this.grids.add(new GribGrid(ggcs, this.gribCollection, vi));
    }
  }

  static class CoordAndAxis {
    Coordinate coord;
    GridAxis axis;
    CoordinateTime2D time2d;

    CoordAndAxis(Coordinate coord, GridAxis axis) {
      this.coord = coord;
      this.axis = axis;
    }

    CoordAndAxis withTime2d(CoordinateTime2D time2d) {
      this.time2d = time2d;
      return this;
    }
  }

  private GribGridCoordinateSystem makeCoordinateSystem(Iterable<Integer> indices,
      HashMap<Integer, CoordAndAxis> coordIndexMap) {
    List<GridAxis> axes = Streams.stream(indices).map(this.gridAxes::get).collect(Collectors.toList());
    GribGridTimeCoordinateSystem tcs = makeTimeCoordinateSystem(indices, coordIndexMap);
    return new GribGridCoordinateSystem(axes, tcs, this.horizCoordinateSystem);
  }

  private GribGridTimeCoordinateSystem makeTimeCoordinateSystem(Iterable<Integer> indices,
      HashMap<Integer, CoordAndAxis> coordIndexMap) {
    List<Integer> timeIndices = Streams.stream(indices).filter(index -> this.gridAxes.get(index).getAxisType().isTime())
        .collect(Collectors.toList());
    int hash = makeHash(timeIndices);
    List<CoordAndAxis> coordAndAxesList = Streams.stream(indices).map(coordIndexMap::get).collect(Collectors.toList());
    return this.timeCoordinateSystems.computeIfAbsent(hash,
        h -> GribGridTimeCoordinateSystem.create(dataset.getType(), coordAndAxesList));
  }

  private int makeHash(Iterable<Integer> indices) {
    Hasher hasher = Hashing.goodFastHash(32).newHasher();
    indices.forEach(hasher::putInt);
    return hasher.hash().asInt();
  }

  @Override
  public void close() throws IOException {
    gribCollection.close();
  }

  @Override
  public String getName() {
    return gribCollection.getName();
  }

  @Override
  public String getLocation() {
    return gribCollection.getLocation() + "#" + group.getId();
  }

  @Override
  public AttributeContainer attributes() {
    return gribCollection.getGlobalAttributes();
  }

  @Override
  public FeatureType getFeatureType() {
    return FeatureType.GRID; // LOOK needed?
  }

  @Override
  public ImmutableList<GridCoordinateSystem> getGridCoordinateSystems() {
    return ImmutableList.copyOf(gridCoordinateSystems.values());
  }

  @Override
  public ImmutableList<GridAxis> getGridAxes() {
    ImmutableList.Builder<GridAxis> builder = ImmutableList.builder();
    builder.addAll(gridAxes.values());
    builder.add(horizCoordinateSystem.getYHorizAxis());
    builder.add(horizCoordinateSystem.getXHorizAxis());
    return builder.build();
  }

  @Override
  public ImmutableList<Grid> getGrids() {
    return this.grids.stream().collect(ImmutableList.toImmutableList());
  }
}
