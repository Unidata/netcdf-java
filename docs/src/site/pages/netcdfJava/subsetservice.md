---
title: Netcdf Subset Service Reference
last_updated: 2019-11-05
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: subset_service.html
---

Please note that the interface described here is still a prototype, and subject to change.

## Contents:
* [Overview](#overview)
* [Rest URL's](#rest-urls)
* [Dataset Descriptions](#dataset-descriptions)
* [Subsetting Parameters (summary)](#subsetting-parameters-summary)
* [Subsetting types and use cases](#subsetting-types-and-use-cases)
    * [Spatial and coordinate subsetting](#spatial-and-coordinate-subsetting)
    * [Grid as point requests](#single-point-requests)
    * [Temporal subsetting and valid time ranges](#temporal-subsetting)
    * [Vertical coordinate subsetting](#variable-subsetting)
    * [Single variable requests](#single-variable-requests)
* [Reference](#reference)

## Overview
The NetCDF Subset Service (NCSS) is a web service for subsetting CDM scientific datasets. The subsetting is specified using earth coordinates, such as lat/lon or projection coordinates bounding boxes and date ranges, rather than index ranges that refer to the underlying data arrays. The data arrays are subsetted but not resampled or reprojected, and preserve the resolution and accuracy of the original dataset.

As of version 4.4.0 of TDS, NCSS supports spatial and temporal subsetting on collections of grids, stations and points. A Dataset is described by a Dataset Description XML document, which describes the dataset in enough detail to enable a programmatic client to form valid data requests.

NCSS may return netCDF binary files (using CF-1.6 when possible), XML, ASCII, or WaterML2 depending on the request and the dataset.

NCSS uses HTTP GET with key-value pairs (KVP) which may appear in any order. The service interface follows REST design, as well as Google/KML and W3C XML Schema Datatypes when applicable.

## REST URLs

~~~
http://{host}/{context}/{service}/{dataset}[/dataset.xml | /dataset.html | {?query}]

where
  {host}     	        = server name, eg thredds.ucar.edu
  {context} 	        = "thredds" (usually)
  {service} 	        = "ncss" (always)
  {dataset} 	        = logical path for the dataset, obtained from the catalog
  dataset.xml           = to get the dataset description in xml
  dataset.html          = to get the human-readable web form
  datasetBoundaries.xml = to get a human-readable description of the bounding boxes
  {?query}              = the KVPs to describe the subset that you want (see below for valid combinations)
~~~
Examples:
* <a href="http://thredds.ucar.edu/thredds/ncss/grib/NCEP/GFS/Global_0p5deg/best?north=47.0126&west=-114.841&east=-112.641&south=44.8534&time_start=present&time_duration=PT3H&accept=netcdf&var=v-component_of_wind_height_above_ground,u-component_of_wind_height_above_ground" target="_blank">ncss/grib/NCEP/GFS/Global_0p5deg/best?north=47.0126&west=-114.841&east=-112.641&south=44.8534&time_start=present&time_duration=PT3H&accept=netcdf&var=v-component_of_wind_height_above_ground,u-component_of_wind_height_above_ground</a>

## Dataset Descriptions

Each dataset has an XML document called the Dataset Description Document. These are intended to perform the same function as OGC GetCapabilities or Atom Introspection, that is, provide clients with the necessary information to formulate a valid request and send it to the server. The content of these documents is still evolving.

### Grid Dataset
A Grid Dataset is a collection of Grids which have horizontal (x,y) coordinates, and optional vertical and time coordinates. Grid data points next to each other in index space are next to each other in coordinate space.

Example gridDataset document (offline example)

### Station Dataset
Station datasets contain a collection of point features representing time-series of data points at named locations.

Example Station Dataset document (offline example)
Station datasets also support station list requests. This request will return all the stations within a specified bounding box. [NOTE: Broken as of 4.5.3. Will be fixed in 4.5.4.]

### Point Dataset
Point datasets contain a collection of point features which can be subset by space and time.

Example Point Dataset document (offline example)
 
## Subsetting Parameters (summary)

|---
| Parameter Name | Required | Description / possible values	 | Constraints | default
|:---|:--- |:--- |:--- |:---
|var	|yes	|Name of variables, separated by ',' (comma).	|Variables must be in the dataset description. Bounding box requests on grid datasets only support requests on variables with same vertical levels and time coordinates.	| None
|latitude, longitude |no	| Point location. units of degrees_east, degrees_north | 	Must be within the dataset bounding box(*). | None
|north, south, east, west	| no	| Lat/lon bounding box, units of degrees_east, degrees_north	| north > south,  if crossing 180 meridian, use east boundary > 180	|
|minx, miny, maxx, maxy	|no	|Projection bounding box, in projection coordinate units.	Only on grid datasets. min < max	
|horizStride	|no	|Take only every nth point (both x and y)	| Only on grid datasets.	| 1
|addLatLon	|no	|if present, make output strictly CF compliant by adding lat/lon coordinates if needed.	| Only on grid datasets, when writing to netCDF: make output strictly CF compliant.	false
|time	|no	|Time as a W3C date or "present". The time slice closest to the requested time is returned	| Must be a time within the dataset time range	present
|time_start, time_end, time_duration	|no	Used to specify a time range. Time as a W3C date or "present".  Duration as a W3C time duration.	| Two of time_start, time_end, time_duration must be present to define a valid time range. The provided time range must intersect the dataset time range	
|temporal	|no	|all	|Must be equal to "all" to have effect	
|timeStride	|no	|Take only every nth time in the available series	|Only on grid datasets.	|1
|vertCoord	|no	|	|Bounding box requests on grid datasets must have the same vertical levels.	|All the vertical levels will be returned
|accept	|no	|Used to specify the returned format. Grid: netCDF and netCDF4. Point: netCDF, netCDF4, XML, CSV, GEOCSV. Station: netCDF, netCDF4, XML, CSV, GEOCSV, WaterML2.|		|Grid: netCDF. Point: CSV. Station: CSV.
|subset	|no	|Used to specify the subsetting type on a station feature. subset=stns means we will provide a station list.	|Only on station datasets. Accepted values are stns and bb	|Default subset type is bounding box.
|stns	|no	|Used when subset=stns to specify the list of stations in the subset.	|Only on station datasets.	|

(*)Note the lat/lon bounding box declared in the dataset description is an approximated rectangle to the actual lat/lon boundaries so there may be valid points within the data but ouside of the declared bounding box in the dataset description.

## Variable subsetting:
For all types, you must specify a list of valid variable names.

### Horizontal spatial subsetting:
grids: you may specify a lat/lon bounding box or a projection bounding box. If none, return the entire horizontal grid.
grids as point: you must specify a point location with the latitude, longitude parameters.
stations: you may specify a lat/lon bounding box, a point location, or a list of stations. If none, do not filter.
points: you may specify a lat/lon bounding box. If none, do not filter.

### Vertical spatial subsetting:
grids: you may specify a vertical coordinate. If none, return all vertical levels

## Temporal subsetting:
For all types, you may specify a time range or a specific time. If none, return the time closest to the present.
A time range will request all features that intersect the range.
A time point will request the feature that is closest to that time.
If you include temporal=all, then return all times.
Output Format (accept parameter):
csv: Comma-separated values, one feature per line
geocsv: Comma-separated values, following the GeoCSV specification
xml: Collection of feature elements
netCDF: CF/NetCDF-3
netCDF4: CF/NetCDF-4 classic model
netCDF4ext: NetCDF-4 extended model
WaterML2: OGC WaterML 2.0 Timeseries

## Subsetting types and use cases
### Spatial and coordinate subsetting
NCSS provides two types of bounding boxes to subset the data:

Lat/lon bounding box: is specified with the params north, south, east and west. The north and south parameters are latitude values, and must be in units of degrees_north and lie between +/- 90. The east and west parameters are longitude values with units of degrees_east, and may be positive or negative, and will be interpreted modulo 360. The requested subset starts at the west longitude and goes eastward until the east longitude. Therefore, when crossing the dateline, the west edge may be greater than the east edge. For grids, if the underlying dataset is on a projection, the minimum enclosing projection bounding box will be calculated and used. The data contained in the intersection of this rectangle with the data is returned. To use, inspect the dataset.xml for the <LatLonBox> elements, which indicate the min and max extensions of the grid, for example:

~~~
<LatLonBox>
  <west>-153.5889</west>
  <east>-48.5984</east>
  <south>11.7476</south>
  <north>57.4843</north>
</LatLonBox>
~~~
        
Projection bounding box (only on grid datasets): is specified with the params minx, miny, maxx and maxy. These are projection coordinates in km on the projection plane; the data contained in the intersection of this rectangle with the data is returned. To use, inspect the dataset.xml for the <projectionBox> elements, which indicate the min and max extensions of the grid, for example:

~~~
  <gridSet name="time layer_between_two_pressure_difference_from_ground_layer y x">
    <projectionBox>
      <minx>-4264.248291015625</minx>
      <maxx>3293.955078125</maxx>
      <miny>-872.8428344726562</miny>
      <maxy>4409.772216796875</maxy>
    </projectionBox>
    ...
~~~
Note that the declared LatLonBox is an approximated rectangle to the full extension of the data and there may be points outside of the declared LatLonBox but within the data. So the provided lat/lon bounding box does not necesarily have to intersect it but it has to intersect the actual data.

By default, if no spatial subsetting is specified, the service returns all the features in the dataset.

Examples of query strings for spatial and coordinate subsetting:

* Lat/lon bounding box:

~~~
north=17.3&south=12.088&west=140.2&east=160.0
~~~

* Projection bounding box:

~~~
minx=-500&miny=-1600&maxx=500&maxy=0
~~~

## Single-point requests

The NetCDF Subset Service allows the user to extract data for a point of interest by specifying its latitude and longitude. The result differs depending on the underlying dataset.

* If it's a grid dataset, that means we're using the grid-as-point service. NCSS will find the grid cell in which the lat/lon falls and return its data as if it were a point feature. The supported output formats are netCDF, netCDF4, XML, and CSV
* If it's a station dataset, NCSS will return data for the station nearest the specified lat/lon. The supported output formats are netCDF, netCDF4, XML, CSV, and WaterML2
* Point datasets do not support single-point requests.

For example:

~~~
?req=station&var=temp&latitude=40.2&longitude=61.8
~~~

This finds the station nearest to (lat=40.2, lon=61.8) and returns its temperature data.

### Temporal subsetting and valid time ranges
There are several ways to do temporal subsetting requests:

* Default: If no temporal subseting is specified, the closest time to the current time is returned.
* All time range: A shorthand to request all the time range in a dataset is setting the parameter temporal=all. This can also be done by providing a valid temporal range containing all the dataset time range.
* One single time: Passing the parameter time will get the time slice closest to the requested time if it is within the time range of the dataset.
* Valid time range: A valid time range is defined with two of the three parameters: time_start, time_end and time_duration.
Times (time, time_start and time_end) must be specified as W3C date string or "present" and time_duration as a W3C time duration

Examples of time query strings with valid temporal ranges:

<b>time_start=2007-03-29T12:00:00Z&time_end=2007-03-29T13:00:00Z (between 12 and 1 pm Greenwich time)</b>
<b>time_start=present&time_duration=P3D (get 3 day forecast starting from the present)</b>
<b>time_end=present&time_duration=PT3H (get last 3 hours)</b>
<b>time=2007-03-29T12:00:00Z</b>
<b>time=present temporal=all</b>

## Vertical coordinate subsetting
Subsetting on the vertical axis of a variable or variables with the same vertical levels may be done with the vertCoord parameter.

By default, all vertical levels are returned.

## Single Variable requests
Note that these single variable requests can be easily extended to multivariable request by simply passing a comma separated list of variables in the var= parameter. Please note that for grid datasets, each variable in the request must have the same vertical levels.

Examples:
~~~
var	Spatial subset	Coordinate subset	Horizontal Stride	Time range	Temporal Stride	Vertical Coordinate	Query string
~~~
Request: "Give me all of the data for the variable Temperature_pressure for the closest time to the current time"

~~~
Temperature_pressure							?var=Temperature_pressure
~~~

Request: "Give me all of the data for the variable Temperature_pressure for all the available time range in the dataset"

~~~
Temperature_pressure				temporal=all			?var=Temperature_pressure&temporal=all
~~~

Request: "Give me all of the data for the variable Temperature_pressure available in a given time range"

~~~
Temperature_pressure				time_start=YYYY-MM-DDThh:mm:ss.sTZD
time_end=YYYY-MM-DDThh:mm:ss.sTZD (Using full temporal bounds)			?var=Temperature_pressure&time_start=YYYY-MM-DDThh:mm:ss.sTZD&time_end=YYYY-MM-DDThh:mm:ss.sTZD
~~~

Request: "Give me all of the data for the variable Temperature_pressure for a specific time"

~~~
Temperature_pressure				time=YYYY-MM-DDThh:mm:ss.sTZD			?var=Temperature_pressure&time=YYYY-MM-DDThh:mm:ss.sTZD
~~~

Request: "Give me the data for the variable Temperature_pressure over a given lat/lon bouding box for a specific time"

~~~
Temperature_pressure	north=41
west=-109.05
east==102.05
south=37			time=YYYY-MM-DDThh:mm:ss.sTZD			?var=Temperature_pressure&time=YYYY-MM-DDThh:mm:ss.sTZD&north=41&west=-109.05&east=-102.05&south=37
~~~

Request: "Give me variable Temperature_pressure for every 5th point on the grid (deltax=deltay=5)"

~~~
Temperature_pressure			5				?var=Temperature_pressure&horStride=5
~~~

Request: "Give me variable Temperature_pressure for every 5th point on the grid (deltax=deltay=5) over a given lat/lon bounding box"

~~~
Temperature_pressure	north=41
west=-109.05
east==102.05
south=37		5				?var=Temperature_pressure&north=41&west=-109.05&east=-102.05&south=37&horStride=5
~~~

Request: "Give me variable Temperature_pressure at a particular vertical level: 1000 mb*"

~~~
Temperature_pressure						1000	?var=Temperature_pressure&vertCoord=1000
~~~

Request: "Give me variable air_temperature for stations named LECO, LEST and LEVX "

~~~
air_temperature	subset=stns
stns=LECO,LEST,LEVX						?var=air_temperature&subset=stns&stns=LECO,LEST,LEVX
~~~
* note that the vertical level value must be in the same units used in the dataset - in this example we assume millibars but you will need to check the dataset description to be sure.

## Reference

### W3C Time Duration

The lexical representation for duration is the <a href="http://www.w3.org/TR/xmlschema-2/#ISO8601" target="_blank">ISO 8601</a> extended format _<b>PnYn MnDTnH nMnS</b>_, where nY represents the number of years, nM the number of months, nD the number of days, 'T' is the date/time separator, nH the number of hours, nM the number of minutes and nS the number of seconds. The number of seconds can include decimal digits to arbitrary precision.

The values of the Year, Month, Day, Hour and Minutes components are not restricted but allow an arbitrary unsigned integer, i.e., an integer that conforms to the pattern [0-9]+. Similarly, the value of the Seconds component allows an arbitrary unsigned decimal. According to <a href="http://www.w3.org/TR/xmlschema-2/#ISO8601" target="_blank">ISO 8601</a>, at least one digit must follow the decimal point if it appears. That is, the value of the Seconds component must conform to the pattern <b>[0-9]+(\.[0-9]+)?</b>. Thus, the lexical representation of duration does not follow the alternative format of § 5.5.3.2.1 in <a href="http://www.w3.org/TR/xmlschema-2/#ISO8601" target="_blank">ISO 8601</a>.

An optional preceding minus sign (-) is allowed, to indicate a negative duration. If the sign is omitted, a positive duration is indicated. See also <a href="https://www.w3.org/TR/xmlschema-2/#isoformats">ISO 8601 Date and Time Formats (§D)</a>.

For example, to indicate a duration of 1 year, 2 months, 3 days, 10 hours, and 30 minutes, one would write: <b>P1Y2M3DT10H30M</b>. One could also indicate a duration of minus 120 days as: -P120D.

Reduced precision and truncated representations of this format are allowed provided they conform to the following:

* If the number of years, months, days, hours, minutes, or seconds in any expression equals zero, the number and its corresponding designator ·may· be omitted. However, at least one number and its designator ·must· be present.
* The seconds part ·may· have a decimal fraction.
* The designator 'T' must be absent if and only if all of the time items are absent. The designator 'P' <b>must</b> always be present.

For example, P1347Y, P1347M, and P1Y2MT2H are all allowed, as are P0Y1347M and P0Y1347M0D.

P-1347M is not allowed although -P1347M is. P1Y2MT is not.

See XML Schema <a href="http://www.w3.org/TR/xmlschema-2/#duration">duration</a> for full details.

### W3C Dates

For our purposes, and ISO Date can be a dateTime or a date:

A dateTime has the form: '-'? yyyy '-' mm '-' dd 'T' hh ':' mm ':' ss ('.' s+)? (zzzzzz)?

where:

* <b>'-'? yyyy</b> is a four-or-more digit, optionally negative-signed numeral that represents the year; if more than four digits, leading zeros are prohibited, and '0000' is prohibited (see the Note above (§3.2.7); also note that a plus sign is not permitted);
* the remaining <b>'-</b>'s are separators between parts of the date portion;
* the first <b>mm</b> is a two-digit numeral that represents the month;
* <b>ddM</b> is a two-digit numeral that represents the day;
* <b>'T'</b> is a separator indicating that time-of-day follows;
* <b>hh</b> is a two-digit numeral that represents the hour; '24' is permitted if the minutes and seconds represented are zero, and the dateTime value so represented is the first instant of the following day (the hour property of a dateTime object in the <a href="http://www.w3.org/TR/xmlschema-2/#dt-value-space">value space</a> cannot have a value greater than 23);
* ':' is a separator between parts of the time-of-day portion;
* the second <b>mm</b> is a two-digit numeral that represents the minute;
* <b>ss</b> is a two-integer-digit numeral that represents the whole seconds;
* <b>'.' s+</b> (if present) represents the fractional seconds;
* <b>zzzzzz</b> (if present) represents the time zone (as described below).
For example, 2002-10-10T12:00:00-05:00 (noon on 10 October 2002, Central Daylight Savings Time as well as Eastern Standard Time in the U.S.) is 2002-10-10T17:00:00Z, five hours later than 2002-10-10T12:00:00Z.

A date is the same as a dateTime without the time part : <b>'-'? yyyy '-' mm '-' dd zzzzzz?</b>

See XML Schema <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">dateTime</a> and <a href="http://www.w3.org/TR/xmlschema-2/#date">date</a> for full details