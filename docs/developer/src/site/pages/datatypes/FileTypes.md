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

| Id | Description | Module | Reference URL |
| BUFR | WMO Binary Universal Form | `bufr` | <http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html/> |
| GRIB-1| WMO GRIB Edition 1 | `grib` | <http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB1-Contents.html> |
| GRIB-2 | WMO GRIB Edition 2 | `grib` | <http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB2_062006.pdf> |
| HDF4 | Hierarchical Data Format, version 4 | `cdm-core` | <http://www.hdfgroup.org/products/hdf4/> |
| HDF5 | Hierarchical Data Format, version 5 | `cdm-core` | <http://www.hdfgroup.org/HDF5/> |
| netCDF | NetCDF classic format | `cdm-core` | <https://www.unidata.ucar.edu/software/netcdf/index.html> |
| netCDF-4 | NetCDF-4 format on HDF-5 | `cdm-core` | <https://www.unidata.ucar.edu/software/netcdf/index.html> |
| OPeNDAP | Open-source Project for a Network Data Access Protocol | `opendap` | <http://opendap.org/> |
| S3 | RandomAccessFile access to CDM datasets on object stores | `cdm-s3` | <https://docs.unidata.ucar.edu/netcdf-java/5.4/userguide/dataset_urls.html#object-stores> |
