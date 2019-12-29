/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Group.Builder;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ReplaceVariableCheck;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;

/**
 * Helper methods for constructing NetcdfDatasets.
 * 
 * @author caron
 * @since Jul 6, 2007
 */
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
  static void transferDataset(NetcdfFile src, NetcdfDataset.Builder target, ReplaceVariableCheck replaceCheck) {
    transferGroup(src, target, src.getRootGroup(), target.rootGroup, replaceCheck);
  }

  // transfer the objects in src group to the target group
  private static void transferGroup(NetcdfFile ds, NetcdfDataset.Builder targetDs, Group src, Group.Builder targetGroup,
      ReplaceVariableCheck replaceCheck) {
    boolean unlimitedOK = true; // LOOK why not allowed?

    // group attributes
    transferAttributes(src, targetGroup.getAttributeContainer());

    // dimensions
    for (Dimension d : src.getDimensions()) {
      if (!targetGroup.findDimensionLocal(d.getShortName()).isPresent()) {
        Dimension newd = Dimension.builder(d.getShortName(), d.getLength()).setIsShared(d.isShared())
            .setIsUnlimited(unlimitedOK && d.isUnlimited()).setIsVariableLength(d.isVariableLength()).build();
        targetGroup.addDimension(newd);
      }
    }

    // variables
    for (Variable v : src.getVariables()) {
      Optional<Variable.Builder<?>> targetV = targetGroup.findVariable(v.getShortName());
      boolean replace = (replaceCheck != null) && replaceCheck.replace(v); // replaceCheck not currently used
      if (replace || !targetV.isPresent()) { // replace it
        // LOOK not needed ??
        /*
         * if ((v instanceof Structure) && !(v instanceof StructureDS)) {
         * v = new StructureDS(targetGroup, (Structure) v);
         * } else
         */
        VariableDS.Builder<?> vb;
        if (!(v instanceof VariableDS)) {
          vb = VariableDS.builder().copyFrom(v);
        } else {
          vb = ((VariableDS) v).toBuilder().setProxyReader(null);
        }

        targetGroup.replaceVariable(vb);
        // LOOK not needed? v.resetDimensions(); // dimensions will be different

      } /*
         * LOOK was else if (!targetV.hasCachedData() && (targetVe.getOriginalVariable() == null)) {
         * // this is the case where we defined the variable, but didnt set its data. we now set it with the first
         * nested
         * // dataset that has a variable with the same name
         * targetVe.setOriginalVariable(v);
         * }
         */
    }

    // nested groups - check if target already has it
    for (Group srcNested : src.getGroups()) {
      Optional<Builder> existing = targetGroup.findGroup(srcNested.getShortName());
      if (!existing.isPresent()) {
        Group.Builder nested = Group.builder(targetGroup).setName(srcNested.getShortName());
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

  /**
   * Find the Group in newFile that corresponds (by name) with oldGroup
   *
   * @param newFile look in this NetcdfFile
   * @param oldGroup corresponding (by name) with oldGroup
   * @return corresponding Group, or null if no match.
   */
  static Group findGroup(NetcdfFile newFile, Group oldGroup) {
    List<Group> chain = new ArrayList<>(5);
    Group g = oldGroup;
    while (g.getParentGroup() != null) { // skip the root
      chain.add(0, g); // put in front
      g = g.getParentGroup();
    }

    Group newg = newFile.getRootGroup();
    for (Group oldg : chain) {
      newg = newg.findGroup(oldg.getShortName());
      if (newg == null)
        return null;
    }
    return newg;
  }

  private static final String boundsDimName = "bounds_dim";

  static Dimension getBoundsDimension(NetcdfFile ncfile) {
    Group g = ncfile.getRootGroup();
    Dimension d = g.findDimension(boundsDimName);
    if (d == null)
      d = ncfile.addDimension(g, new Dimension(boundsDimName, 2, true));
    return d;
  }

}
