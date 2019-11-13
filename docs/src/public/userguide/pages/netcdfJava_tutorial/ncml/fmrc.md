---
title: Forecast Model Run Collection Aggregation
last_updated: 2019-11-05
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: fmrc_ref.html
---

## Definitions
A <b>_forecast model_</b> is a scientific model that predicts the time evolution of a system starting from some initial state. Its output is a time series for each of its variables, which may be state variables or derived variables. The model is often run periodically, so one may have a collection of different runs. The collection of data output for a particular model run is called a <b>_forecast model run_</b>, which may be output in a single file or in several files. The model is run periodically, and so one may have a collection of <b>_forecast model runs_</b>, which we assume can be uniquely identified by the start of the model run, called the model run time, also called the <b>_analysis time_</b> or <b>_generating time_</b>.

The time series for a forecast model run is the list of <b>_forecast times_</b>, also known as the <b>_valid times_</b>. (For our purposes, the forecast times are just whatever the time series is, ignoring whether its a forecast or a nowcast, etc.). The difference between the run time and the forecast time is the forecast offset, sometimes called the forecast hour.

## Grid datasets with two time dimensions
The <b>ucar.nc2.dt.GridDatatype</b> is the CDM _scientific datatype_ for grids. It has been generalized to allow two time dimensions, called the <b>_runtime_</b> and <b>_time_</b> dimensions, in order to represent collections of forecast model runs. Such a dataset can be created by writing a single file, or by using NcML aggregation to create a virtual dataset out of multiple files.

Both the <b>_runtime_</b> and <b>_time_</b> coordinates may have type <b>_String</b>_ or _<b>char</b>_ and hold ISO 8601 dates, or have a <b>_udunit_</b> date unit string in its units attribute. For example:

~~~
  String runtime(run=8);
     :long_name = "Run time for model";
     :standard_name = "forecast_reference_time";
     :_CoordinateAxisType = "RunTime";

  data:
  "2006-09-05T12:00:00Z", "2006-09-06T12:00:00Z", "2006-09-07T12:00:00Z", "2006-09-08T12:00:00Z", 
  "2006-09-09T12:00:00Z", "2006-09-10T12:00:00Z", "2006-09-11T12:00:00Z", "2006-09-12T12:00:00Z"
~~~

The <b>_time_</b> coordinate is the forecast (valid) time, and it will differ for each run, and so will be two dimensional:

~~~
   double time(run=8, time=16);
     :units = "hours since 2006-09-05T12:00:00Z";
     :long_name = "forecast (valid) time";
     :standard_name = "time";
     :_CoordinateAxisType = "Time";

   data:       
   {90.0, 96.0, 102.0, 108.0, 114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0},
   {114.0, 120.0, 126.0, 132.0, 138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0},
   {138.0, 144.0, 150.0, 156.0, 162.0, 168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0},
   {162.0, 168.0, 174.0, 180.0, 186.0, 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0, 234.0, 240.0, 246.0, 252.0},
   {186.0, 192.0, 198.0, 204.0, 210.0, 216.0, 222.0, 228.0, 234.0, 240.0, 246.0, 252.0, 258.0, 264.0, 270.0, 276.0},
   {210.0, 216.0, 222.0, 228.0, 234.0, 240.0, 246.0, 252.0, 258.0, 264.0, 270.0, 276.0, 282.0, 288.0, 294.0, 300.0},
   {234.0, 240.0, 246.0, 252.0, 258.0, 264.0, 270.0, 276.0, 282.0, 288.0, 294.0, 300.0, 306.0, 312.0, 318.0, 324.0},
   {258.0, 264.0, 270.0, 276.0, 282.0, 288.0, 294.0, 300.0, 306.0, 312.0, 318.0, 324.0, 330.0, 336.0, 342.0, 348.0}

~~~

The data variables will generally have both the runtime and time dimensions, as well as the z, y, and x dimensions:

~~~
   float Dew_point_temperature(run=8, time=16, height_above_ground1=1, y=689, x=1073);
     :units = "K";
     :long_name = "Dew point temperature @ height_above_ground";
~~~

A dataset with a runtime dimension and a 2D time dimension as described here is called an <b>_FMRC_</b> (Forecast Run Model Collection) <b>_dataset_</b>. You can open it as an ordinary dataset and manipulate it through the <b>_NetcdfFile_</b> or <b>_NetcdfDataset_</b> APIs.

More typically you want to open it as a <b>_ucar.nc2.dt.GridDataset_</b>, so that the grid variables are found and made into <b>_ucar.nc2.dt.GridDatatype_</b> objects, and especially so that the time coordinates are found through methods on the <b>_ucar.nc2.dt.GridCoordSystem_</b>:

~~~
  public CoordinateAxis1DTime getRunTimeAxis();
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index);
~~~

Possibly more interesting is to make it into a <b>_ucar.nc2.dt.fmrc.ForecastModelRunCollection_</b> object, which allows you to view the dataset in several ways. This option is described in section 4 below.

## Aggregating Forecast Model Runs

A common case is that the model output is spread out in multiple files. A special kind of NcML aggregation can be used to create an FMRC dataset.

### Case 1: All data for each forecast model run is in a single file
This case is similar to a [JoinNew](/ncml_aggregation.html#exercise-3-aggregation-on-a-new-dimension){:target="_blank"} aggregation, in that a new, outer dimension is created, and each file becomes one slice of the new dataset.

~~~
   <?xml version="1.0" encoding="UTF-8"?>
(1)<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" enhance="true" >
(2) <aggregation dimName="runtime" type="forecastModelRunCollection">
(3)  <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_0000.grib1" coordValue="2006-09-10T00:00:00Z" enhance="true" />
     <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_0600.grib1" coordValue="2006-09-10T06:00:00Z" enhance="true" />
     <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_1200.grib1" coordValue="2006-09-10T12:00:00Z" enhance="true" />
    </aggregation>
   </netcdf>
~~~

* The netcdf element always has <b>_enhance="true"_</b>, which adds the coordinate systems needed for a GridDataset.
* A <b>_forecastModelRunCollection_</b> aggregation is declared, and an outer dimension called <b>_runtime_</b> will be created.
* All the files in the collection are explicitly named, as well as their <b>_runtime coordinate values_</b>. The values must be ISO 8601 formatted dates. The files themselves must contain all the output times from one model run. The attribute <b>_enhance="true"_</b> adds the coordinate systems needed to identify the (forecast) time coordinate.

Equivalently, you can use an NcML <b>_scan_</b> element:

~~~
   <?xml version="1.0" encoding="UTF-8"?>
   <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" enhance="true" >
    <aggregation dimName="run" type="forecastModelRunCollection">
(1)   <scan location="/data/ldm/NAM_CONUS_80km/" suffix=".grib1" dateFormatMark="Run_#yyyyMMdd_HHmm" enhance="true" />
    </aggregation>
   </netcdf>
~~~

* All the files in the directory <b>_/data/ldm/CONUS_80km/_</b>, ending in <b>_.grib1_</b> will be aggregated. The run time coordinate values will be extracted from the filename, using the <b>_dateFormatMark_</b> attribute.
A runtime dimension and coordinate variable is added, and the time coordinate is made into a 2D coordinate, as required for an FMRC datasset:
~~~
   double time(run=3, time=11);
     :units = "hours since 2006-09-10T00:00:00Z";
     :long_name = "Coordinate variable for time dimension";
     :standard_name = "time";
     :_CoordinateAxisType = "Time";
~~~

This example assumes that all the time coordinates in each of the files have the same units, in this example, <b>_"hours since 2006-09-10T00:00:00Z"_</b>. If that is not the case, then the time values must be read in and adjusted to have a common unit, which is indicated by adding the <b>_timeUnitsChange_</b> attribute on the aggregation element:

~~~
    <aggregation dimName="run" type="forecastModelRunCollection" timeUnitsChange="true">
~~~

When you have a different number of forecast times in each model run, you must also use the timeUnitsChange attribute on the aggregation element (as of 4.0.18).

### Case 2: Data for each forecast model run is in multiple files
In this case we can use nested aggregations, the inner aggregation to join the files together that make one run, then an outer aggregation to make the runs into an FMRC dataset. The following is a single FMRC that shows 3 variations on how to do the inner aggregations:

 <?xml version="1.0" encoding="UTF-8"?>
 <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" enhance="true">

(1)<aggregation dimName="run" type="forecastModelRunCollection">

(2) <netcdf coordValue="2006-09-10T00:00:00Z">
(3)  <aggregation dimName="Time" type="joinExisting">
(4)   <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_0000/Hour_F00.grib1" coordValue="0"/>
      <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_0000/Hour_F03.grib1" coordValue="3"/>
      <netcdf location="file:/data/ldm/NAM_CONUS_80km/Run_20060910_0000/Hour_F06.grib1" coordValue="6"/>
     </aggregation>
    </netcdf>

(5) <netcdf coordValue="2006-09-10T06:00:00Z">
     <aggregation dimName="Time" type="joinExisting">
(6)   <scan location="/data/ldm/NAM_CONUS_80km/Run_20060910_0600/" suffix=".grib1" />
     </aggregation>
    </netcdf>

(7) <netcdf coordValue="2006-09-10T12:00:00Z">

(8)  <variable name="Time" shape="Time" type="int">
 	    <attribute name="long_name" value="Forecast Time"/>
      <attribute name="units" value="hours since 2006-09-10T12:00:00Z"/>
      <attribute name="_CoordinateAxisType" value="Time"/>
      <values start="0" increment="1"/>
     </variable>

     <aggregation dimName="Time" type="joinExisting">
      <scan location="/data/ldm/NAM_CONUS_80km/Run_20060910_1200/" suffix=".grib1" />
     </aggregation>
    </netcdf>

  </aggregation
 </netcdf>
The outer, forecastModelRunCollection aggregation is declared. It will consist of the nested datasets.
The first inner dataset has a run time coordinate of 2006-09-10T00:00:00Z.
The aggregation will join on the existing dimension called Time.
Each file is explicitly listed, along with its coordinate value for the forecast time. Since only one value is listed, the files must have only one forecast time coordinate.
The second inner dataset has a run time coordinate of 2006-09-10T06:00:00Z.
All the files in the directory /data/ldm/NAM_CONUS_80km/Run_20060910_0600/, ending in .grib1 will be aggregated. The time coordinate values must be found in each file's time coordinate variable.
The third inner dataset has a run time coordinate of 2006-09-10T12:00:00Z.
The coordinate variable for the aggregation dimension is defined, and given attributes and values. The datasets will be sorted alphanumerically, and they must be 1 hour apart.

### Case 3: Data for each forecast model run is in multiple files, with one forecast time per file
In the case where the data has a single time step in each file, and we can figure out the runtime and forecast time from each file's pathname, we can use a special aggregation type called forecastModelRunSingleCollection, and a special form of the scan element called scanFmrc.

 <?xml version="1.0" encoding="UTF-8"?>
 <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">

(1) <aggregation dimName="run" type="forecastModelRunSingleCollection" timeUnitsChange="true" >

(2) <scanFmrc location="/data/grib/rtmodels/" regExp=".*_nmm\.GrbF[0-9]{5}$"
(3)   runDateMatcher="yyMMddHH#_nmm.GrbF#"
(4)   forecastOffsetMatcher="#_nmm.GrbF#HHH"/>
   </aggregation>
 </netcdf>
The aggregation is declared as type forecastModelRunSingleCollection, whose forecast time units vary.
All the files in /data/grib/rtmodels/ whose full pathname matches the regular expression ".*_nmm\.GrbF[0-9]{5}$" are used.
The files will be grouped by run date by extracting the run date from the filename using the runDateMatcher pattern.
Within each run, the forecastOffset will be extracted using the forecastOffsetMatcher pattern.
In this example, the files are named, for example 06091212_nmm.GrbF03000. The regular expression ensures that only files that have a literal "_nmm.GrbF" in the name, followed by exactly 5 digits, and nothing afterwards, will be used. The runDateMatcher matches the literal "_nmm.GrbF" in the file's full pathname, then applies the java.text.SimpleDateFormat yyMMddHH to the 8 characters that come before the match, to derive the run date coordinate. The forecastOffsetMatcher also matches the literal "_nmm.GrbF", then turns the 3 characters after the match into a double, to calculate the hour offset from the run date.

Another example:

    <scanFmrc location="C:/data/rap/" suffix=".nc" subdirs="true"
              runDateMatcher="yyyyMMddHH#/wrfout_d01_#"
              forecastDateMatcher="#/wrfout_d01_#yyyy-MM-dd_HHmm"/>
works on the files in the directory C:/data/rap/ and its subdirectories that end in ".nc". These files are named, for example C:/data/rap/2006070611/wrfout_d01_2006-07-06_080000.DPG_F.nc. The runDateMatcher matches the literal "/wrfout_d01_" in the file's full pathname, then applies the SimpleDateFormat yyyyMMddHH to the 10 characters that come before the match, to derive the run date coordinate. The forecastDateMatcher also matches the literal "/wrfout_d01_", then applies the SimpleDateFormat yyyy-MM-dd_HHmm to the 15 characters that come after the match, to derive the forecast time coordinate. Note that in this forecastDateMatcher example, the "-" and "_" characters are literals, and the other chanarcters "y", "M", "d", "H" are special characters that match year, month, day and hour numbers, respectively. The dates are assumed to be GMT unless otherwise specified. See java.text.SimpleDateFormat for more details of how date matching works.

Notes common to all cases
A Variable that is also a Coordinate will not be promoted to use the runtime dimension, unless you explicitly tell it to do so, eg:
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <aggregation dimName="runtime" type="forecastModelRunCollection" recheckEvery="10min" timeUnitsChange="true">
   <variableAgg name="my_vertical_coord"/>
   <scan location="test" dateFormatMark="ncom_glb_reg7_#yyyyMMdd" subdirs="false"/>
 </aggregation>
</netcdf>
    
## Forecast Model Run Collection
In Figure 1, a ten day forecast with daily output snapshots is made once a day.



Figure 1. A Forecast Model Runs Collection (FMRC)

 

If you wrap an FMRC dataset in a ucar.nc2.dt.fmrc.ForecastModelRunCollection, you can then create different views of the dataset that all have 1D time coordinates. You might use a FmrcImpl constructor, an implementation of the ForecastModelRunCollection interface:

  public FmrcImpl(String location);  
  public FmrcImpl(NetcdfDataset ncd);
You can then extract the following views of the dataset from the ForecastModelRunCollection object:

  public List getRunDates();  // possible run dates; List<Date>
  public NetcdfDataset getRunTimeDataset( Date runTime);  // one model run

  public List getForecastDates(); // possible forecast dates; List<Date>
  public NetcdfDataset getForecastTimeDataset( Date forecastTime); // constant forecast date
  public List getForecastOffsets(); // possible forecast offsets; List<Double>
  public NetcdfDataset getForecastOffsetDataset( double offset); // constant forecast offset
  public NetcdfDataset getBestTimeSeries( ); 
The getRunTimeDataset() method will return a forecast model run dataset representing one of the vertical columns in Figure 1 (above). The getRunDates() method returns a list of the valid run times.

The getForecastTimeDataset() method will return a constant forecast date dataset representing one of the horizontal columns in Figure 1. The getForecastDates() method returns a list of the valid forecast times. This kind of dataset has successively shorter forecasts of the same endpoint. The final timestep is a nowcast (where T=0 for the run).  

The getForecastOffsetDataset() method will return a constant forecast offset dataset representing one of the diagonals in Figure 1. The getForecastOffsets() method returns a list of the valid forecast offsets, normally in units of hours. For example, in Figure 1, you might compare the nowcast dataset (offset = 0, purple diagonal) from the 3 day forecast dataset (dark blue diagonal), to track how well the model does in its 3 day forecasts.

Finally, the getBestTimeSeries() method creates the "best" time series over the collection. In Figure 1 it will include the nowcasts (purple diagonal) as well as the latest model forecasts (rightmost column).

## Using FMRC with the TDS datasetFmrc element
The datasetFmrc element has been replaced by the featureCollection element in TDS 4.2. The result in a TDS catalog is the same however:



## Debugging Tips
When things dont work, its best to debug the aggregation outside of the THREDDS Data Server. Put the NcML in its own file, and try to open with ToolsUI.
The underlying datasets have to be gridded datasets. Open a component file in ToolsUI and see if its a grid. The CoordSys and FeatureTypes/Grids tabs are used for this.