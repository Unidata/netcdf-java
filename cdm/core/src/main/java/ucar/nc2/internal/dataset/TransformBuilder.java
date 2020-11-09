package ucar.nc2.internal.dataset;

import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.Nullable;
import java.util.Formatter;

public class TransformBuilder {
  public String name;
  private AttributeContainer ctvAttributes = new AttributeContainerMutable("");
  private CoordinateTransform preBuilt;
  private boolean built;

  public TransformBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public TransformBuilder setCtvAttributes(AttributeContainer ctv) {
    this.ctvAttributes = ctv;
    return this;
  }

  public TransformBuilder setPreBuilt(CoordinateTransform preBuilt) {
    this.preBuilt = preBuilt;
    this.name = preBuilt.getName();
    return this;
  }

  @Nullable
  public CoordinateTransform build(NetcdfDataset ncd) {
    if (built)
      throw new IllegalStateException("already built " + name);
    built = true;

    if (this.preBuilt != null) {
      return this.preBuilt;
    }

    CoordinateTransform.Builder<?> ctb =
        CoordTransformFactory.makeCoordinateTransform(ncd, this.ctvAttributes, new Formatter(), new Formatter());
    if (ctb == null) {
      return null;
    }
    if (this.name != null) {
      ctb.setName(this.name);
    }
    ctb.setCtvAttributes(this.ctvAttributes);
    return ctb.build();
  }
}
