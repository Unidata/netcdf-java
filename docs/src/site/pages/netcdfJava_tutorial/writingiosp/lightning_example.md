---
title: Writing an IOSP - Example
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar
permalink: lightning_example.html
toc: false
---

## Writing an IOSP for Netdf-Java (version 4+)

We will work on an example {% include link_file.html file="/netcdfJava_tutorial/writingiosp/lightningData.txt" text="lightning data test file" %}, which looks like:

~~~
USPLN-LIGHTNING,2006-10-23T18:01:00,2006-10-23T18:01:00
2006-10-23T17:59:39,18.415434,-93.480526,-26.8,1
2006-10-23T17:59:40,5.4274766,-71.2189314,-31.7,1
2006-10-23T17:59:44,9.3568365,-76.8001513,-34.3,1
...
~~~

This is a text file, with variable length lines. We won't worry much about the nuances of the data, we just need to know that there are occasional header lines 
starting with `USPLN-LIGHTNING`, and a separate line for each lightning strike, with comma delimited fields. The fields are:

1. date of strike (GMT)
2. latitude
3. longitude
4. intensity
5. number of strokes

We will walk through implementing methods in a new IOSP: 
{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&getIOSP %}
{% endcapture %}
{{ rmd | markdownify }}

*Note:* that we have already implemented three methods: `isBuilder` returns `true`, `getFileTypeId` returns a String identifier, 
and `getFileTypeDescription` returns a description of our IOSP. 

#### Implementing `isValidFile`

First, we must identify our files. It's not foolproof, but we will assume that all our files start with the exact String `USPLN-LIGHTNING`, 
so our `isValidFile` function can look like this:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&implementIsValidFile %}
{% endcapture %}
{{ rmd | markdownify }}

#### Reading a file

To implement our `build` method, we will have to add all the `Variable`, `Attributes` and `Dimensions` to the empty `NetcdfFile` object. 
The `Dimensions` have to have the actual lengths in them, so we need to find out how many strike records there are. 
Since these are variable length records, we have no choice but to read through the entire file. So we start with creating a private method to do so. 
We will ignore the occasional header records, and place each strike into a `Strike` object:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&readALlData %}
{% endcapture %}
{{ rmd | markdownify }}

#### Implementing the `build` method

Now we can populate the empty `Group.Builder` with the necessary objects in our `build` method, as follows:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&implementBuild %}
{% endcapture %}
{{ rmd | markdownify }}

#### Implementing `read` methods

At this point we need to figure out how to implement the `read` methods. Since we have no `Structures`, we can ignore `readNestedData`. 
Of course, you are probably saying "we already read the data, are we just going to throw it away?". 
So for now, lets suppose that we have decided that these are always small enough files that we can safely read the entire data into memory. 
This allows us to create the data arrays during the open and cache them in the `Variable`.  
 
First we'll create some instance fields to hold our read data, one for each netCDF `Variable`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&createDataArrays %}
{% endcapture %}
{{ rmd | markdownify }}

The additional code in for our `readAllData` method looks like:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&implementReadMethods %}
{% endcapture %}
{{ rmd | markdownify }}

*Note:* Once we return from this method, the ArrayList of records and the Strike objects themselves are no longer used anywhere, 
so they will get garbage-collected. So we don't have the data taking twice as much space as needed.

#### Caching the read data
{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&setSourceData %}
{% endcapture %}
{{ rmd | markdownify }}
   
The method `setSourceData` sets the data array for a `Variable`. It must be the complete data array for the `Variable`, with the correct type and shape. 
Having set this, the `read` method will never be called for that `Variable`, it will always be satisfied from the source data `Array`. 
If all `Variables` have source data, then the `read` method will never be called, so we don't need to implement it.

#### Adding Coordinate Systems and Typed Dataset information

AS an `IOServiceProvider` implementer, you presumably know everything there is to know about this data file. 
If you want your data file to be understood by the higher layers of the CDM, you should also add the coordinate system and typed dataset information that is needed. 
To do so, you need to understand the Conventions used by these layers. In this case, we have Point data, so we are going to use Unidata's _Coordinate Conventions and 
Unidata's Point Observation Conventions which requires us to add certain attributes. The payoff is that we can then look at our data through the Point tab of the ToolsUI.

The additional code in the `build` method looks like this:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&addCoordSystemsAndTypedDatasets %}
{% endcapture %}
{{ rmd | markdownify }}


#### Register your IOSP
We now have not only a working IOSP, but a `PointObsDataset` that can be displayed and georeferenced! You will need to register your IOSP. 
The easiest way is to load it at runtime (e.g. on application spin-up) as follows:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/LightningExampleTutorial.java&registerIOSP %}
{% endcapture %}
{{ rmd | markdownify }}

For more information on registering an IOSP, see the [runtime loading](runtime_loading.html#register-an-ioserviceprovider)

Once your IOSP is registered, calling `NetcdfFiles.open` or `NetcdfDatasets.open` on the provided [data file](netcdfJava_tutorial/writingiosp/lightningData.txt) 
will use your implementation.