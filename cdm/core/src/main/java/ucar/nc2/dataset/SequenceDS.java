/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.ma2.Array;
import java.io.IOException;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/**
 * Enhanced sequence
 * 
 * @author caron
 * @since Nov 10, 2009
 * @deprecated SequenceDS will not extend StructureDS in 6.
 */
@Deprecated
public class SequenceDS extends StructureDS {

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return new StructureDataConverter(this, orgSeq.getStructureIterator(bufferSize));
  }

  private static class StructureDataConverter implements StructureDataIterator {
    private StructureDataIterator orgIter;
    private SequenceDS newStruct;
    private int count;

    StructureDataConverter(SequenceDS newStruct, StructureDataIterator orgIter) {
      this.newStruct = newStruct;
      this.orgIter = orgIter;
    }

    @Override
    public boolean hasNext() throws IOException {
      return orgIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      StructureData sdata = orgIter.next();
      return newStruct.convert(sdata, count++);
    }

    @Override
    public void setBufferSize(int bytes) {
      orgIter.setBufferSize(bytes);
    }

    @Override
    public StructureDataIterator reset() {
      orgIter = orgIter.reset();
      return (orgIter == null) ? null : this;
    }

    @Override
    public int getCurrentRecno() {
      return orgIter.getCurrentRecno();
    }

    @Override
    public void close() {
      orgIter.close();
    }
  }

  @Override
  public Array read(ucar.ma2.Section section) throws java.io.IOException {
    return read();
  }

  @Override
  public Array read() throws IOException {
    Array data = orgSeq.read();
    return convert(data, null);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private ucar.nc2.Sequence orgSeq;

  protected SequenceDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.orgSeq = builder.orgSeq;
    this.orgVar = builder.orgSeq;
    this.orgName = builder.orgName;
  }

  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setOriginalVariable(this.orgVar).setOriginalName(this.orgName).setUnits(this.proxy.units)
        .setDesc(this.proxy.desc);
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

  public static abstract class Builder<T extends Builder<T>> extends StructureDS.Builder<T> {
    private ucar.nc2.Sequence orgSeq;
    private boolean built;

    public T setOriginalVariable(Sequence orgVar) {
      this.orgSeq = orgVar;
      return self();
    }

    public T setOriginalName(String orgName) {
      this.orgName = orgName;
      return self();
    }

    public T setUnits(String units) {
      this.units = units;
      if (units != null) {
        addAttribute(new Attribute(CDM.UNITS, units));
      }
      return self();
    }

    public T setDesc(String desc) {
      this.desc = desc;
      if (desc != null) {
        addAttribute(new Attribute(CDM.LONG_NAME, desc));
      }
      return self();
    }

    /** Copy metadata from orgVar. */
    public T copyFrom(Sequence orgVar) {
      super.copyFrom(orgVar);
      for (Variable v : orgVar.getVariables()) {
        Variable.Builder newVar;
        if (v instanceof Sequence) {
          newVar = SequenceDS.builder().copyFrom((Sequence) v);
        } else if (v instanceof Structure) {
          newVar = StructureDS.builder().copyFrom((Structure) v);
        } else {
          newVar = VariableDS.builder().copyFrom(v);
        }
        addMemberVariable(newVar);
      }
      setOriginalVariable(orgVar);
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
