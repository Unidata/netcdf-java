/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.util.Optional;
import javax.annotation.Nullable;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Group.Builder;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

/** Helper methods for constructing NetcdfDatasets. */
class BuilderHelper {

  /**
   * Copy contents of "src" to "target". skip ones that already exist (by name).
   * Dimensions and Variables are replaced with equivalent elements, but unlimited dimensions are turned into regular
   * dimensions.
   * Attribute doesnt have to be replaced because its immutable, so its copied by reference.
   *
   * @param src transfer from here. If src is a NetcdfDataset, transferred variables get reparented to target group.
   * @param target transfer to this NetcdfDataset.
   * @param replaceCheck if null, add if a Variable of the same name doesnt already exist, otherwise
   *        replace if replaceCheck.replace( Variable v) is true
   */
  static void transferDataset(NetcdfFile src, NetcdfDataset.Builder<?> target,
      @Nullable ReplaceVariableCheck replaceCheck) {
    transferGroup(src, target, src.getRootGroup(), target.rootGroup, replaceCheck);
  }

  // transfer the objects in src group to the target group
  private static void transferGroup(NetcdfFile ds, NetcdfDataset.Builder<?> targetDs, Group src,
      Group.Builder targetGroup, @Nullable ReplaceVariableCheck replaceCheck) {
    boolean unlimitedOK = true;

    // group attributes
    transferAttributes(src.attributes(), targetGroup.getAttributeContainer());

    // dimensions
    for (Dimension d : src.getDimensions()) {
      if (!targetGroup.findDimensionLocal(d.getShortName()).isPresent()) {
        Dimension newd = Dimension.builder().setName(d.getShortName()).setIsShared(d.isShared())
            .setIsUnlimited(unlimitedOK && d.isUnlimited()).setIsVariableLength(d.isVariableLength())
            .setLength(d.getLength()).build();
        targetGroup.addDimension(newd);
      }
    }

    // variables
    for (Variable v : src.getVariables()) {
      Optional<Variable.Builder<?>> targetV = targetGroup.findVariableLocal(v.getShortName());
      boolean replace = (replaceCheck != null) && replaceCheck.replace(v); // replaceCheck not currently used
      if (replace || !targetV.isPresent()) { // replace it
        VariableDS.Builder<?> vb;
        if (!(v instanceof VariableDS)) {
          vb = VariableDS.builder().copyFrom(v);
        } else {
          vb = ((VariableDS) v).toBuilder();
        }
        targetGroup.replaceVariable(vb);
      }
    }

    // nested groups - check if target already has it
    for (Group srcNested : src.getGroups()) {
      Optional<Builder> existing = targetGroup.findGroupLocal(srcNested.getShortName());
      if (!existing.isPresent()) {
        Group.Builder nested = Group.builder().setName(srcNested.getShortName());
        targetGroup.addGroup(nested);
        transferGroup(ds, targetDs, srcNested, nested, replaceCheck);
      } else {
        transferGroup(ds, targetDs, srcNested, existing.get(), replaceCheck);
      }
    }
  }

  static void transferAttributes(AttributeContainer src, AttributeContainerMutable target) {
    for (Attribute a : src) {
      if (null == target.findAttribute(a.getShortName()))
        target.addAttribute(a);
    }
  }

}
