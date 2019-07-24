## Overview

The netCDF-Java library implements a [Common Data Model (CDM)](common_data_model_overview.html), a generalization of the netCDF, OpenDAP and HDF5 data models.
The netCDF-Java library is a 100% Java framework for _reading_ netCDF and other file formats into the CDM, as well as _writing_ to the netCDF-3 file format.
[Writing to the netCDF-4 file format](netcdf4_c_library.html) requires installing the netCDF C library.
The netCDF-Java library also implements [NcML](ncml_overview.html), which allows you to add metadata to CDM datasets, as well as to create virtual datasets through aggregation.
The [THREDDS Data Server (TDS)](https://www.unidata.ucar.edu/software/thredds/current/tds/){:target="_blank"} is built on top of the netCDF-Java library.

NetCDF-Java is Free and Open Source Software, and is hosted on [GitHub](https://github.com/unidata/netcdf-java){:target="_blank"}.
To build the latest stable version from source or contribute code to the netCDF-Java project, [see here](building_from_source.html).
Most projects use netcdfAll.jar or toolsUI.jar (download [here](https://www.unidata.ucar.edu/downloads/netcdf/netcdf-java/index.jsp){:target="_blank"}), or include the desired artifacts in their maven or gradle builds.
See [Using netCDF-Java Maven Artifacts](using_netcdf_java_artifacts.html) for details.