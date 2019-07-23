---
title: CDMRemote
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: cdmremote.html
---

## CDM Remote Web Service

CDM Remote is a web service implemented in the CDM library (client) and TDS (server), providing remote access to CDM datasets, using [ncstream](ncstream.html) as the on-the-wire protocol. It provides access at the NetcdfFile and FeatureDataset levels of the CDM API, so there are two levels of services:

1. <b>_CdmRemote_</b> provides index subsetting on remote CDM datasets, with the same functionality that <b>_NetcdfFile_</b> provides for CDM local datasets and <b>_DODSNetcdfFile_</b> provides for remote OPeNDAP datasets. <b>CdmRemote_</b> supports the full CDM data model.
2. <b>CDM Remote Feature_</b> provides coordinate subsetting on remote CDM Feature Datasets, with similar functionality to WCS and Unidata's experimental [NetCDF Subset Service (NCSS)](#subset_service.html). It provides a remote API for <b>_Point_</b> and <b>_StationTimeSeries_</b> Feature Datasets.

The CDM Remote services and protocol are experimental and should not be used outside of the CDM stack for now.

### CdmRemote Protocol (Data Access and Coordinate Systems)

The client forms requests of the form <b>_endpoint?query_</b>. The possible query parameters are:

~~~
  req=( CDL | NcML | capabilities | header | data)
  var=vars
where:
  vars := varspec | varspec[';' varspec]
  varspec := varname[subsetSpec]
  varname := escaped variable name
  subsetSpec := '(' fortran-90 arraySpec ')'

  fortran-90 arraySpec := dim | dim ',' dims
  dim := ':' | slice | start ':' end | start ':' end ':' stride
  slice := INTEGER
  start := INTEGER
  stride := INTEGER
  end := INTEGER
~~~
  
* Request parameter values are case-insensitive
* The var parameter is only used for data requests (req=data)
* The variable names are case-sensitive and must be <a href="cdm_objectnames.html#cdmremote"> backslash-escaped</a>
* Nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
* Optional components are enclosed between square braces '[' and ']'.

Example service requests are:

|---
| Request | 	Response
|:-:|:-:
| http://server:8080/thredds/cdmremote/data.nc?req=CDL |  dataset CDL  
| http://server:8080/thredds/cdmremote/data.nc?req=NcML |	dataset NcML 
| http://server:8080/thredds/cdmremote/data.nc?req=capabilities	 | capabilities XML 
|http://server:8080/thredds/cdmremote/data.nc?req=header  |	header ncstream message, contains the structural metadata
|http://server:8080/thredds/cdmremote/data.nc?req=data&var=Temp(0:99:10,0:127,:);lat;lon | 	data ncstream message

* Data request uses Section specification (same as Fortran-90 array notation) to ask for a subset in index space.
* Variable names are case-sensitive and must be <a href="cdm_objectnames.html#cdmremote"> backslash-escaped</a>
* Capabilities response indicates which (if any) feature types the dataset can be opened as, along with the URLs of those services. TBD.
* The protobuf messages are defined by <b>_thredds\cdm\src\main\java\ucar\nc2\stream\ncStream.proto_</b>.

### Client implementation

<b>_ucar.nc2.stream.CdmRemote_</b> is a subclass of NetcdfFile which provides index subsetting on remote CDM datasets. <b>_NetcdfDataset.openOrAcquireFile()_</b> looks for <b>_cdmremote:url_<b/> prefix, and calls new CdmRemote(url) if found. The url must be an endpoint for a cdmremote service.

* upon opening, req=header is called and the NetcdfFile objects are read from the response
* when data is requested, and the data is not already stored (immediate mode). then req=data is called

### Capabilities

~~~
<cdmRemoteCapabilities location="http://localhost:8080/thredds/cdmremote/testAll/testdata/2004050300_eta_211.nc">
  <featureDataset url="http://localhost:8080/thredds/cdmremote/testAll/testdata/2004050300_eta_211.nc" />
</cdmRemoteCapabilities>
~~~

### CdmrFeature Protocol (Point Feature API)

The client forms requests of the form <b>_endpoint?query_</b>. The possible query parameters are:

~~~
  req=( capabilities | data | form | stations)
  accept= (csv | xml | ncstream | netcdf )
  time_start,time_end=time range
  north,south,east,west=bounding box
  var=vars
  stn=stns

where:
  vars := varName | varName[,varName]
  stns := stnName | stnName[,stnName]
  varName := valid variable name
  stnName := valid station name
~~~

* request names are case-insensitive
* capabilities lists the possible accept values
* variable names are case-sensitive and must be UTF8-escape encoded
* station names are case-sensitive and must be UTF8-escape encoded
* literals are in bold

#### PointDataset

Possible requests are:

~~~
  req=( capabilities | data | form)
~~~
  
Example service requests are:

|---
| Request |	Response | Meaning
|:-:|:-:|:-:
| http://server:8080/thredds/cdmrfeature/data.nc/point?req=form	| HTML form	 | form interface
| http://server:8080/thredds/cdmrfeature/data.nc/point?req=capabilities	| capabilities XML	| describe dataset
| http://server:8080/thredds/cdmrfeature/data.nc/point?req=data	pointFeatureList | message	| get all the data
| http://server:8080/thredds/cdmrfeature/data.nc/point?req=data&north=40.3&south=22.8&east=-80&west=-105	| pointFeatureList message	| get data in bounding box
| http://server:8080/thredds/cdmrfeature/data.nc/point?req=data&time_start=&time_end=	| pointFeatureList message	| get data in time range
| http://server:8080/thredds/cdmfeature/data.nc/point?req=data&var=Temp,lat,lon	| pointFeatureList message	| get data for listed variables

* data requests return an unordered list of observation as StructureData
* The protobuf messages are defined by thredds\cdm\src\main\java\ucar\nc2\ft\point\remote\pointStream.proto

#### StationTimeSeries
Possible query parameters are:
~~~
  req=( capabilities | data | form | stations)
~~~

#### Example service requests are:

|---
| Request	| Response	| Meaning
|:-:|:-:|:-:
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=form	| HTML form	| form interface
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=capabilities	| capabilities XML	| describe dataset
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=stations	| stationListMessage	| get all stations
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=stations&north=40.3&south=22.8&east=-80&west=-105	| stationListMessage	| get stations in bounding box
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=stations&stn=KDEN,KLOG,MOAS	| stationListMessage	| get stations in list
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=data&time_start=&time_end=&stn=KDEN	| pointFeatureList message	| get data in time range
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=data&stn=KDEN,KLOG,MOAS	| pointFeatureList message	| get data for station list
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=data&north=40.3&south=22.8&east=-80&west=-105	| pointFeatureList message	| get data in bounding box
| http://server:8080/thredds/cdmrfeature/data.nc/station?req=data&var=Temp,lat,lon&stn=KDEN	| pointFeatureList message	| get data for listed variables

* a list of stations or a bounding box must always be provided (?)
* data requests return an unordered list of observation as StructureData
* The protobuf messages are defined by thredds\cdm\src\main\java\ucar\nc2\ft\point\remote\pointStream.proto

### Questions:

* stationList ncstream vs XML message

#### Client implementation

<b>_ucar.nc2.stream.CdmRemoteFeatureDataset.factory()_</b> makes an HTTP request to <b>_endpoint+"?req=capabilities"_</b> to obtain the GetCapabilities XML document. This document describes what feature type(s) the dataset supports, which determines what are the valid requests, along with the endpoints for those feature types, which may be different than the original endpoint.

<b>_FeatureDatasetFactoryManager.open()_</b> looks for <b>_cdmremote:url_</b> prefix, and calls <b>-CdmRemoteFeatureDataset.factory(url)_</b> if found. <b>_ThreddsDataFactory.openFeatureDataset()_</b> looks for catalog InvDatasets with <b>_featureType.isPointFeatureType()_</b> and <b>_ServiceType.CdmRemote_</b>, and if found, calls <b>_CdmRemoteFeatureDataset.factory()_</b>.

### GetCapabilities XML document

This is an ad-hoc format (for now), example:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<capabilities location="http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocalHome">
  <featureDataset type="station" url="http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocalHome/station" />
  <variable name="record.air_pressure_at_sea_level" type="float">
    <attribute name="long_name" value="Air pressure at sea level" />
    <attribute name="standard_name" value="air_pressure_at_sea_level" />
    <attribute name="_FillValue" type="float" value="-99999.0" />
    <attribute name="units" value="hectoPascal" />
  </variable>
  <variable name="record.air_temperature" type="float">
    <attribute name="long_name" value="Air temperature at 2 meters" />
    <attribute name="standard_name" value="air_temperature" />
    <attribute name="_FillValue" type="float" value="-99999.0" />
    <attribute name="units" value="Celsius" />
  </variable>
  ...

  <LatLonBox>
    <west>-180.0000</west>
    <east>180.0000</east>
    <south>-90.0000</south>
    <north>82.5199</north>
  </LatLonBox>
  <AcceptList>
    <accept>raw</accept>
    <accept>xml</accept>
    <accept>csv</accept>
    <accept>netcdf</accept>
    <accept>ncstream</accept>
  </AcceptList>
</capabilities>
~~~ 

### PointStream Grammer

An <b>_pointstream_</b> is a sequence of one or more messages:

~~~
   pointstream = {message}
   message = stationListMessage | pointFeatureListMessage | errorMessage | endMessage
   stationListMessage = MAGIC_StationList, vlen, PointStreamProto.StationList 
   pointFeatureListMessage = pointFeatureCollectionMessage, {pointFeatureMessage}, (endMessage | errorMessage)


   pointFeatureCollectionMessage = MAGIC_PointFeatureCollection, vlen, PointStreamProto.PointFeatureCollection
   pointFeatureMessage = MAGIC_PointFeature, vlen, PointStreamProto.PointFeature
   endMessage = MAGIC_END
   errorMessage = MAGIC_ERR, vlen, NcStreamProto.Error
   
   vlen = variable length encoded positive integer == length of the following object in bytes


   // 8 byte constants

   MAGIC_StationList            =  0fe, 0xec, 0xce, 0xda 
   MAGIC_PointFeatureCollection =  0xfi, 0xec, 0xce, 0xba 

   MAGIC_PointFeature           =  0xab, 0xec, 0xce, 0xba 
   MAGIC_END                    =  0xed, 0xed, 0xde, 0xde

   MAGIC_ERR                    =  0xab, 0xad, 0xba, 0xda 
~~~

The protobuf messages are defined by

* <b>_thredds\cdm\src\main\java\ucar\nc2\ft\point\remote\pointStream.proto_</b>
