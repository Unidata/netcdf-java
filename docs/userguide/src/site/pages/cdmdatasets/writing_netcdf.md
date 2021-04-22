---
title: Writing netCDF
last_updated: 2019-07-23
sidebar: userguide_sidebar
permalink: writing_netcdf.html
toc: false
---

## NetCDF File Writing (version 4.3+)

You can programmatically create, edit, and add data to *netCDF-3* and *netCDF-4* files, using `NetcdfFormatWriter`.
To copy an existing CDM dataset, you can use the [CDM nccopy application](cdm_utility_programs.html). 
By combining `nccopy` and NcML, you can copy just parts of an existing dataset, as well as [make modifications to it with NcML](ncml/indexhtml){:target="_blank"}.

### Requirements to Write netCDF files
CDM version 4.3 and above supports writing netCDF files. Writing netCDF-3 files is included in the core netCDF-Java API. 
 
To write to netCDF-4:
* include the `netcdf4` module in your build (see [here](using_netcdf_java_artifacts.html) for more information)
* install the [netCDF-4 C library](netcdf4_c_library.html) on your machine

#### Creating a new netCDF-3 file

To create a new netCDF-3 file, use `NetcdfFormatWriter.createNewNetcdf3`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&createNCFile %}
{% endcapture %}
{{ rmd | markdownify }}

The above example code produces a file that looks like:

~~~
netcdf C:/tmp/testWrite.nc {
 dimensions:
 lat = 64;
 lon = 128;
 svar_len = 80;
 names = 3;
 variables:
   double temperature(lat=64, lon=128);
    :units = "K";
    :scale = 1, 2, 3; // int
   char svar(svar_len=80);
   char names(names=3, svar_len=80);
   double scalar;
    
    // global attributes:
   :yo = "face";
   :versionD = 1.2; // double
   :versionF = 1.2f; // float
   :versionI = 1; // int
   :versionS = 2S; // short
   :versionB = 3B; // byte
   }
~~~

By default, The `fill` property is set to `false`. When `fill = true`, all values are written twice: first with the fill value, then with the data values. 
If you know you will write all the data, you do not need to use `fill`. If you don't know if all the data will be written, 
turning `fill` on ensures that any values not written will have the fill value. 
Otherwise, those values will be undefined: possibly zero, or possibly garbage. To enable `fill`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&setFillOption %}
{% endcapture %}
{{ rmd | markdownify }}

#### Open an existing file for writing

To open an existing CDM file for writing:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&openNCFileForWrite %}
{% endcapture %}
{{ rmd | markdownify }}

#### Writing data to a new or existing file

In both cases (new and existing files) the data writing is the same. 
The following examples demonstrate several ways to write data to an opened file.

1) Writing numeric data:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeDoubleData %}
{% endcapture %}
{{ rmd | markdownify }}

2) Writing char data as a String:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeCharData %}
{% endcapture %}
{{ rmd | markdownify }}

3) Writing a String array:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeStringArray %}
{% endcapture %}
{{ rmd | markdownify }}

4) Writing scalar data:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeScalarData %}
{% endcapture %}
{{ rmd | markdownify }}
 
5) Netcdf files may include unlimited (record) dimensions. To write one record at a time along the record dimentsion:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeRecordOneAtATime %}
{% endcapture %}
{{ rmd | markdownify }}

#### Writing to a netCDF-4 file with compression (version 4.5)

The main use of netCDF-4 is to get the performance benefits from compression, and possibly from chunking 
([why it matters](https://www.unidata.ucar.edu/blogs/developer/en/entry/chunking_data_why_it_matters)). 
By default, the Java library will use the default chunking algorithm to write chunked and compressed netcdf-4 files. 
To control chunking and compression settings, you must create a `Nc4Chunking` object and pass it into `NetcdfFormatWriter.createNewNetcdf4`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/WritingNetcdfTutorial.java&writeWithCompression %}
{% endcapture %}
{{ rmd | markdownify }}
  
See [here](netcdf4_c_library.html#writing-netcdf-4-files) for more details on Nc4Chunking.
