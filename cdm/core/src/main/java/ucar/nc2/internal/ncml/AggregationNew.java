/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.ncml;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.CancelTask;

/**
 * JoinNew Aggregation.
 *
 * @author caron
 */
public class AggregationNew extends AggregationOuter {

  public AggregationNew(NetcdfDataset.Builder ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.joinNew, recheckS);
  }

  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);

    // open a "typical" nested dataset and copy it to newds
    AggDataset typicalDataset = getTypicalDataset();
    NetcdfFile typical = typicalDataset.acquireFile(null);
    BuilderHelper.transferDataset(typical, ncDataset, null);

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension(dimName, getTotalCoords());
    Group.Builder root = ncDataset.rootGroup;
    root.removeDimension(dimName); // remove previous declaration, if any
    root.addDimension(aggDim);

    promoteGlobalAttributes((AggDatasetOuter) typicalDataset);

    List<String> aggVarNames = getAggVariableNames();

    // Look for a variable matching the new aggregation dimension
    Optional<Variable.Builder<?>> joinAggCoord = root.findVariable(dimName);

    // Not found, create the aggregation coordinate variable
    if (!joinAggCoord.isPresent()) {
      DataType coordType = getCoordinateType();
      VariableDS.Builder joinAggCoordVar =
          VariableDS.builder().setName(dimName).setDataType(coordType).setDimensionsByName(dimName);
      root.addVariable(joinAggCoordVar);
      joinAggCoordVar.setProxyReader(this);
      if (isDate)
        joinAggCoordVar.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));

      // if speced externally, this variable will get replaced
      // LOOK was CacheVar cv = new CoordValueVar(joinAggCoordVar.getFullName(), joinAggCoordVar.dataType,
      // joinAggCoordVar.units);
      CacheVar cv = new CoordValueVar(joinAggCoordVar.shortName, joinAggCoordVar.dataType, joinAggCoordVar.units);
      joinAggCoordVar.setSPobject(cv);
      cacheList.add(cv);
    } else {
      Variable.Builder joinAggCoordVar = joinAggCoord.get();
      if (joinAggCoordVar.getRank() == 0) {
        // For an existing variable matching the aggregated dim name, if it's a scalar
        // variable, we can just use it and its values for the aggregation coordinate variable
        // Need to ensure it's included in the list of variables to aggregate
        if (!aggVarNames.contains(joinAggCoordVar.shortName)) {
          aggVarNames.add(joinAggCoordVar.shortName);
        }
      } else {
        throw new IllegalArgumentException(
            "Variable " + dimName + " already exists, but is not a scalar (suitable for aggregating as a coordinate).");
      }
    }

    // if no names specified, add all "non-coordinate" variables.
    // Note that we haven't identified coordinate systems with CoordSysBuilder, so that info is not available.
    // So this isn't that general of a solution. But probably better than nothing
    if (aggVarNames.isEmpty()) {
      for (Variable v : typical.getVariables()) {
        if (!(v instanceof CoordinateAxis))
          aggVarNames.add(v.getShortName());
      }
    }

    // now we can create all the aggNew variables
    // use only named variables
    for (String varname : aggVarNames) {
      Optional<Variable.Builder<?>> aggVarOpt = root.findVariable(varname);
      if (!aggVarOpt.isPresent()) {
        logger.error(ncDataset.location + " aggNewDimension cant find variable " + varname);
        continue;
      }
      Variable.Builder aggVar = aggVarOpt.get();

      // construct new variable, replace old one LOOK what about Structures?
      // LOOK was Group.Builder newGroup = BuilderHelper.findGroup(ncDataset, aggVar.getParentGroup());
      VariableDS.Builder vagg = VariableDS.builder().setName(aggVar.shortName).setDataType(aggVar.dataType)
          .setDimensionsByName(dimName + " " + Dimensions.makeDimensionsString(aggVar.dimensions));
      vagg.setProxyReader(this);
      BuilderHelper.transferAttributes(aggVar.getAttributeContainer(), vagg.getAttributeContainer());

      // _CoordinateAxes if it exists must be modified
      Attribute att = vagg.getAttributeContainer().findAttribute(_Coordinate.Axes);
      if (att != null) {
        String axes = dimName + " " + att.getStringValue();
        vagg.addAttribute(new Attribute(_Coordinate.Axes, axes));
      }

      root.replaceVariable(vagg);
      aggVars.add(vagg);

      if (cancelTask != null && cancelTask.isCancel())
        return;
    }

    setDatasetAcquireProxy(typicalDataset, ncDataset);
    typicalDataset.close(typical); // close it because we use DatasetProxyReader to acquire

    if (isDate && timeUnitsChange) {
      root.findVariable(dimName).ifPresent(v -> {
        try {
          readTimeCoordinates(v, cancelTask);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      });
    }

    // LOOK ncDataset.finish();
  }

  /**
   * What is the data type of the aggregation coordinate ?
   *
   * @return the data type of the aggregation coordinate
   */
  private DataType getCoordinateType() {
    List<AggDataset> nestedDatasets = getDatasets();
    AggDatasetOuter first = (AggDatasetOuter) nestedDatasets.get(0);
    return first.isStringValued ? DataType.STRING : DataType.DOUBLE;
  }

}
