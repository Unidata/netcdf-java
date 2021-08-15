/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.gcdm.GcdmGridProto;
import ucar.gcdm.GcdmConverter;
import ucar.nc2.AttributeContainer;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GcdmGrid implements Grid {
  private static final Logger log = LoggerFactory.getLogger(GcdmGrid.class);

  @Override
  public String getName() {
    return proto.getName();
  }

  @Override
  public String getDescription() {
    return proto.getDescription();
  }

  @Override
  public String getUnits() {
    return proto.getUnit();
  }

  @Override
  public AttributeContainer attributes() {
    return GcdmConverter.decodeAttributes(getName(), proto.getAttributesList());
  }

  @Override
  public ArrayType getArrayType() {
    return GcdmConverter.convertDataType(proto.getArrayType());
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return coordsys;
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
  public GridReferencedArray readData(GridSubset subset) throws IOException {
    subset.setGridName(getName());
    return gcdmGridDataset.readData(subset);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final GcdmGridDataset gcdmGridDataset;
  private final GcdmGridProto.Grid proto;
  private final GridCoordinateSystem coordsys;

  private GcdmGrid(Builder builder, List<GridCoordinateSystem> coordsys) {
    this.gcdmGridDataset = builder.gcdmGridDataset;
    this.proto = builder.proto;
    Optional<GridCoordinateSystem> csopt =
        coordsys.stream().filter(cs -> cs.getName().equals(proto.getCoordinateSystemName())).findFirst();
    if (csopt.isPresent()) {
      this.coordsys = csopt.get();
    } else {
      throw new IllegalStateException("Cant find coordinate system " + proto.getCoordinateSystemName());
    }
  }

  public Builder toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder addLocalFieldsToBuilder(Builder b) {
    return b.setProto(this.proto);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private GcdmGridDataset gcdmGridDataset;
    private GcdmGridProto.Grid proto;
    private boolean built;

    public Builder setDataset(GcdmGridDataset gcdmGridDataset) {
      this.gcdmGridDataset = gcdmGridDataset;
      return this;
    }

    public Builder setProto(GcdmGridProto.Grid proto) {
      this.proto = proto;
      return this;
    }

    public GcdmGrid build(List<GridCoordinateSystem> coordsys) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GcdmGrid(this, coordsys);
    }
  }

}
