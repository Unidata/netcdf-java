/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.ft2.coverage.*;
import java.util.*;

/**
 * Helper class to create logical subsets.
 * Used by CFGridCoverageWriter
 *
 * @author caron
 * @since 7/12/2015
 */
class CoverageSubsetter2 {

  static java.util.Optional<CoverageCollection> makeCoverageDatasetSubset(CoverageCollection org,
      List<String> gridsWanted, SubsetParams params, Formatter errlog) {

    // Get subset of original objects that are needed by the requested grids
    List<Coverage> orgCoverages = new ArrayList<>();
    Map<String, CoverageCoordSys> orgCoordSys = new HashMap<>(); // eliminate duplicates
    Set<String> coordTransformSet = new HashSet<>(); // eliminate duplicates

    for (String gridName : gridsWanted) {
      Coverage orgGrid = org.findCoverage(gridName);
      if (orgGrid == null)
        continue;
      orgCoverages.add(orgGrid);
      CoverageCoordSys cs = orgGrid.getCoordSys();
      orgCoordSys.put(cs.getName(), cs);
      coordTransformSet.addAll(cs.getTransformNames());
    }

    // LOOK bail out if any fail, make more robust
    // subset all coordSys, and eliminate duplicate axes.
    Map<String, CoverageCoordAxis> subsetCoordAxes = new HashMap<>();
    Map<String, CoverageCoordSys> subsetCFCoordSys = new HashMap<>();
    for (CoverageCoordSys orgCs : orgCoordSys.values()) {
      // subsetCF make do some CF tweaks, not needed in regular subset
      java.util.Optional<CoverageCoordSys> opt = orgCs.subset(params, true, false, errlog);
      if (!opt.isPresent()) {
        return java.util.Optional.empty();
      }

      CoverageCoordSys subsetCoordSys = opt.get();
      subsetCFCoordSys.put(orgCs.getName(), subsetCoordSys);
      for (CoverageCoordAxis axis : subsetCoordSys.getAxes()) {
        subsetCoordAxes.put(axis.getName(), axis); // eliminate duplicates
      }
    }

    // here are the objects we need to make the subsetted dataset
    List<CoverageCoordSys> coordSys = new ArrayList<>();
    List<CoverageCoordAxis> coordAxes = new ArrayList<>();
    List<Coverage> coverages = new ArrayList<>();
    List<CoverageTransform> coordTransforms = new ArrayList<>();

    coordSys.addAll(subsetCFCoordSys.values());

    // must use a copy, because of setDataset()
    coordAxes.addAll(subsetCoordAxes.values());

    for (Coverage orgCov : orgCoverages) {
      // must substitute subsetCS
      CoverageCoordSys subsetCs = subsetCFCoordSys.get(orgCov.getCoordSysName());
      coverages.add(new Coverage(orgCov, subsetCs)); // must use a copy, because of setCoordSys()
    }

    for (String tname : coordTransformSet) {
      CoverageTransform t = org.findCoordTransform(tname); // these are truly immutable, so can use originals
      if (t != null)
        coordTransforms.add(t);
    }

    // put it all together
    return java.util.Optional.of(new CoverageCollection(org.getName(), org.getCoverageType(),
        new AttributeContainerMutable(org.getName(), org.attributes()), null, null, null, coordSys, coordTransforms,
        coordAxes, coverages, org.getReader())); // use org.reader -> subset always in coord space !
  }

  CoverageCoordAxis1D findIndependentAxis(String want, List<CoverageCoordAxis> axes) {
    String name = want == null ? null : want.trim();
    for (CoverageCoordAxis axis : axes)
      if (axis instanceof CoverageCoordAxis1D && axis.getName().equalsIgnoreCase(name))
        return (CoverageCoordAxis1D) axis;
    return null;
  }

}
