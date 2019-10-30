package ucar.nc2.internal.dataset;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import ucar.ma2.DataType;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.CancelTask;

/** Helper class to enhance Datasets, with coordinate systems and scale/offset/missing */
public class DatasetEnhancer {

  public static boolean enhanceNeeded(Set<Enhance> want, Set<Enhance> have) {
    if (want == null)
      return false;
    if (have == null && !want.isEmpty())
      return true;
    for (Enhance mode : want) {
      if (!have.contains(mode))
        return true;
    }
    return false;
  }

  private final NetcdfFile ncfile;
  private final NetcdfDataset.Builder ds;
  private final Set<Enhance> wantEnhance;
  private final CancelTask cancelTask;

  public DatasetEnhancer(NetcdfFile ncfile, NetcdfDataset.Builder ds, Set<Enhance> wantEnhance, CancelTask cancelTask) {
    this.ncfile = ncfile;
    this.ds = ds;
    this.wantEnhance = wantEnhance == null ? EnumSet.noneOf(Enhance.class) : wantEnhance;
    this.cancelTask = cancelTask;
  }

  /*
   * private static CoordSysBuilderIF enhance(NetcdfDataset ds, Set<Enhance> mode, CancelTask cancelTask)
   * throws IOException {
   * if (mode == null) {
   * mode = EnumSet.noneOf(Enhance.class);
   * }
   * 
   * // CoordSysBuilder may enhance dataset: add new variables, attributes, etc
   * CoordSysBuilderIF builder = null;
   * if (mode.contains(Enhance.CoordSystems) && !ds.getEnhanceMode().contains(Enhance.CoordSystems)) {
   * builder = ucar.nc2.dataset.CoordSysBuilder.factory(ds, cancelTask);
   * builder.augmentDataset(ds, cancelTask);
   * // LOOK ds.convUsed = builder.getConventionUsed();
   * }
   * 
   * // now enhance enum/scale/offset/unsigned, using augmented dataset
   * if ((mode.contains(Enhance.ConvertEnums) && !ds.getEnhanceMode().contains(Enhance.ConvertEnums))
   * || (mode.contains(Enhance.ConvertUnsigned) && !ds.getEnhanceMode().contains(Enhance.ConvertUnsigned))
   * || (mode.contains(Enhance.ApplyScaleOffset) && !ds.getEnhanceMode().contains(Enhance.ApplyScaleOffset))
   * || (mode.contains(Enhance.ConvertMissing) && !ds.getEnhanceMode().contains(Enhance.ConvertMissing))) {
   * for (Variable v : ds.getVariables()) {
   * VariableEnhanced ve = (VariableEnhanced) v;
   * ve.enhance(mode);
   * if ((cancelTask != null) && cancelTask.isCancel())
   * return null;
   * }
   * }
   * 
   * // now find coord systems which may change some Variables to axes, etc
   * if (builder != null) {
   * // temporarily set enhanceMode if incomplete coordinate systems are allowed
   * if (mode.contains(Enhance.IncompleteCoordSystems)) {
   * ds.getEnhanceMode().add(Enhance.IncompleteCoordSystems);
   * builder.buildCoordinateSystems(ds);
   * ds.getEnhanceMode().remove(Enhance.IncompleteCoordSystems);
   * } else {
   * builder.buildCoordinateSystems(ds);
   * }
   * }
   * 
   * ds.finish(); // recalc the global lists
   * ds.getEnhanceMode().addAll(mode);
   * 
   * return builder;
   * }
   */

  /*
   * Enhancement use cases
   * 1. open NetcdfDataset(enhance).
   * 2. NcML - must create the NetcdfDataset, and enhance when its done.
   *
   * Enhance mode is set when
   * 1) the NetcdfDataset is opened
   * 2) enhance(EnumSet<Enhance> mode) is called.
   *
   * Possible remove all direct access to Variable.enhance
   */
  public NetcdfDataset enhance() throws IOException {

    // CoordSystemBuilder may enhance dataset: add new variables, attributes, etc
    CoordSystemBuilder coordSysBuilder = null;
    if (wantEnhance.contains(Enhance.CoordSystems) && !ds.enhanceMode.contains(Enhance.CoordSystems)) {
      Optional<CoordSystemBuilder> hasNewBuilder = CoordSystemFactory.factory(ds, cancelTask);
      if (!hasNewBuilder.isPresent()) {
        return useOldBuilder();
      }
      coordSysBuilder = hasNewBuilder.get();
      coordSysBuilder.augmentDataset(cancelTask);
      ds.setConventionUsed(coordSysBuilder.getConventionUsed());
    }

    enhanceGroup(ds.rootGroup);

    // now find coord systems which may change some Variables to axes, etc
    if (coordSysBuilder != null) {
      // temporarily set enhanceMode if incomplete coordinate systems are allowed
      if (wantEnhance.contains(Enhance.IncompleteCoordSystems)) {
        ds.enhanceMode.add(Enhance.IncompleteCoordSystems);
        coordSysBuilder.buildCoordinateSystems();
        ds.enhanceMode.remove(Enhance.IncompleteCoordSystems);
      } else {
        coordSysBuilder.buildCoordinateSystems();
      }
    }

    ds.enhanceMode.addAll(wantEnhance);
    return ds.build();
  }

  private void enhanceGroup(Group.Builder group) {

    for (Variable.Builder vb : group.vbuilders) {
      if (vb instanceof StructureDS.Builder<?>) {
        enhanceStructure((StructureDS.Builder<?>) vb);
      } else if (vb instanceof VariableDS.Builder) {
        enhanceVariable((VariableDS.Builder) vb);
      } else {
        throw new IllegalStateException("Not a VariableDS " + vb.shortName);
      }
    }

    for (Group.Builder gb : group.gbuilders) {
      enhanceGroup(gb);
    }
  }

  private void enhanceStructure(StructureDS.Builder<?> sdb) {
    for (Variable.Builder<?> vb : sdb.vbuilders) {
      if (vb instanceof StructureDS.Builder) {
        enhanceStructure((StructureDS.Builder) vb);
      } else if (vb instanceof VariableDS.Builder) {
        enhanceVariable((VariableDS.Builder) vb);
      } else {
        throw new IllegalStateException("Not a VariableDS " + vb.shortName);
      }
    }
  }

  /*
   * private void enhanceVariable(Variable.Builder v) {
   * if ((wantEnhance.contains(Enhance.ConvertEnums) && !ds.enhanceMode.contains(Enhance.ConvertEnums))
   * || (wantEnhance.contains(Enhance.ConvertUnsigned) && !ds.enhanceMode.contains(Enhance.ConvertUnsigned))
   * || (wantEnhance.contains(Enhance.ApplyScaleOffset) && !ds.enhanceMode.contains(Enhance.ApplyScaleOffset))
   * || (wantEnhance.contains(Enhance.ConvertMissing) && !ds.enhanceMode.contains(Enhance.ConvertMissing))) {
   * 
   * Variable newVar;
   * if (v instanceof Sequence) {
   * newVar = new SequenceDS(g, (Sequence) v);
   * } else if (v instanceof Structure) {
   * newVar = new StructureDS(g, (Structure) v);
   * } else {
   * newVar = new VariableDS(g, v, false); // enhancement done later
   * }
   * return newVar;
   * }
   */

  private void enhanceVariable(VariableDS.Builder vb) {
    Set<Enhance> varEnhance = EnumSet.copyOf(wantEnhance);

    // varEnhance will only contain enhancements not already applied to orgVar.
    if (vb.orgVar instanceof VariableDS) {
      for (Enhance orgVarEnhancement : ((VariableDS) vb.orgVar).getEnhanceMode()) {
        varEnhance.remove(orgVarEnhancement);
      }
    }

    // enhance() may have been called previously, with a different enhancement set.
    // So, we need to reset to default before we process this new set.
    // if (vb.orgDataType != null) {
    // vb.setDataType(vb.orgDataType); // LOOK needed ?
    // }

    if (varEnhance.contains(Enhance.ConvertEnums) && vb.dataType.isEnum()) {
      vb.setDataType(DataType.STRING);
      return; // We can return here, because the other conversions don't apply to STRING.
    }

    vb.addEnhanceMode(varEnhance);
  }

  // ODO remove after all conventions are ported.
  private NetcdfDataset useOldBuilder() throws IOException {
    return NetcdfDataset.wrap(ncfile, wantEnhance);
  }


}
