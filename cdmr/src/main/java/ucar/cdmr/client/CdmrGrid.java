package ucar.cdmr.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.cdmr.CdmrGridProto;
import ucar.cdmr.CdmrConverter;
import ucar.nc2.AttributeContainer;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CdmrGrid implements Grid {
  private static final Logger log = LoggerFactory.getLogger(CdmrGrid.class);

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
    return proto.getUnits();
  }

  @Override
  public AttributeContainer attributes() {
    return CdmrConverter.decodeAttributes(getName(), proto.getAttributesList());
  }

  @Override
  public ArrayType getArrayType() {
    return CdmrConverter.convertDataType(proto.getDataType());
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return coordsys;
  }

  @Override
  public boolean hasMissing() {
    return proto.getHasMissing();
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException {
    subset.setGridName(getName());
    return cdmrGridDataset.readData(subset);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final CdmrGridDataset cdmrGridDataset;
  private final CdmrGridProto.Grid proto;
  private final GridCoordinateSystem coordsys;

  private CdmrGrid(Builder builder, List<GridCoordinateSystem> coordsys) {
    this.cdmrGridDataset = builder.cdmrGridDataset;
    this.proto = builder.proto;
    Optional<GridCoordinateSystem> csopt =
        coordsys.stream().filter(cs -> cs.getName().equals(proto.getCoordSys())).findFirst();
    if (csopt.isPresent()) {
      this.coordsys = csopt.get();
    } else {
      throw new IllegalStateException("Cant find coordinate system " + proto.getCoordSys());
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
    private CdmrGridDataset cdmrGridDataset;
    private CdmrGridProto.Grid proto;
    private boolean built;

    public Builder setDataset(CdmrGridDataset cdmrGridDataset) {
      this.cdmrGridDataset = cdmrGridDataset;
      return this;
    }

    public Builder setProto(CdmrGridProto.Grid proto) {
      this.proto = proto;
      return this;
    }

    public CdmrGrid build(List<GridCoordinateSystem> coordsys) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CdmrGrid(this, coordsys);
    }
  }

}
