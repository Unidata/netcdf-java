/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import ucar.ma2.ArrayStructure;
import ucar.ma2.DataType;
import ucar.nc2.Group;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.Array;
import java.io.IOException;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.StructureDataEnhancer.StructureDataIteratorEnhanced;

/** Enhanced sequence */
public class SequenceDS extends Sequence implements StructureEnhanced {

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return new StructureDataIteratorEnhanced(this, orgSeq.getStructureIterator(bufferSize));
  }

  @Override
  public Variable getOriginalVariable() {
    return orgSeq;
  }

  @Override
  public String getOriginalName() {
    return orgName;
  }

  @Override
  public ImmutableList<CoordinateSystem> getCoordinateSystems() {
    return null;
  }

  @Override
  public Array read(ucar.ma2.Section section) throws java.io.IOException {
    return read();
  }

  @Override
  public Array read() throws IOException {
    Array data = orgSeq.read();
    StructureDataEnhancer enhancer = new StructureDataEnhancer(this);
    return enhancer.enhance((ArrayStructure) data, null);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private final ucar.nc2.Sequence orgSeq;
  private final String orgName; // in case Sequence was renamed, and we need the original name

  protected SequenceDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.orgSeq = builder.orgSeq;
    this.orgName = builder.orgName;
  }

  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setOriginalSequence(this.orgSeq).setOriginalName(this.orgName);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends Sequence.Builder<T> {
    private ucar.nc2.Sequence orgSeq;
    private String orgName;
    private boolean built;

    public T setOriginalSequence(Sequence orgVar) {
      this.orgSeq = orgVar;
      return self();
    }

    public T setOriginalName(String orgName) {
      this.orgName = orgName;
      return self();
    }

    /** Copy metadata from orgVar. */
    public T copyFrom(Sequence orgVar) {
      super.copyFrom(orgVar);
      for (Variable v : orgVar.getVariables()) {
        Variable.Builder<?> newVar;
        if (v instanceof Sequence) {
          newVar = SequenceDS.builder().copyFrom((Sequence) v);
        } else if (v instanceof Structure) {
          newVar = StructureDS.builder().copyFrom((Structure) v);
        } else {
          newVar = VariableDS.builder().copyFrom(v);
        }
        addMemberVariable(newVar);
      }
      setOriginalSequence(orgVar);
      setOriginalName(orgVar.getShortName());
      return self();
    }

    /** Normally this is called by Group.build() */
    public SequenceDS build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.SEQUENCE);
      return new SequenceDS(this, parentGroup);
    }
  }

}
