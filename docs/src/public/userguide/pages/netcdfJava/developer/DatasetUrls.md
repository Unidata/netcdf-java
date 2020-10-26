---
title: Dataset URLs
last_updated: 2020-04-06
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: dataset_urls.html
---

The netCDF-Java library can read datasets from a variety of sources.
The dataset is named using a Uniform Resource Location (URL).
This page summarizes the netCDF-Java API use of URLs.


Special Note: When working with remote data services, it's important to note that not all servers handle encoded URLs.
By default, netCDF-Java will encode illegal URI characters using percent encoding (e.g. `[` will become `%5B`).
If you find you are having trouble accessing a remote dataset due to the encoding, set the java System Property `httpservices.urlencode` to `"false"` using, for example `System.setProperty("httpservices.urlencode", "false");`.

## `ucar.nc2.NetcdfFile.open(String location)`

### Local Files

`NetcdfFile` can work with local files, e.g:

* `/usr/share/data/model.nc`
* `file:/usr/share/data/model.nc`
* `file:C:/share/data/model.nc` (NOTE we advise using forward slashes **everywhere**, including Windows)
* `data/model.nc` (relative to the current working directory)

When using a file location that has an embedded `:` char, eg `C:/share/data/model.nc`, it\'s a good idea to add the `file:` prefix, to prevent the `C:` from being misinterpreted as a URL schema.

### Remote Files

#### HTTP
`NetcdfFile` can open HTTP remote files, [served over HTTP](read_over_http.html), for example:

* https://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.nc

The HTTP server must implement the getRange header and functionality.
Performance will be strongly affected by file format and the data access pattern.

To disambiguate HTTP remote files from OPeNDAP or other URLS, you can use `httpserver:` instead of `http:`, e.g.:

* `httpserver://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.nc`

#### Object Stores

`NetcdfFiles` and `NetcdfDatasets` can open files stored as a single objects on any Object Store that supports the AWS RESTful API with byte range-requests, similar to HTTP.
This new functionality is not available in the now deprecated `NetcdfFile` and `NetcdfDataset` open methods.
You will also need to include the `cdm-s3` artifact in your build (visit the [netcdf-java artifact guide](using_netcdf_java_artifacts.html) for details).
Currently, this is not part of `netcdfAll.jar`.
netCDF-Java implements a custom URI for identifying objects in an Object Store.
Using the generic URI syntax from <a href="https://tools.ietf.org/html/rfc3986">RFC3986</a>, the CDM will identify resources located in an object store as follows:
* scheme (**required**): defined to be cdms3
* authority (**optional for AWS S3, otherwise required**): If present, the authority component is preceded by a double slash ("//") and is terminated by the next slash ("/").
  As with the generic URI syntax, the authority is composed of three parts:
    * authority = `[ userinfo "@" ] host [ ":" port ]`
       * userinfo (**optional**): name of the profile to be used by the AWS SDK
       * host (**required**): host name of the object store
          Note: If you need to supply a profile name when accessing an AWS S3 object, you must use the generic host name AWS in order to have a valid URI.
       * port(**optional**): default: 443
* path (**required**): path associated with the bucket
  * may not be empty.
  * the final path segment is interpreted to be the name of the object stores bucket.
* query (**required**): full or partial object key
  * Only full keys can be used to read an object through the netCDF-Java API.
  * Partial keys are treated as prefixes, and are used by netCDF-Java when, for example, performing bucket listing operations.
* fragment (**optional**): configuration options
  * Configuration options may be passed in through fragment on the CDM S3 URI.
  * Currently, only one configuration option is available and is used to describe a delimiter for keys that have been designed to be hierarchical.
    A commonly encountered case is that the object keys are the same as the file path on the system from which they were uploaded.
    In this case, the delimiter might be the "/" character.
    If the fragment is not used, netCDF-Java will assume there is no hierarchical structure to the object keys.

Example `cdms3` URIs (Any S3 compatible Object Store):
* cdms3://profile_name@my.endpoint.edu/endpoint/path/bucket-name?super/long/key#delimiter=/
* cdms3://profile_name@my.endpoint.edu/bucket-name?super/long/key#delimiter=/
* cdms3://my.endpoint.edu/endpoint/path/bucket-name?super/long/key#delimiter=/
* cdms3://my.endpoint.edu/bucket-name?super/long/key#delimiter=/

Secure HTTP access is assumed by default.
Insecure HTTP access is attempted when of the following ports is explicitly referenced in the authority portion of the `cdms3` URI:
* 80
* 8008
* 8080
* 7001 (used by WebLogic)
* 9080 (used by WebSphere)
* 16080 (used by Mac OS X Server)

##### Credentials

netCDF-Java uses the AWS SDK to manage credentials, even for non-AWS object stores.
One method for supplying credentials is through the use of a special credentials file, in which named profiles can be used to manage multiple sets of credentials.
References to `profile_name` in the above examples corresponds to a named profile in an AWS credentials file.
The default credentials file is located in your home directory at `<home-dir>/.aws/credentials`.
The `aws.sharedCredentialsFile` Java System property can be used to define a different credentials file, for example:

~~~java
System.setProperty("aws.sharedCredentialsFile", "C:/Users/me/mycredfile");
try (NetcdfFile ncfile = NetcdfFiles.open(AWS_G16_S3_URI_FULL)) {
  ...
} finally {
  System.clearProperty(AWS_SHARED_CREDENTIALS_FILE_PROP);
}
~~~

The format of the credentials file is:

~~~
[default]
aws_access_key_id={DEFAULT_ACCESS_KEY_ID}
aws_secret_access_key={DEFAULT_SECRET_ACCESS_KEY}

[profile-name1]
aws_access_key_id={PROFILE_NAME1_ACCESS_KEY_ID}
aws_secret_access_key={PROFILE_NAME1_SECRET_ACCESS_KEY}
region=us-east-1

[region-only-profile]
region=us-gov-west-1
~~~

The `aws_access_key_id` and `aws_secret_access_key` parameters are used to define your credentials, even for non-AWS S3 Object Store systems.
Note that an AWS region can be set for a given profile in this same file.
For more information, please see the [AWS Documentation](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html#setting-credentials){:target="_blank"}.

Example `cdms3` URIs (specific to AWS S3):
* cdms3:bucket-name?super/long/key
* cdms3://profile_name@aws/bucket-name?super/long/key

Note: In order to supply a profile name (one way to set the region and/or credentials) while maintaining conformance to the URI specification, you may use "aws" as the host.
In addition to the use of the credentials file for setting the region, as described above, the region may be set using the `aws.region` Java System Property, or the `AWS_REGION` environment variable.
Note that a region set within the credentials file for the `default` profile will take precedence over all others.
Possible values for the region code can be found in the [AWS Regional endpoints](https://docs.aws.amazon.com/general/latest/gr/rande.html#regional-endpoints){:target="_blank"} documentation.

When running in AWS and accessing objects from S3, it is better to avoid the use of a credentials file when possible.
One way to do that is to attach an IAM Policy role to the EC2 instance or lambda function in which your code is running.
For more information on IAM Profiles, please visit the [AWS User Guide](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies.html){:target="_blank"}.
 
The following examples show how one could access the same GOES 16 data file across a variety of Object Store technologies (special thanks to the [NOAA Big Data project's](https://www.noaa.gov/big-data-project){:target="_blank"}):

[AWS S3 bucket](https://registry.opendata.aws/noaa-goes/){:target="_blank"} in the US East 1 region (open access):

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/DatasetUrlExamples.java&awsGoes16Example %}
{% endcapture %}
{{ rmd | markdownify }}

[Google Cloud Storage](https://console.cloud.google.com/storage/browser/gcp-public-data-goes-16){:target="_blank"} (open access):

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/DatasetUrlExamples.java&gcsGoes16Example %}
{% endcapture %}
{{ rmd | markdownify }}

[Open Science Data Cloud](https://www.opensciencedatacloud.org/){:target="_blank"} (Ceph) (open access):

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/DatasetUrlExamples.java&osdcGoes16Example %}
{% endcapture %}
{{ rmd | markdownify }}

### File Types

The local or remote file must be one of the [formats](file_types.html) that the netCDF-Java library can read.
We call this set of files Common Data Model files, or CDM files for short, to make clear that the NetCDF-Java library is not limited to netCDF files.

If the URL ends with a with `.Z`, `.zip`, `.gzip`, `.gz`, or `.bz2`, the file is assumed to be compressed.
The netCDF-Java library will uncompress/unzip and write a new file without the suffix, then read from the uncompressed file. 
Generally it prefers to place the uncompressed file in the same directory as the original file.
If it does not have write permission on that directory, it will use the [cache directory](disk_caching.html) defined by `ucar.nc2.util.DiskCache`.

## `ucar.nc2.dataset.NetcdfDataset.openFile(String location)`

`NetcdfDataset` adds another layer of functionality to the CDM data model, handling other protocols and optionally enhancing the dataset with Coordinate System information, scale/offset processing, dataset caching, etc.

* `openFile()` can open the same datasets as `NetcdfFile`, plus those listed below.
* `openDataset()` calls `NetcdfDataset.openFile()`, then optionally enhances the dataset.
* `acquireDataset()` allows dataset objects to be cached in memory for performance.

### OPeNDAP datasets

`NetcdfDataset` can open OPeNDAP datasets, which use a `dods:` or `http:` prefix, for example:

* `http://thredds.ucar.edu/thredds/dodsC/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1`
* `dods://thredds.ucar.edu/thredds/models/NCEP/GFS/Global_5x2p5deg/GFS_Global_5x2p5deg_20070313_1200.nc`

To avoid confusion with remote HTTP files, OPeNDAP URLs may use the `dods:` prefix.
Also note that when passing an OPeNDAP dataset URL to the netCDF-Java library, do not include any the access suffixes, e.g. `.dods`, `.ascii`, `.dds`, etc.

For an `http:` URL, we make a `HEAD` request, and if it succeeds and returns a header with `Content-Description="dods-dds"` or `"dods_dds"`, then we open as OPeNDAP.
If it fails we try opening as an HTTP remote file.
Using the `dods:` prefix makes it clear which protocol to use.

The netCDF-Java `NetcdfDatasets.open*` methods can also be used to read the binary response from an OPeNDAP server from a file on disk.
**Note:** one downside to this approach is that the entire dataset will be loaded into memory.
At a minimum, you will need to have saved the binary response (`.dods`).
It is _strongly recommended_ that you also save the Data Attribute Structure (`.das`) as well, as this contains metadata for the dataset.
The two files must be located in the same directory and should only differ by file extension.
Once the files are in place, you may open the saved response by appending the `file:` protocol to the path to the `.dods` file:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/DatasetUrlExamples.java&openDodsBinaryFile %}
{% endcapture %}
{{ rmd | markdownify }}

In the example above, `pathToDodsFile` should look like `C:/Users/me/Downloads/cool-dataset.nc.dods` or `/home/me/data/cool-dataset.nc.dods`.
Again, is it _strongly recommended_ that `cool-dataset.nc.das` exist, but its existence is technically optional (but you will **not** have metadata without it).

As an example, the following two URLs will provide an example of each type of file needed:
* [.dods](https://thredds.ucar.edu/thredds/dodsC/casestudies/python-gallery/NAM_20161031_1200.nc.dods?time[0:1:0],y[0:100:427],x[0:100:613],lat[0:100:427][0:100:613],lon[0:100:427][0:100:613],Temperature_height_above_ground[0:1:0][0:1:0][0:100:427][0:100:613],height_above_ground1[0:1:1])
* [.das](https://thredds.ucar.edu/thredds/dodsC/casestudies/python-gallery/NAM_20161031_1200.nc.das)

{% comment %}
  If any of the two urls above change, make sure the changes are reflected in docs/src/test/java/examples/DatasetUrlExamples.java
{% endcomment %}

### NcML datasets

`NetcdfDataset` can open NcML datasets, which may be local or remote, and must end with a `.xml` or `.ncml` suffix, for example:

* `/usr/share/data/model.ncml`
* `file:/usr/share/data/model.ncml`
* `https://www.unidata.ucar.edu/software/netcdf-java/testdata/mydata1.xml`

Because xml is so widely used, we recommend using the `.ncml` suffix when possible.

### THREDDS Datasets

`NetcdfDataset` can open THREDDS datasets, which are contained in THREDDS Catalogs.
The general form is:

`thredds:catalogURL#dataset_id`

where `catalogURL` is the URL of a THREDDS catalog, and `dataset_id` is the `ID` of a dataset inside of that catalog.
The `thredds:` prefix ensures that it is understood as a THREDDS dataset.
Examples:

* `thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly`
* `thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData`

In the first case, `http://localhost:8080/test/addeStationDataset.xml` must be a catalog containing a dataset with `ID` `surfaceHourly`.
The second case will open a catalog located at `c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml` and find the dataset with `ID` `AddeSurfaceData`.

`NetcdfDataset` will examine the thredds dataset object and extract the dataset URL, open it and return a `NetcdfDataset`.
If there are more than one dataset access URL, it will choose a service that it understands.
You can modify the preferred services by calling `thredds.client.catalog.tools.DataFactory.setPreferAccess()`.
The dataset metadata in the THREDDS catalog may be used to augment the metadata of the `NetcdfDataset`.

### THREDDS Resolver Datasets

`NetcdfDataset` can open THREDDS Resolver datasets, which have the form

`thredds:resolve:resolverURL`

The `resolverURL` must return a catalog with a single top level dataset, which is the target dataset.
For example:

`thredds:resolve:https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/latest.xml`

In this case, `https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/latest.html` returns a catalog containing the latest dataset in the `grib/NCEP/GFS/Global_0p25deg` collection. 
`NetcdfDataset` will read the catalog, extract the THREDDS dataset, and open it as in section above.

### CdmRemote Datasets

`NetcdfDataset` can open [CDM Remote](cdmremote.html) datasets, with the form

`cdmremote:cdmRemoteURL`

for example

* `cdmremote:http://server:8080/thredds/cdmremote/data.nc`

The `cdmRemoteURL` must be an endpoint for a `cdmremote` web service, which provides index subsetting on remote CDM datasets.

### DAP4 datasets

`NetcdfDataset` can open datasets through the DAP4 protocol.
The url should either begin with `dap4:` or `dap4:http:`.
Examples:

* `dap4:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1`
* `dap4://thredds.ucar.edu:8080/thredds/models/NCEP/GFS/Global_5x2p5deg/GFS_Global_5x2p5deg_20070313_1200.nc`

To avoid confusion with other protocols using HTTP URLs, DAP4 URLs are often converted to use the `dap4:` prefix.
Also note that when passing a DAP4 dataset URL to the netCDF-Java library, do not include any of the access suffixes, e.g. `.dmr`, `.dap`, `.dst`, etc.


## `ucar.nc2.ft.FeatureDatasetFactoryManager.open()`

`FeatureDatasetFactory` creates [Feature Datasets](feature_datasets.html) for Coverages (Grids), Discrete Sampling Geometry (Point) Datasets, Radial Datasets, etc.
These may be based on local files, or they may use remote access protocols.

`FeatureDatasetFactoryManager` can open the same URLs that `NetcdfDataset` and `NetcdfFile` can open, plus the following:

### CdmrFeature Datasets

`FeatureDatasetFactoryManager` can open [CdmRemote Feature Datasets](cdmremote_feature_datasets.html), which have the form

`cdmrFeature:cdmrFeatureURL`

for example:

* `cdmrFeature:http://server:8080/thredds/cdmremote/data.nc`

The `cdmrFeatureURL` must be an endpoint for a `cdmrFeature` web service, which provides coordinate subsetting on remote Feature Type datasets.

## THREDDS Datasets

`FeatureDatasetFactoryManager` can also open CdmRemote Feature Datasets, by passing in a dataset `ID` in a catalog, exactly as in `NetcdfDataset.open` as explained above.
The general form is

`thredds:catalogURL#dataset_id`

where `catalogURL` is the URL of a THREDDS catalog, and `dataset_id` is the `ID` of a dataset inside of that catalog.
The `thredds:` prefix ensures that the URL is understood as a THREDDS catalog and dataset.
Example:

* `thredds:http://localhost:8081/thredds/catalog/grib.v5/gfs_2p5deg/catalog.html#grib.v5/gfs_2p5deg/TwoD`

If the dataset has a `cdmrFeature` service, the `FeatureDataset` will be opened through that service.
This can be more efficient than opening the dataset through the index-based services like `OPeNDAP` and `cdmremote`.

### Collection Datasets

`FeatureDatasetFactoryManager` can open collections of datasets specified with a [collection specification string](https://docs.unidata.ucar.edu/tds/5.0/userguide/collection_spec_string_ref.html){:target="_blank"}.
This has the form

`collection:spec`

`FeatureDatasetFactoryManager` calls `CompositeDatasetFactory.factory(wantFeatureType, spec)` if found, which returns a `FeatureDataset`.
Currently only a limited number of Point Feature types are supported. This is an experimental feature.

### NcML referenced datasets

NcML datasets typically reference other CDM datasets, using the `location` attribute of the `netcdf` element, for example:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
     location="file:/dev/netcdf-java-2.2/test/data/example1.nc">
...
~~~

The `location` is passed to `ucar.nc2.dataset.NetcdfDataset.openFile()`, and so can be any valid CDM dataset location.
In addition, an NcML referenced dataset location can be relative to the NcML file or the working directory:

* A relative URL resolved against the NcML location (eg `subdir/mydata.nc`).
  You must not use a `file:` prefix in this case.
* An absolute file URL with a relative path (eg `file:data/mine.nc`). 
  The file will be opened relative to the working directory.

There are a few subtle differences between using a location in NcML and passing a location to the `NetcdfDataset.openFile()` and related methods:

* In NcML, you **MUST** always use forward slashes in your paths, even when on a Windows machine.
   For example: `file:C:/data/mine.nc`. `NetcdfFile.open()` will accept backslashes on a Windows machine.
* In NcML, a relative URL is resolved against the NcML location.
  In `NetcdfFile.open()`, it is interpreted as relative to the working directory.

### NcML scan location

NcML aggregation `scan` elements use the `location` attribute to specify which directory to find files in, for example:

~~~xml
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <aggregation dimName="time" type="joinExisting">
    <scan location="/data/model/" suffix=".nc" />
  </aggregation>
</netcdf>
~~~

Allowable forms of the location for the scan directory are:
* `/usr/share/data/`
* `file:/usr/share/data/`
* `file:C:/share/data/model.nc` (NOTE we advise using forward slashes everywhere, including Windows)
* `data/model.nc` (relative to the NcML directory)
* `file:data/model.nc` (relative to the current working directory)

When using a directory location that has an embedded `:` char, e.g. `C:/share/data/model.nc`, its a really good idea to add the `file:` prefix, to prevent the `C:` from being misinterpreted as a URI schema.

Note that this is a common mistake:

`<scan location="D:\work\agg" suffix=".nc" />`

on a Windows machine, this will try to scan `D:/work/agg/D:/work/agg`.
Use

`<scan location="D:/work/agg" suffix=".nc" />`

or better

`<scan location="file:D:/work/agg" suffix=".nc" />`