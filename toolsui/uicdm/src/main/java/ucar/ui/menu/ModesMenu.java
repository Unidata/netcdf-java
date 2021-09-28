/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.menu;

import thredds.client.catalog.tools.DataFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.internal.dataset.CoordSystemFactory;
import ucar.nc2.internal.iosp.hdf4.H4header;
import ucar.nc2.internal.iosp.hdf5.H5iosp;
// import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenu;

public class ModesMenu extends JMenu {

  private final ToolsUI toolsui;

  public ModesMenu(ToolsUI tui) {
    super("Modes");
    setMnemonic('M');

    this.toolsui = tui;

    addNetcdfFileSubmenu();
    addNetcdfDatasetSubmenu();
    addHdfeosSubmenu();
    addGribSubmenu();
    addFmrcSubmenu();
  }

  private void addNetcdfFileSubmenu() {
    JMenu ncMenu = new JMenu("NetcdfFile");

    AbstractAction a;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        toolsui.setUseRecordStructure(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "Netcdf3: use records", false, 'V', -1);
    BAMutil.addActionToMenu(ncMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        toolsui.setUseBuilders(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "use builders", true, 'B', -1);
    BAMutil.addActionToMenu(ncMenu, a);

    // Add the submenu
    add(ncMenu);
  }

  private void addNetcdfDatasetSubmenu() {
    JMenu dsMenu = new JMenu("NetcdfDataset");
    AbstractAction a;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        CoordSystemFactory.setUseMaximalCoordSys(state);
      }
    };
    BAMutil.setActionPropertiesToggle(a, null, "Set Use Maximal CoordSystem",
        CoordSystemFactory.getUseMaximalCoordSys(), 'N', -1);
    BAMutil.addActionToMenu(dsMenu, a);

    // Add the submenu
    add(dsMenu);
  }


  private void addHdfeosSubmenu() {
    JMenu subMenu = new JMenu("HDF-EOS");

    AbstractAction a;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        H5iosp.useHdfEos(state);
      }
    };
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionProperties(a, null, "Use HDF-EOS StructMetadata to augment HDF5", true, '5', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        H4header.useHdfEos(state);
      }
    };
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionProperties(a, null, "Use HDF-EOS StructMetadata to augment HDF4", true, '4', -1);
    BAMutil.addActionToMenu(subMenu, a);

    // Add the submenu
    add(subMenu);
  }


  private void addGribSubmenu() {
    JMenu subMenu = new JMenu("GRIB");

    AbstractAction a;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        toolsui.setGribDiskCache();
      }
    };
    BAMutil.setActionProperties(a, null, "Set Grib disk cache\u2026", false, 'G', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        Grib1ParamTables.setStrict(state);
      }
    };
    boolean strictMode = Grib1ParamTables.isStrict();
    a.putValue(BAMutil.STATE, strictMode);
    BAMutil.setActionPropertiesToggle(a, null, "GRIB1 strict", strictMode, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean state = (Boolean) getValue(BAMutil.STATE);
        GribData
            .setInterpolationMethod(state ? GribData.InterpolationMethod.cubic : GribData.InterpolationMethod.linear);
      }
    };
    boolean useCubic = GribData.getInterpolationMethod() == GribData.InterpolationMethod.cubic;
    a.putValue(BAMutil.STATE, useCubic);
    BAMutil.setActionPropertiesToggle(a, null, "Use Cubic Interpolation on Thin Grids", useCubic, 'I', -1);
    BAMutil.addActionToMenu(subMenu, a);

    // static public boolean useGenTypeDef = false, useTableVersionDef = true, intvMergeDef = true, useCenterDef = true;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useGenTypeDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useGenTypeDef);
    BAMutil.setActionPropertiesToggle(a, null, "useGenType", FeatureCollectionConfig.useGenTypeDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useTableVersionDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useTableVersionDef);
    BAMutil.setActionPropertiesToggle(a, null, "useTableVersion", FeatureCollectionConfig.useTableVersionDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.intvMergeDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.intvMergeDef);
    BAMutil.setActionPropertiesToggle(a, null, "intvMerge", FeatureCollectionConfig.intvMergeDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FeatureCollectionConfig.useCenterDef = (Boolean) getValue(BAMutil.STATE);
      }
    };
    a.putValue(BAMutil.STATE, FeatureCollectionConfig.useCenterDef);
    BAMutil.setActionPropertiesToggle(a, null, "useCenter", FeatureCollectionConfig.useCenterDef, 'S', -1);
    BAMutil.addActionToMenu(subMenu, a);

    // add the submenu
    add(subMenu);
  }


  private void addFmrcSubmenu() {
    JMenu subMenu = new JMenu("FMRC");

    AbstractAction a;

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        FeatureCollectionConfig.setRegularizeDefault(state);
      }
    };
    // ToolsUI default is to regularize the FMRC
    FeatureCollectionConfig.setRegularizeDefault(true);
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionPropertiesToggle(a, null, "regularize", true, 'R', -1);
    BAMutil.addActionToMenu(subMenu, a);

    a = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        DataFactory.setPreferCdm(state);
      }
    };
    // ToolsUI default is to use cdmRemote access
    DataFactory.setPreferCdm(true);
    a.putValue(BAMutil.STATE, true);
    BAMutil.setActionPropertiesToggle(a, null, "preferCdm", true, 'P', -1);
    BAMutil.addActionToMenu(subMenu, a);

    // Add the submenu
    add(subMenu);
  }
}
