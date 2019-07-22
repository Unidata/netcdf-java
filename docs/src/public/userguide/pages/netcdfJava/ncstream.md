---
title: NetCDF Streaming Format (Experimental)
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar 
permalink: ncstream.html
toc: false
---

## Overview
NetCDF Streaming Format (ncstream) is a write-optimized encoding of CDM datasets. Ncstream consists of a series of header and data messages, in any order. Writes are always appended. Later messages override earlier ones whenever they overlap or conflict. To add or modify structural metadata, simply append a new header message. Each data message identifies the variable and the section (rectangular subset) of data it contains. A variable's data thus consists of the collection of data messages for it, if any.

### Design Goals
* Experiment with our own on-the-wire protocol to rapidly explore new ideas for remote data access.
* Experiment with ways to optimize subset extraction of large datasets, especially for the case when the subset is specified in coordinate space rather than index space.
* Must be easy to read ncstream and write netCDF-3 or netCDF-4 files.
& The information content / data model of ncstream is identical to CDM. So ncstream is an alternate encoding of CDM datasets.

### Possible uses

* Study OPeNDAP implementation. The java implementation of ncstream should be (close to) maximal I/O efficient. By comparing the same requests on the same server to OPeNDAP, we should be able to tell how much speedup a rewrite of the OPeNDAP software will yield.
* Augment/explore OPeNDAP. Where the DAP protocol is deficient, ncstream is a way for us to explore possible solutions.
* Augment/explore WCS. We are setting up experimental data services on thredds.ucar.edu that allow coordinate subsetting on Feature datasets. We can use ncstream to get these services working, offering much the same functionality as WCS. Implementing WCS services are then much easier to develop. Experience using ncstream can potentially be fed into WCS standards. This work has already been going on under the name of "NetCDF Subset Service", and ncstream will be offered as one of the output types.
* Explore asynchronous data services. Both OPeNDAP and WCS want asynchronous data services, for the case when requested data cannot be immediately returned, for example because of extensive computation or because the data resides in a tape silo. We can explore possible solutions by having our own remote access protocol.
* Explore alternate file format for CDM datasets. Certain applications may need some of the extended features of CDM / Netcdf-4, without needing all of the complexity of the HDF5 format. Ncstream may comprise a "sweet spot" of functionality for some use cases.
* Parallel I/O. Append-only file writing may be useful in some high performance applications, with a second pass (external to the generating program) that efficiently converts to netCDF-3 or netCDF-4 files. During that conversion, a smart program could decide on chunking parameters, data-dependent compression choices, and other read optimizations.

### Implementation

Messages are encoded using Google's <a href="http://code.google.com/p/protobuf/">Protobuf library</a>.

"Protocol buffers are a flexible, efficient, automated mechanism for serializing structured data – think XML, but smaller, faster, and simpler. You define how you want your data to be structured once, then you can use special generated source code to easily write and read your structured data to and from a variety of data streams and using a variety of languages. You can even update your data structure without breaking deployed programs that are compiled against the "old" format."

The main advantage of protobuf over XML is performance, since both message size and parsing speed is improved. A very important feature of protobuf is the ability to evolve the message structure in a way that doesn't break previous code.

We don't use protobuf messages for the data, since protobuf messages are built in memory, and we need to be able to stream (write data directly from its source onto the output stream, eg socket). The multidimensional data is written to the stream in row-major order, with reader-makes-right byte ordering. A data message identifying the variable and the section that the data represents is part of every data message.

Variable length datatypes like String and Opaque use the vdataMessage for data transfer. First the number of objects is written, then each object, preceded by its length in bytes as a vlen. Strings are encoded as UTF-8 bytes. Opaque is just a bag of bytes.

Ncstream implements the full [CDM data model](common_data_model_overview.html).

See also:

[ncstream grammar](ncstream_grammar.html)

[CDM Remote Web service](cdmremote.html)