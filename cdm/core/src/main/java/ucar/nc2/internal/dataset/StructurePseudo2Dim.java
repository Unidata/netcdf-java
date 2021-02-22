/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.*;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import ucar.nc2.dataset.VariableDS;

/** Make a collection of variables with the same 2 outer dimensions into a fake 2D Structure(outer,inner) */
@Immutable
public class StructurePseudo2Dim extends StructurePseudoDS {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructurePseudo2Dim.class);
  private static final boolean debugRecord = false;

  /**
   * Make a Structure out of named Variables which have var(outer, inner, ...)
   *
   * @param group the containing group, if null use root group
   * @param shortName short name of this Structure
   * @param varNames limited to these variables. all must var(outer, inner, ...). If null, then find all such variables.
   * @param outer the outer dimension, may not be null
   * @param inner the inner dimension, may not be null
   */
  public static StructurePseudo2Dim fromVars(Group group, String shortName, List<String> varNames, Dimension outer,
      Dimension inner) {
    StructurePseudo2Dim.Builder<?> builder =
        StructurePseudo2Dim.builder().setName(shortName).setDimensions(ImmutableList.of(outer, inner));

    // find all variables in this group that has this as the outer dimension
    if (varNames == null) {
      List<Variable> vars = group.getVariables();
      varNames = new ArrayList<>(vars.size());
      for (Variable orgV : vars) {
        if (orgV.getRank() < 2)
          continue;
        if (outer.equals(orgV.getDimension(0)) && inner.equals(orgV.getDimension(1)))
          varNames.add(orgV.getShortName());
      }
    }

    for (String name : varNames) {
      Variable orgV = group.findVariableLocal(name);
      if (orgV == null) {
        log.warn("StructurePseudo2Dim cannot find variable " + name);
        continue;
      }
      if (orgV instanceof Structure) {
        continue; // no substructures
      }

      if (!outer.equals(orgV.getDimension(0)))
        throw new IllegalArgumentException(
            "Variable " + orgV.getNameAndDimensions() + " must have outermost dimension=" + outer);
      if (!inner.equals(orgV.getDimension(1)))
        throw new IllegalArgumentException(
            "Variable " + orgV.getNameAndDimensions() + " must have 2nd dimension=" + inner);

      VariableDS.Builder<?> memberV = VariableDS.builder().setName(orgV.getShortName()).setDataType(orgV.getDataType())
          .setUnits(orgV.getUnitsString()).setDesc(orgV.getDescription());
      memberV.setSPobject(orgV.getSPobject()); // ??
      memberV.addAttributes(orgV.attributes());

      List<Dimension> dimList = new ArrayList<>(orgV.getDimensions());
      memberV.setDimensions(dimList.subList(2, dimList.size())); // remove first 2 dimensions
      // memberV.enhance(enhanceScaleMissing); ??

      builder.addMemberVariable(memberV);
      builder.addOriginalVariable(orgV);
    }

    return builder.build(group);
  }

  @Override
  protected Array _read(Section section) throws IOException, InvalidRangeException {
    if (null == section)
      return _read();

    if (debugRecord)
      System.out.println(" read psuedo records " + section.getRange(0));

    String err = section.checkInRange(getShape());
    if (err != null)
      throw new InvalidRangeException(err);

    Range outerRange = section.getRange(0);
    Range innerRange = section.getRange(1);

    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA(smembers, section.getShape());

    for (Variable v : orgVariables) {
      List<Range> vsection = new ArrayList<>(v.getRanges());
      vsection.set(0, outerRange);
      vsection.set(1, innerRange);
      Array data = v.read(vsection); // LOOK should these be flattened ??
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected StructurePseudo2Dim(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
  }

  @Override
  public Builder<?> toBuilder() {
    return (Builder<?>) super.addLocalFieldsToBuilder(builder());
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

  public static abstract class Builder<T extends Builder<T>> extends StructurePseudoDS.Builder<T> {
    private boolean built;

    /** Normally this is called by Group.build() */
    public StructurePseudo2Dim build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.STRUCTURE);
      return new StructurePseudo2Dim(this, parentGroup);
    }
  }
}
