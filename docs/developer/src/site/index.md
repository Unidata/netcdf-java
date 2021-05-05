---
title: NetCDF-Java introduction
last_updated: 2019-07-24
sidebar: developer_sidebar
permalink: index.html
toc: false
---

## What is the THREDDS Project?

The overarching goal of Unidata's Thematic Real-time Environmental Distributed Data Services (THREDDS) project is to provide students, educators, and researchers with coherent access to a large collection of real-time and archived datasets from a variety of environmental data sources at a number of distributed server sites.

## What is netCDF-Java?

The netCDF-Java library implements a [Common Data Model (CDM)](cdm_overview.html), a generalization of the netCDF, OpenDAP and HDF5 data models.
The netCDF-Java library is a 100% Java framework for _reading_ netCDF and other file formats into the CDM, as well as _writing_ to the netCDF-3 file format.
Writing to the netCDF-4 file format requires installing the [netCDF C library](../userguide/netcdf4_c_library.html){:target="_blank"}.
The netCDF-Java library also implements [NcML](/thredds/ncml/current/index.html){:target="_blank"}, which allows you to add metadata to CDM datasets, as well as to create virtual datasets through aggregation.
The [THREDDS Data Server (TDS)](https://www.unidata.ucar.edu/software/thredds/current/tds/){:target="_blank"} is built on top of the netCDF-Java library.

NetCDF-Java is Free and Open Source Software, and is hosted on [GitHub](https://github.com/unidata/netcdf-java){:target="_blank"}.
To build the latest stable version from source or contribute code to the netCDF-Java project, [see here](../userguide/building_from_source.html){:target="_blank"}.
Most projects use netcdfAll.jar or toolsUI.jar (download [here](https://www.unidata.ucar.edu/downloads/netcdf-java/){:target="_blank"}), or include the desired artifacts in their maven or gradle builds.
See [Using netCDF-Java Maven Artifacts](../userguide/using_netcdf_java_artifacts.html){:target="_blank"} for details.

As of version 5.0, netCDF-Java is released under the BSD-3 licence, which can be found can be found [here](https://github.com/Unidata/netcdf-java/blob/master/LICENSE){:target="_blank"}.

For information on how to cite netCDF-Java, please visit <https://www.unidata.ucar.edu/community/index.html#acknowledge>