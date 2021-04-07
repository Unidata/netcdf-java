---
title: CDM File Types
last_updated: 2018-10-10
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
For more information on including modules in your build, see [here](using_netcdf_java_artifacts.html).

To register your format/IOServiceProvider, or to send corrections and additions to this table, please send email to <support-netcdf-java@unidata.ucar.edu>.

| Id | Description | Module | Reference URL
| BUFR | WMO Binary Universal Form | `bufr` | <http://www.wmo.int/pages/prog/www/WMOCodes/OperationalCodes.html/> |
| CINRAD | Chinese Level-II Base Data | `cdm-radial` | <http://www.cinrad.com/> |
| DMSP | Defense Meteorological Satellite Program | `cdm-misc` | <http://dmsp.ngdc.noaa.gov/> |
| DORADE | DOppler RAdar Data Exchange Format | `cdm-radial` | <http://www.eol.ucar.edu/rsf/UserGuides/SABL/DoradeFormat/DoradeFormat.html> <http://www.eol.ucar.edu/instrumentation/airborne-instruments/eldora/> |
| F-TDS | Ferret I/O Service Provider and Server-side Analysis | `cdm-core` | <http://ferret.pmel.noaa.gov/LAS/documentation/the-ferret-thredds-data-server-f-tds> |
| FYSAT | Chinese FY-2 satellite image data in AWX format | `cdm-image` | <http://satellite.cma.gov.cn/> |
| GempakGrid| GEMPAK Gridded Data | `cdm-mcidas` | <https://www.unidata.ucar.edu/software/gempak/> |
| GempakSurface | GEMPAK Surface Obs Data | `cdm-mcidas` | <https://www.unidata.ucar.edu/software/gempak/> |
| GINI | GOES Ingest and NOAAPORT Interface | `cdm-image` | <http://weather.unisys.com/wxp/Appendices/Formats/GINI.html> |
| GRIB-1| WMO GRIB Edition 1 | `grib` | <http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB1-Contents.html> |
| GRIB-2 | WMO GRIB Edition 2 | `grib` | <http://www.wmo.ch/pages/prog/www/WMOCodes/Guides/GRIB/GRIB2_062006.pdf> |
| GTOPO | USGS GTOPO digital elevation model | `cdm-misc` | <http://edc.usgs.gov/products/elevation/gtopo30/gtopo30.html> |
| HDF4 | Hierarchical Data Format, version 4 | `cdm-core` | <http://www.hdfgroup.org/products/hdf4/> |
| HDF5 | Hierarchical Data Format, version 5 | `cdm-core` | <http://www.hdfgroup.org/HDF5/> |
| McIDASArea| McIDAS area file | `cdm-mcidas` | <http://www.ssec.wisc.edu/mcidas/doc/misc_doc/area2.html> |
| McIDASGrid | McIDAS grid file | `cdm-mcidas` | <http://www.ssec.wisc.edu/mcidas/doc/prog_man/2006>  <http://www.ssec.wisc.edu/mcidas/doc/prog_man/2006/formats-20.html#22077> |
| netCDF | NetCDF classic format | `cdm-core` | <https://www.unidata.ucar.edu/software/netcdf/index.html> |
| netCDF-4 | NetCDF-4 format on HDF-5 | `netcdf4` | <https://www.unidata.ucar.edu/software/netcdf/index.html> |
| NEXRAD-2 | NEXRAD Level-II Base Data | `cdm-radial` | <http://www.ncdc.noaa.gov/oa/radar/radarresources.html> <http://www.tsc.com/SETS/_3TDWR.htm> |
| NEXRAD-3 | NEXRAD Level-III Products | `cdm-radial` | <http://www.ncdc.noaa.gov/oa/radar/radarresources.html> |
| NLDN | National Lightning Detection Network | `cdm-misc` | <http://www.vaisala.com/weather/products/aboutnldn.html> |
| NMCon29 | NMC Office Note 29 | `cdm-misc` | <http://www.emc.ncep.noaa.gov/mmb/data_processing/on29.htm/> |
| OPeNDAP | Open-source Project for a Network Data Access Protocol | `opendap` | <http://opendap.org/> |
| S3 | RandomAccessFile access to CDM datasets on object stores | `cdm-s3` | <https://docs.unidata.ucar.edu/netcdf-java/5.4/userguide/dataset_urls.html#object-stores>
| SIGMET | SIGMET-IRIS weather radar | `cdm-radial` | <http://www.vaisala.com/en/defense/products/weatherradar/Pages/IRIS.aspx> |
| UAMIV | CAMx UAM-IV formatted files | `cdm-radial` | <http://www.camx.com/> |
| UniversalRadarFormat | Universal Radar Format | `cdm-radial` | <ftp://ftp.sigmet.com/outgoing/manuals/program/cuf.pdf> |
| USPLN | US Precision Lightning Network | `cdm-misc` | <http://www.uspln.com/> |
| VIS5D | Vis5D grid file | `cdm-vs5d` | <http://www.ssec.wisc.edu/~billh/vis5d.html>