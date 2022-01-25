---
title: CDM File Types
last_updated: 2021-05-20
sidebar: developer_sidebar
toc: false
permalink: file_types.html
---

The NetCDF-Java/CDM library provides a uniform API to many different scientific file formats and remote access protocols.
Generally, application programmers should respect the encapsulation of these formats.
When information is needed about the underlying file type, `NetcdfFile.getFileTypeId()`, `NetcdfFile.getFileTypeVersion()`, and `NetcdfFile.getFileTypeDescription()` methods can be called.

The **Id** must be unique and once registered, will never change, so that application code can test against it.
**Version** information should have a standard form for each file type, but the form may differ between file types.
The **Description** is human-readable and may present variations when appropriate, such as adding processing history, etc.
The **Reference URL**(s) in this table are informational, and may change as needed.

To support this functionality, `getFileTypeId()`, `getFileTypeVersion()`, and `getFileTypeDescription()` methods have been added to the IOServiceProvider interface as of version 4.0.46.
You will need to add these methods to your IOServiceProvider implementations. To use an IOSP implementation, you must include its module in your netCDF-java build.
For more information on including modules in your build, see [here](../userguide/using_netcdf_java_artifacts.html){:target="_blank"}.

To register your format/IOServiceProvider, or to send corrections and additions to this table, please send email to <support-netcdf-java@unidata.ucar.edu>.

| Id | Description | Module | 
| BUFR | WMO Binary Universal Form | `bufr` | 
| GeoTIFF | Georeferencing information embedded within a TIFF file | 'cdm-misc' |
| GRIB-1| WMO GRIB Edition 1 | `grib` | 
| GRIB-2 | WMO GRIB Edition 2 | `grib` | 
| HDF4 | Hierarchical Data Format, version 4 | `cdm-core` | 
| HDF5 | Hierarchical Data Format, version 5 | `cdm-core` | 
| netCDF | NetCDF classic format | `cdm-core` | 
| netCDF-4 | NetCDF-4 format on HDF-5 | `cdm-core` | 
| OPeNDAP | Open-source Project for a Network Data Access Protocol | `opendap` | 
| S3 | RandomAccessFile access to CDM datasets on object stores | `cdm-s3` | 
