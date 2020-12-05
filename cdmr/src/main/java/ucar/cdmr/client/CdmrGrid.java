package ucar.cdmr.client;

import ucar.cdmr.CdmrGridProto;
import ucar.cdmr.CdmrConverter;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.AttributeContainer;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CdmrGrid implements Grid {
  @Override
  public String getName() {
    return proto.getName();
  }

  @Nullable
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
  public DataType getDataType() {
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
  public GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException {
    return null; // TODO
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final CdmrGridProto.Grid proto;
  private final GridCoordinateSystem coordsys;

  private CdmrGrid(Builder builder, List<GridCoordinateSystem> coordsys) {
    this.proto = builder.proto;
    Optional<GridCoordinateSystem> csopt = coordsys.stream().filter(cs -> cs.getName().equals(proto.getCoordSys())).findFirst();
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
    private CdmrGridProto.Grid proto;
    private boolean built;

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
