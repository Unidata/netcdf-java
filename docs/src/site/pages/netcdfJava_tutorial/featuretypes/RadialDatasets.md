---
title: Radial Datasets
last_updated: 2019-06-28
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: radial_datasets.html
---
## Tutorial: The Radial Scientific Feature Type

### The Radial Dataytpe

<b>_Radial Datatype_</b> uses polar coordinates (elevation, azimuth, distance) to describe the location of its points in space, and it is referred as <b>_Radial Coordinate System_</b>. <b>_A Radial Coordinate System_</b> has a <b>_Elevation_</b>, <b>_Azimuth_</b>, and <b>_Distance_</b> coordinate axis. It may also has a <b>_Time_</b> coordinate axis. Generally, in our level II and level III radar products, there is a time variable at radial (elevation, azimuth), or sweep (elevation) level, so it is considered as a variable, rather than coordinate axis.

A <b>_RadialDatasetSweep_</b> has a collection of <b>_Radial Variables_</b>. The data in each Variable is organized into <b>_sweeps_</b>. A sweep is a connected set of <b>_radials_</b>. All the radials in a sweep are assumed to have the same number of gates and the same <b>_beam width_</b> and <b>_nyquist frequency_</b>. A <b>_radial_</b> is a set of data sampled along a straight line at constant intervals called the <b>_gate size_</b>. The radial's geometry is described by an <b>_elevation_</b> and <b>_azimuth angles_</b> relative to some origin.

A <b>_RadialDatasetSweep_</b> is a NetcdfDataset whose Radials have been identified and grouped into Sweeps based on Radial Coordinate Systems. Here is the UML for the RadialDatasetSweep interface classes, found in the <b>_ucar.nc2.dt_</b> package:

{% include image.html file="netcdf-java/tutorial/feature_types/Radial.png" alt="Radial" caption="Radial Interface" %}

### Opening and using a RadialDatasetSweep

The general way to open a RadialDataset is to use FeatureDatasetFactoryManager

~~~
  Formatter errlog = new Formatter();
  FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.RADIAL, location, null, errlog);
  if (fdataset == null) {
    log.error("**failed on {} %n --> {} %n", location, errlog);
    return null;
  }

  FeatureType ftype = fdataset.getFeatureType();
  assert (ftype == FeatureType.RADIAL);
  RadialDatasetSweep radialDataset = (RadialDatasetSweep) fdataset;
~~~
  
TypedDatasetFactory wraps three RadialDatasetSweep classes: ucar.nc2.dt.radial.Nids2Dataset, ucar.nc2.dt.radial.LevelII2Dataset, and ucar.nc2.dt.radial.Dorade2Dataset. It opens a radial dataset with an appropriate class, and can be extended to cover more radial products. If the radar is stationary, isStationary() returns true, and getCommonOrigin() returns the earth location of radar. If the radar product has more than one sweep, isVolume() will return 1. The getDataVariables returns the list of variables, for each variable, getNumSweeps(), getSweep(int sweepNum), and readAllData() is used to handle the data variable at the sweep level, inside each sweep, getRadialNumber(), getGateNumber(), getElevation(int radial), getTime(int radial), and readData(int radial) returns all information associated with the radial object.
 
### Example of Retrieving information from a RadialDataset

~~~
  private void showInfo(RadialDatasetSweep rds, String varName, Formatter f) {
    f.format("Radial Dataset %s%n", rds.getLocation());

      /* radar information */
    String stationID = rds.getRadarID();
    String stationName = rds.getRadarName();
    boolean isVolume = rds.isVolume();
    f.format("  stationName = %s%n", stationName);
    f.format("  isVolume = %s%n", isVolume);

      /* radial variable */
    RadialDatasetSweep.RadialVariable v = (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
    f.format("  info for variable = %s%n", varName);
    f.format("  number of sweeps = %d%n", v.getNumSweeps());

    // loop over sweeps
    for (int sweep = 0; sweep < v.getNumSweeps(); sweep++) {
      RadialDatasetSweep.Sweep sw = v.getSweep(sweep);
      float me = sw.getMeanElevation();
      int nrays = sw.getRadialNumber();
      int ngates = sw.getGateNumber();
      f.format("    %d : elev=%f nrays=%d ngates=%d%n", sweep, me, nrays, ngates);

      try {
        for (int j = 0; j < nrays; j++) {
          float azi = sw.getAzimuth(j);
          float ele = sw.getElevation(j);
          float[] data = sw.readData(j);
          f.format("      %d : azimuth=%f elev=%f data len=%d%n", j, azi, ele, data.length);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
~~~
 
### Using ToolsUI to look at Radial Dataset

You can use ToolsUI <b>_FeatureTypes/Radial_</b> Tab to view Radial Datasets. The top table lists the RadialVariables in the dataset, and selecting one of these shows the Sweeps that are contained by that variable.

{% include image.html file="netcdf-java/tutorial/feature_types/TuiRadial.png" alt="Tui Radial" caption="" %}

Like all CDM datasets, you can also use ToolsUI Viewer Tab to view the netCDF structure of a Radial Dataset:

{% include image.html file="netcdf-java/tutorial/feature_types/TuiViewerRadialDataset.png" alt="Radial Dataset" caption="" %}

Use the IDVto display the Radial Dataset of a level II product, showing 3D sweep of elevation angle 0.5 degree:

{% include image.html file="netcdf-java/tutorial/feature_types/IDV3DSweep.png" alt="IDV 3d Sweep" caption="" %}

and volume image of Reflectivity which is composed of 9 elevation sweeps:

{% include image.html file="netcdf-java/tutorial/feature_types/IDVVolume.png" alt="IDV Volume" caption="" %}