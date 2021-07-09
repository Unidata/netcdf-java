/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.RangeIterator;
import ucar.array.SectionIterable;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.collection.GribArrayReader;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridReferencedArray;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.MaterializedCoordinateSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Grib implementation of {@link Grid} */
public class GribGrid implements Grid {
  private final GribGridCoordinateSystem coordinateSystem;
  public final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.VariableIndex vi;
  private final String name;
  private final AttributeContainer attributes;

  public GribGrid(GribGridCoordinateSystem coordinateSystem, GribCollectionImmutable gribCollection,
      GribCollectionImmutable.VariableIndex vi) {
    this.coordinateSystem = coordinateSystem;
    this.gribCollection = gribCollection;
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

  // LOOK Array could be Vlen for Fmrc
  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException {
    Formatter errLog = new Formatter();
    // LOOK it would appear the only thing needed out of it (besides geo referencing) is getSubsetRanges()
    Optional<MaterializedCoordinateSystem> opt = coordinateSystem.subset(subset, errLog);
    if (opt.isEmpty()) {
      throw new InvalidRangeException(errLog.toString());
    }
    MaterializedCoordinateSystem subsetCoordSys = opt.get();
    List<RangeIterator> ranges = new ArrayList<>(subsetCoordSys.getSubsetRanges());
    SectionIterable want = new ucar.array.SectionIterable(ranges, getCoordinateSystem().getNominalShape());

    GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vi);
    Array<?> data = dataReader.readData(want);

    return GridReferencedArray.create(this.name, getArrayType(), (Array<Number>) data, subsetCoordSys);
  }
}
