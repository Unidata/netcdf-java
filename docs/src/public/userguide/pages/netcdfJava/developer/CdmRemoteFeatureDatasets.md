---
title: CDMRemote Feature Datasets
last_updated: 2018-10-10
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: cdmremote_feature_datasets.html
---

### CDM Remote Web Service

[CDM Remote](cdmremote.html) is a web service implemented in the CDM library (client) and TDS (server), providing remote access to CDM datasets, using ncstream as the on-the-wire protocol. An experimental python client is under development. It provides access at the NetcdfFile and FeatureDataset levels of the CDM API, so there are two levels of services:

1. CDM Remote provides index subsetting on remote CDM datasets, with the same functionality that NetcdfFile provides for CDM local datasets and DODSNetcdfFile provides for remote OPeNDAP datasets. CdmRemote supports the full CDM data model.

2. CDM Remote Feature provides coordinate subsetting on remote CDM Feature Datasets, with similar functionality to OGC’s Web Coverage Service and the exact functionality of Unidata’s NetCDF Subset Service (NCSS).

* CDM Remote Feature for Coverage/Grid Data provides coordinate subsetting on remote CDM Coverage (Grid) Feature Datasets
* CDM Remote Feature for Point Data provides coordinate subsetting on remote CDM Point (DSG) Feature Datasets

This API and protocol is available in TDS/CDM version >= 5.0.

CDM Remote Protocol (Index space Data Access)

The client sends requests of the form endpoint?query. The possible query parameters are:

~~~
  'req'=( 'CDL' | 'NcML' | 'header' | 'data')
  'var'=vars
  'deflate'=0..9
where:
  deflate := deflate at specified level
  vars := varspec | varspec[';' varspec]
  varspec := varname[subsetSpec]
  varname := backslash escaped variable name
  subsetSpec := '(' fortran-90 arraySpec ')'

  fortran-90 arraySpec := dim | dim ',' dims
  dim := ':' | slice | start ':' end | start ':' end ':' stride
  slice := INTEGER
  start := INTEGER
  stride := INTEGER
  end := INTEGER
~~~

endpoint is the URL of a cdmremote service provided by the THREDDS Data Server (TDS).

Request parameter values are case-insensitive

The deflate and var parameters are only used for data requests (req=data)

Setting deflate requests that the server compress the data at the specified deflate level. The returned message indicates if this was done or not.

### Example service requests

|---
| Request | Response
:- |:-
| http://server:8080/thredds/cdmremote/data.nc?req=CDL | dataset CDL
| http://server:8080/thredds/cdmremote/data.nc?req=NcML | dataset NcML
| http://server:8080/thredds/cdmremote/data.nc?req=header | ncstream header message
| http://server:8080/thredds/cdmremote/data.nc?req=data&var=Temp(0:99:10,0:127,:);lat;lon | ncstream data message

* CDL and NcML outputs are human-friendly listings of the CDM dataset’s structural metadata, familiar to users of netCDF.

* The ncstream header and ncstream data messages are for software consumption only.

* A data request uses the Section specification (same as Fortran-90 array notation) to ask for a subset in index space.

* Variable names are case-sensitive and must be backslash-escaped

### Client libraries

#### Java

<b>_ucar.nc2.stream.CdmRemote_</b> is a subclass of <b>_NetcdfFile_</b> which provides index subsetting on remote CDM datasets. <b>_NetcdfDataset.openOrAcquireFile()_</b> sees a cdmremote:url prefix, and instantiates a <b>_CdmRemote object_</b>. The url must be an endpoint for a cdmremote service. ToolsUI now preferentially uses cdmremote service when it is available.

* upon opening, <b>_req=header_</b> is called and the NetcdfFile objects are read from the response

* when data is requested, and the data are not already stored locally, then <b>_req=data_</b> is called.

* from the client POV, the dataset is exactly the same as any other <b>_NetcdfFile_</b>.

CDM datasets can also be serialized into ncstream messages and stored in a disk file, and then read as a "normal" netCDF file by the Netcdf-Java library. In this sense ncstream becomes another format for NetCDF files, alongside netCDF-3 and netCDF-4.

* <b>_CdmRemote.writeToFile()_</b> will serialize an entire remote dataset to a file.

* <b>_ucar.nc2.stream.NcStreamIosp_</b> is a Netcdf-Java IOSP that can read ncstream disk files.

#### Python

coming soon