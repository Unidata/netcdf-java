/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.dods;

import com.google.common.base.Preconditions;
import opendap.util.RC;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;

/**
 * Describe {@link Class}
 */
public class DodsRegroup {

  Group.Builder rootGroup;

  // Define a utility class to decompose names
  private static class NamePieces {
    String prefix = null; // group part of the path
    String var = null; // struct part of the path
    String name = null; // last name in a path
  }

  /**
   * Go thru the variables/structure-variables and their attributes and move to the proper groups.
   */
  protected void reGroup() {
    Preconditions.checkArgument(RC.getUseGroups());

    // Start by moving global attributes
    // An issue to be addressed is that some attributes that should be attached
    // to variables, instead get made global with name var.att.
    for (Attribute ncatt : rootGroup.getAttributeContainer()) {
      DODSAttribute dodsatt = (DODSAttribute) ncatt;
      String dodsname = dodsatt.getDodsName();
      NamePieces pieces = parseName(dodsname);
      if (pieces.var != null) {
        // Figure out which variable to which this attribute should be moved.
        // In the event that there is no matching
        // variable, then keep the attribute as is.
        String searchname = pieces.var;
        if (pieces.prefix != null)
          searchname = pieces.prefix + '/' + searchname;
        Variable.Builder<?> v = rootGroup.findVariableNested(searchname).orElse(null);
        if (v != null) {
          // TODO WRONG MUST REMOVE
          // rootGroup.remove(ncatt);
          // change attribute name to remove var.
          Attribute renameAtt = ncatt.toBuilder().setName(pieces.name).build();
          v.addAttribute(renameAtt);
        }
      } else if (pieces.prefix != null) {
        // We have a true group global name to move to proper group
        // convert prefix to an actual group
        Group.Builder gb = Group.builder().setName(dodsname); // LOOK ignorelast?
        rootGroup.addGroup(gb);
        // TODO WRONG MUST REMOVE
        // rootGroup.remove(ncatt);
        gb.addAttribute(ncatt);
      }
    }

    Object[] varlist = rootGroup.getVariables().toArray();

    // In theory, we should be able to fix variable attributes
    // by just removing the group prefix. However, there is the issue
    // that attribute names sometimes have as a suffix varname.attname.
    // So, we should use that to adjust the attribute to attach to that
    // variable.
    for (Object var : varlist) {
      reGroupVariableAttributes(rootGroup, (Variable) var);
    }
  }

  @Deprecated
  protected void reGroupVariable(Group.Builder rootGroup, DodsVariable dodsv) {
    String dodsname = dodsv.getDodsName();
    NamePieces pieces = parseName(dodsname);
    if (pieces.prefix != null) {
      // convert prefix to an actual group
      Group.Builder gnew = rootGroup.makeRelativeGroup(this, dodsname, true);
      // Get current group for the variable
      Group.Builder gold;
      gold = dodsv.getParentGroupOrRoot();
      if (gnew != gold) {
        gold.remove(dodsv);
        dodsv.setParentGroup(gnew);
        gnew.addVariable(dodsv);
      }
    }
  }

  private void reGroupVariableAttributes(Group.Builder rootGroup, Variable.Builder vb) {
    String vname = vb.shortName;
    Group.Builder vgroup = vb.getParentGroupOrRoot();
    for (Attribute ncatt : vb.getAttributeContainer()) {
      DODSAttribute dodsatt = (DODSAttribute) ncatt;
      String dodsname = dodsatt.getDodsName();
      NamePieces pieces = parseName(dodsname);
      Group.Builder agroup;
      if (pieces.prefix != null) {
        // convert prefix to an actual group
        agroup = rootGroup.makeRelativeGroup(this, dodsname, true);
      } else
        agroup = vgroup;

      // If the attribute group is different from the variable group,
      // then we have some kind of inconsistency; presumably from
      // the original dds+das; in any case, use the variable's group
      if (agroup != vgroup)
        agroup = vgroup;

      if (pieces.var != null && !pieces.var.equals(vname)) {
        // move the attribute to the correct variable
        // (presumably in the same group)
        Variable.Builder<?> newvar = agroup.findVariableLocal(pieces.var).orElse(null);
        if (newvar != null) {// if not found leave the attribute as is
          // otherwise, move the attribute and rename
          // TODO WRONG MUST REMOVE
          // v.remove(ncatt);
          Attribute renameAtt = ncatt.toBuilder().setName(pieces.name).build();
          v.addAttribute(renameAtt);
        }
      }
    }
  }

  // Utility to decompose a name
  NamePieces parseName(String name) {
    NamePieces pieces = new NamePieces();
    int dotpos = name.lastIndexOf('.');
    int slashpos = name.lastIndexOf('/');
    if (slashpos < 0 && dotpos < 0) {
      pieces.name = name;
    } else if (slashpos >= 0 && dotpos < 0) {
      pieces.prefix = name.substring(0, slashpos);
      pieces.name = name.substring(slashpos + 1, name.length());
    } else if (slashpos < 0 && dotpos >= 0) {
      pieces.var = name.substring(0, dotpos);
      pieces.name = name.substring(dotpos + 1, name.length());
    } else {// slashpos >= 0 && dotpos >= 0)
      if (slashpos > dotpos) {
        pieces.prefix = name.substring(0, slashpos);
        pieces.name = name.substring(slashpos + 1, name.length());
      } else {// slashpos < dotpos)
        pieces.prefix = name.substring(0, slashpos);
        pieces.var = name.substring(slashpos + 1, dotpos);
        pieces.name = name.substring(dotpos + 1, name.length());
      }
    }
    // fixup
    if (pieces.prefix != null && pieces.prefix.length() == 0)
      pieces.prefix = null;
    if (pieces.var != null && pieces.var.length() == 0)
      pieces.var = null;
    if (pieces.name.length() == 0)
      pieces.name = null;
    return pieces;
  }

  // end reGroup?
  ///////////////////////////////////////////////////////////////////////////////////////////

}
