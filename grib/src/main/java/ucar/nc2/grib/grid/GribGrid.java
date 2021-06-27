/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridReferencedArray;
import ucar.nc2.grid.GridSubset;

import java.io.IOException;

public class GribGrid implements Grid {
  private final GribGridCoordinateSystem coordinateSystem;
  private final GribCollectionImmutable.VariableIndex vi;
  private final String name;
  private final AttributeContainer attributes;

  public GribGrid(GribGridCoordinateSystem coordinateSystem, GribCollectionImmutable gribCollection,
      GribCollectionImmutable.VariableIndex vi) {
    this.coordinateSystem = coordinateSystem;
    this.vi = vi;
    this.name = vi.makeVariableName();

    AttributeContainerMutable atts = new AttributeContainerMutable(vi.makeVariableName());
    atts.addAttribute(new Attribute(CDM.LONG_NAME, vi.makeVariableDescription()));
    atts.addAttribute(new Attribute(CDM.UNITS, vi.makeVariableUnits()));
    gribCollection.addVariableAttributes(atts, vi);
    this.attributes = atts.toImmutable();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return this.attributes.findAttributeString(CDM.LONG_NAME, null);
  }

  @Override
  public String getUnits() {
    return this.attributes.findAttributeString(CDM.UNITS, null);
  }

  @Override
  public AttributeContainer attributes() {
    return this.attributes;
  }

  @Override
  public ArrayType getArrayType() {
    return ArrayType.FLOAT;
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException {
    return null;
  }
}
