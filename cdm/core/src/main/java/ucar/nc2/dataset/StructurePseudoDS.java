/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Make a collection of variables with the same outer dimension into a fake Structure.
 * Its fake because the variables are not stored contiguously.
 * 
 * <pre>
 *  so
 *   var1(dim, other);
 *   var2(dim, other);
 *   var3(dim, other);
 * becomes
 *   struct {
 *     var1(other);
 *     var2(other);
 *     var3(other);
 *   } name(dim);
 * </pre>
 * 
 * @author caron
 */
@Immutable
public class StructurePseudoDS extends StructureDS {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructurePseudoDS.class);
  private static boolean debugRecord = false;

  /**
   * Make a Structure out of all Variables with the named dimension as their outermost dimension, or from a list
   * named Variables, each has the same named outermost dimension.
   *
   * @param group part of this group
   * @param shortName short name of this Structure
   * @param varNames limited to these variables, all must have dim as outer dimension. If null, use all Variables
   *        with that outer dimension
   * @param outerDim existing, outer dimension
   */
  public static StructurePseudoDS fromVars(Group group, String shortName, List<String> varNames, Dimension outerDim) {
    StructurePseudoDS.Builder<?> builder =
        StructurePseudoDS.builder().setName(shortName).setDimensions(ImmutableList.of(outerDim));

    if (varNames == null) {
      List<Variable> vars = group.getVariables();
      varNames = new ArrayList<>(vars.size());
      for (Variable orgV : vars) {
        if (orgV.getDataType() == DataType.STRUCTURE)
          continue;

        Dimension dim0 = orgV.getDimension(0);
        if (outerDim.equals(dim0))
          varNames.add(orgV.getShortName());
      }
    }

    for (String name : varNames) {
      Variable orgV = group.findVariableLocal(name);
      if (orgV == null) {
        log.warn("StructurePseudoDS cannot find variable " + name);
        continue;
      }
      if (orgV instanceof Structure) {
        continue; // no substructures
      }

      Dimension dim0 = orgV.getDimension(0);
      if (!outerDim.equals(dim0))
        throw new IllegalArgumentException(
            "Variable " + orgV.getNameAndDimensions() + " must have outermost dimension=" + outerDim);

      VariableDS.Builder<?> memberV = VariableDS.builder().setName(orgV.getShortName()).setDataType(orgV.getDataType())
          .setUnits(orgV.getUnitsString()).setDesc(orgV.getDescription());
      memberV.setSPobject(orgV.getSPobject()); // ??
      memberV.addAttributes(orgV.attributes());

      List<Dimension> dims = new ArrayList<>(orgV.getDimensions());
      dims.remove(0); // remove outer dimension
      memberV.setDimensions(dims);

      // memberV.enhance(enhanceScaleMissing); TODO ??

      builder.addMemberVariable(memberV);
      builder.addOriginalVariable(orgV);
    }
    return builder.build(group);
  }

  @Override
  public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
    if (debugRecord)
      System.out.println(" read all psuedo records ");
    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA(smembers, getShape());

    for (Variable v : orgVariables) {
      Array data = v.read();
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }

  @Override
  public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    if (null == section)
      return _read();

    if (debugRecord)
      System.out.println(" read psuedo records " + section.getRange(0));

    String err = section.checkInRange(getShape());
    if (err != null)
      throw new InvalidRangeException(err);

    Range r = section.getRange(0);

    StructureMembers smembers = makeStructureMembers();
    ArrayStructureMA asma = new ArrayStructureMA(smembers, section.getShape());

    for (Variable v : orgVariables) {
      List<Range> vsection = new ArrayList<>(v.getRanges());
      vsection.set(0, r);
      Array data = v.read(vsection); // LOOK should these be flattened ??
      StructureMembers.Member m = smembers.findMember(v.getShortName());
      m.setDataArray(data);
    }

    return asma;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  protected List<Variable> orgVariables = new ArrayList<>(); // the underlying original variables

  protected StructurePseudoDS(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.orgVariables = builder.orgVariables;
  }

  @Override
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.addOriginalVariables(this.orgVariables);
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
    private List<Variable> orgVariables = new ArrayList<>(); // the underlying original variables
    private boolean built;

    public T addOriginalVariable(Variable orgVar) {
      orgVariables.add(orgVar);
      return self();
    }

    public T addOriginalVariables(List<Variable> orgVars) {
      orgVariables.addAll(orgVars);
      return self();
    }

    /** Normally this is called by Group.build() */
    public StructurePseudoDS build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      this.setDataType(DataType.STRUCTURE);
      return new StructurePseudoDS(this, parentGroup);
    }
  }

}
