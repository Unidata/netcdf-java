---
title: Coordinate Attribute Examples
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar
permalink: dmsp_example.html
toc: false
---

## Writing an IOSP:Example - DMSP

The ucar.nc2.iosp.dmsp.DSMPiosp class provides access to DMSP satellite data in the NOAA/NGDC DMSP archive format. Currently only data from the OLS instrument is supported, in particular only NOAA/NGDC DMSP OIS (OLS Integrated Smooth) data files. The OIS data contains both visible and infrared imagery at 2.7km resolution.
The DMSP satellites are polar orbiting satellites crossing the equator, depending on the satellite, at either dawn/dusk or noon/midnight. The OLS instrument is a scanning imager.
More information is available at http://dmsp.ngdc.noaa.gov/.

#### Structure of Dataset

~~~
The data files contain one or more text header records followed by a number of binary data records.
======
HEADER
======
RECORD 1 : VARIABLE 1, VARIABLE 2, ..., VARIABLE N
======
RECORD 2 : VARIABLE 1, VARIABLE 2, ..., VARIABLE N
======
...
======
RECORD 3 : VARIABLE 1, VARIABLE 2, ..., VARIABLE N
======
~~~

The header record contains metadata describing the dataset including the size of the records and the number of data records (sample header record):

~~~
    file ID: /dmsp/moby-1-3/subscriptions/IBAMA/1353226646955.tmp
(1) data set ID: DMSP F14 OLS LS & TS
 record bytes: 3040
    number of header records: 1
(2) number of records: 692
    suborbit history: F14200307192230.OIS (1,691)
    ...
    % daylight: 0.0
    % full moon: 57.8
    % terminator evident: 0.0
    end header
~~~

The structure of all the NGDC DMSP data types are described in an XDR descriptor file (dda.x). Here is a summary of the OIS data record description:

~~~
    short  Year;                             /* 4 digit year                   */
(3) short  DayOfYear;                        /* day of year (January 1 = 1)    */
    double SecondsOfDay;                     /* seconds of day [0.0 - 86400.0) */

    float Latitude;                          /* Geodetic Latitude in degrees   */
(4) float Longitude;                         /* Longitude in degrees (0 - 360) */
    float Altitude;                          /* Altitude in kilometers         */
    float Heading;                           /* Heading west of north          */

    ...
    solar
  and lunar conditions
    calibration
    ...

    struct {
      u_int QualityFlag;                     /* scan line quality flag */
(5)   opaque Pixels [ 1465 ];                /* scan line pixels       */
    } LightVideoData;                        /* pixels = 6 bit visible data */
    struct {
      u_int QualityFlag;                     /* scan line quality flag */
(6)   opaque Pixels [ 1465 ];                /* scan line pixels       */
    } ThermalVideoData;                      /* pixels = 8 bit thermal data */
~~~    
   
So each data record contains the time (3), the satellite location (4), a visible scan and quality flag (5), and a thermal/IR scan and quality flag (6).

From the header, we get the number of records (2) which will become the unlimited dimension -- after subtracting the number of header records. Another function of the header is to identify the dataset as NGDC DMSP OIS data (1).

#### Mapping the Data into the netCDF-3 Data Model

A few things to remember:

* An unlimited dimensions mean that all data for a point in that dimension are written together (record oriented).
* The order of dimensions in a variable matter -- outer dimension increments fastest.

Since a netCDF-3 data file with an unlimited dimension looks similar in structure to our DMSP data, we will better represent the DMSP data by making the unlimited dimension represent the number of records.
The visible and infrared data can be contained in two dimensional arrays:

~~~
 dimensions:
   numScans = UNLIMITED;   // (691 currently)
   numSamplesPerScan = 1465;
 variables:
   byte visibleImagery(numScans=691, numSamplesPerScan=1465);
   byte infraredImagery(numScans=691, numSamplesPerScan=1465);
~~~

The time and satellite ephemeris data:

~~~
   int year(numScans=691);
   int dayOfYear(numScans=691);
   double secondsOfDay(numScans=691);

   float satEphemLatitude(numScans=691);
   float satEphemLongitude(numScans=691);
   float satEphemAltitude(numScans=691);
   float satEphemHeading(numScans=691);
~~~
The remaining data from the dataset maps into the netCDF data model easily ({% include link_file.html file="/netcdfJava_tutorial/writingiosp/IospDmsp.cdl" text="resulting CDL" %}.

### Mapping the Data into a netCDF-3 Convention

Whether the information is in the data files or in the format specification, we need to gather enough information and map it into the netCDF file format to make the netCDF data view we are developing useful to other netCDF users. To accomplish this, it is important to select and follow appropriate conventions.

We will follow the basic attribute conventions from the netCDF Users Guide with the "long_name", "units", "scale_offset", and "add_offset" attributes. We will also follow the more recent Coordinate attribute convention which describes attributes for describing more general coordinate systems than supported by coordinate variables as defined in the netCDF Users Guide.

The variables above are not enough for existing coordinate axes conventions. So, we will calculate a more standard time coordinate as well as latitude and longitude for every pixel in the imagery:

~~~
   float time(numScans=691);
   float latitude(numScans=691, numSamplesPerScan=1465);
   float longitude(numScans=691, numSamplesPerScan=1465);
~~~   
   
#### Sample DMSP OIS Dataset 

Look at structure of the example dataset in ToolsUI:
* Start ToolsUI (webstart)
* In Viewer, open "/data/nj22/dmsp/F14200307192230.n.OIS"
* Select "infraredImagery" variable, then right-click, then select "Ncdump Data".
* Click on "Image".

Look at the example dataset as a grid:
* In ToolsUI, open the "Grid" tab
* Open the example dataset
* View Grid

### IOSP Details

The DMSP IOSP code is located in thredds/cdm/src/main/java/ucar/nc2/iosp/dmsp/DMSPiosp.java.
public boolean isValidFile(RandomAccessFile raf) throws IOException;
We identify that this is a DMSP OIS data file by reading some of the header record. A number of header items are checked including the "data set ID" information which contains a description of the dataset:
data set ID: DMSP F14 OLS LS & TS

#### public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException;

As with the lightning example, in this method we create the dimensions, attributes, and variables and add them to the empty NetcdfFile object. All of the information needed to construct these objects is contained in the header record.
It is important at this point to make sure that any needed conventions are followed in the attribute definitions:

~~~
(1)   curVariable.addAttribute( new Attribute( "long_name", curVarInfo.getLongName()));
    curVariable.addAttribute( new Attribute( "units", curVarInfo.getUnits()));

(2)   if ( curVariable.getName().equals( "latitude"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the geometry of the satellite scans and an ellipsoidal earth (a=6378.14km and e=0.0818191830)."));
      curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Lat.toString()));
      }
(3)   else if ( curVariable.getName().equals( "longitude"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the geometry of the satellite scans and an ellipsoidal earth (a=6378.14km and e=0.0818191830)."));
      curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Lon.toString()));
      }
(4)   else if ( curVariable.getName().equals( "time"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the satellite epoch for each scan."));
        this.startDateString = this.header.getStartDateAtt().getStringValue();
        try
        {
          this.startDate = DMSPHeader.DateFormatHandler.ISO_DATE_TIME.getDateFromDateTimeString( this.startDateString);
        }
        catch ( ParseException e )
        {
          throw new IOException( "Invalid DMSP file: \"startDate\" attribute value <" + this.startDateString +
                                 "> not parseable with format string <" + DMSPHeader.DateFormatHandler.ISO_DATE_TIME.getDateTimeFormatString() + ">.");
        }
      curVariable.addAttribute( new Attribute( "units", "seconds since " + this.startDateString));
      curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Time.toString()));
      }
(5)  else if ( curVariable.getName().equals( "infraredImagery"))
      {
     curVariable.addAttribute( new Attribute( _Coordinate.Axes, "latitude longitude"));
        curVariable.addAttribute( new Attribute( "_Unsigned", "true"));
        curVariable.addAttribute( new Attribute( "scale_factor", new Float((310.0-190.0)/(256.0-1.0))));
        curVariable.addAttribute( new Attribute( "add_offset", new Float( 190.0)));
        curVariable.addAttribute( new Attribute( "description",
                                                 "Infrared pixel values correspond to a temperature range of 190 to 310 " +
                                                 "Kelvins in 256 equally spaced steps. Onboard calibration is performed " +
                                                 "during each scan. -- From http://dmsp.ngdc.noaa.gov/html/sensors/doc_ols.html"));
      }
(6)   else if ( curVariable.getName().equals( "visibleImagery"))
      {
     curVariable.addAttribute( new Attribute( _Coordinate.Axes, "latitude longitude"));
        curVariable.addAttribute( new Attribute( "_Unsigned", "true"));
        curVariable.addAttribute( new Attribute( "description",
                                                 "Visible pixels are relative values ranging from 0 to 63 rather than " +
                                                 "absolute values in Watts per m^2. Instrumental gain levels are adjusted " +
                                                 "to maintain constant cloud reference values under varying conditions of " +
                                                 "solar and lunar illumination. Telescope pixel values are replaced by " +
                                                 "Photo Multiplier Tube (PMT) values at night. " +
                                                 "-- From http://dmsp.ngdc.noaa.gov/html/sensors/doc_ols.html"));
      }
~~~
      
1. Define "long_name" and "units" attributes for each variable.
2. For the "latitude" variable, define the "_CoordinateAxisType" attribute.
3. For the "longitude" variable, define the "_CoordinateAxisType" attribute.
4. For the "time" variable, define the "units" and "_CoordinateAxisType" attributes.
5. For the "infraredImagery" variable, define the "_CoordinateAxes" attribute

#### public Array readData( Variable v2, List section ) throws IOException, InvalidRangeException;

The readData() method is were we implement reading the data from disk.

1. For all the data read from the dataset, we use offset information to calculate the location in the RAF for each data point of the requested variable.
2. To simplify the implementation, we are reading all data for each requested variable, subsetting (if requested) is done using ucar.ma2.Array subsetting.
3. We calculate the time, latitude, and longitude from the existing time and satellite ephemeris data. The calculation of the data is postponed until the first request for these variables.
4. The time, latitude, and longitude data is cached since its calculation is expensive.

Reading all the variable data for any size request means that this will not scale well for larger datasets. A future implementation may move to reading subsets of the data.

### Data Type

#### Grid

~~~
The current implementation represents the image data as a grid with 2-D latitude and longitude coordinate variables by setting the "_CoordinateAxes" attribute for the visible and infrared variables to "latitude longitude":
   byte visibleImagery(numScans=691, numSamplesPerScan=1465);
     :_CoordinateAxes = "latitude longitude";
   byte infraredImagery(numScans=691, numSamplesPerScan=1465);
     :_CoordinateAxes = "latitude longitude";
~~~
     
This representation hides the time dependence of the data but allows current applications that read grids to understand this dataset.
