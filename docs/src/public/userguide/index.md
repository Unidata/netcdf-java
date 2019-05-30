---
title: Introduction to the THREDDS Project and netCDF-Java
permalink: index.html
layout: page
---

The overarching goal of Unidata's Thematic Real-time Environmental Distributed Data Services (THREDDS) project is to provide students, educators, and researchers with coherent access to a large collection of real-time and archived datasets from a variety of environmental data sources at a number of distributed server sites.

The NetCDF-Java library implements a Common Data Model (CDM), a generalization of the NetCDF, OpenDAP and HDF5 data models.
The library is a prototype for the NetCDF-4 project, which provides a C language API for the "data access layer" of the CDM, on top of the HDF5 file format.
The NetCDF-Java library is a 100% Java framework for reading netCDF and other file formats into the CDM, as well as writing to the netCDF-3 file format.
Writing to the netCDF-4 file format requires installing the netCDF C library.
The NetCDF-Java library also implements NcML, which allows you to add metadata to CDM datasets, as well as to create virtual datasets through aggregation.
The THREDDS Data Server (TDS) is built on top of the NetCDF-Java library.
