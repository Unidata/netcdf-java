/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.RangeIterator;
import ucar.array.SectionIterable;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.collection.GribArrayReader;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.MaterializedCoordinateSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Grib implementation of {@link Grid} */
public class GribGrid implements Grid {
  private final GridCoordinateSystem coordinateSystem;
  public final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.VariableIndex vi;
  private final String name;
  private final AttributeContainer attributes;

  public GribGrid(GribIosp iosp, GribCollectionImmutable gribCollection, GridCoordinateSystem coordinateSystem,
      GribCollectionImmutable.VariableIndex vi) {
    this.coordinateSystem = coordinateSystem;
    this.gribCollection = gribCollection;
    this.vi = vi;
    this.name = iosp.makeVariableName(vi);

    AttributeContainerMutable atts = new AttributeContainerMutable(this.name);
    atts.addAttribute(new Attribute(CDM.LONG_NAME, iosp.makeVariableLongName(vi)));
    atts.addAttribute(new Attribute(CDM.UNITS, iosp.makeVariableUnits(vi)));
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
    Optional<MaterializedCoordinateSystem> opt = coordinateSystem.subset(subset, errLog);
    if (opt.isEmpty()) {
      throw new InvalidRangeException(errLog.toString()); // LOOK lame. Optional, empty, null?
    }
    MaterializedCoordinateSystem subsetCoordSys = opt.get();

    if (subsetCoordSys.specialReadNeeded()) {
      // handles longitude cylindrical coord when data has full (360 deg) longitude axis.
      Array<Number> data = subsetCoordSys.readSpecial(this);
      return GridReferencedArray.create(getName(), getArrayType(), data, subsetCoordSys);

    } else {
      List<RangeIterator> ranges = new ArrayList<>(subsetCoordSys.getSubsetRanges());
      SectionIterable want = new ucar.array.SectionIterable(ranges, getCoordinateSystem().getNominalShape());

      GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vi);
      Array<?> data = dataReader.readData(want);

      return GridReferencedArray.create(this.name, getArrayType(), (Array<Number>) data, subsetCoordSys);
    }
  }

  @Override
  public Array<Number> readDataSection(ucar.array.Section section) throws InvalidRangeException, IOException {
    GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vi);
    SectionIterable want = new ucar.array.SectionIterable(section, getCoordinateSystem().getNominalShape());
    return (Array<Number>) dataReader.readData(want);
  }

  @Override
  public String toString() {
    return name;
  }
}
