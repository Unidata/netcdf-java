![netcdf-java icon](https://www.unidata.ucar.edu/images/logos/thredds_netcdf-150x150.png)
<br>
<br>

# netCDF-Java/CDM

The netCDF Java library provides an interface for scientific data access.
It can be used to read scientific data from a variety of file formats including netCDF, HDF, GRIB, BUFR, and many others.
By itself, the netCDF-Java library can only write netCDF-3 files.
It can write netCDF-4 files by using JNI to call the netCDF-C library.
It also implements Unidata's Common Data Model (CDM) to provide data geolocation capabilities.

For more information about netCDF-Java/CDM, see the netCDF-Java web page at

* https://www.unidata.ucar.edu/software/netcdf-java/

and the CDM web page at

https://docs.unidata.ucar.edu/netcdf-java/current/userguide/common_data_model_overview.html

You can obtain a copy of the latest released version of netCDF-Java software from

* https://www.unidata.ucar.edu/downloads/netcdf-java/

More documentation can be found at

* https://docs.unidata.ucar.edu/netcdf-java/current/userguide/index.html

A mailing list, netcdf-java@unidata.ucar.edu, exists for discussion of all things netCDF-Java/CDM including announcements about netCDF-Java/CDM bugs, fixes, enhancements, and releases.
For information about how to subscribe, see the "Subscribe" link on this page

* https://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/

For more general netCDF discussion, see the netcdfgroup@unidata.ucar.edu email list.

We appreciate feedback from users of this package.
Please send comments and suggestions to <support-netcdf-java@unidata.ucar.edu>.
For bug reports, feel free to open an issue on this repository, or contact us at the address above.
Please identify the version of the package as well as the version/vendor of Java you are using.
For potential security issues, please contact security@unidata.ucar.edu directly.


## NetCDF Markup Language (NcML)

NcML is an XML representation of netCDF metadata, it approximates the header information one gets from a netCDF file with the "ncdump -h" command.
NcML is similar to the netCDF CDL (network Common data form Description Language), except, of course, it uses XML syntax.

Beyond simply describing a netCDF file, it can also be used to describe changes to existing netCDF files.
A limited number of tools, mainly netCDF-Java based tools, support these features of NcML.

For more information about NcML, see the NcML web page at

https://docs.unidata.ucar.edu/netcdf-java/current/userguide/ncml_overview.html

## THREDDS Catalogs

THREDDS Catalogs can be thought of as representing logical directories of on-line data resources.
They are encoded as XML and provide a place for annotations and other metadata about the data resources.
While the THREDDS Data Server (TDS) generates THREDDS Catalogs, THREDDS Catalogs are not limited to those produced by the TDS.
These XML documents are how THREDDS-enabled data consumers find out what data is available from data providers.

THREDDS Catalog documentation (including the specification) is available at

* https://docs.unidata.ucar.edu/tds/5.0/userguide/basic_client_catalog.html

## Licensing

netCDF-Java is released under the BSD-3 licence, which can be found [here](https://github.com/Unidata/netcdf-java/blob/master/LICENSE).

Furthermore, this project includes code from third-party open-source software components:
* [Gretty](https://github.com/akhikhl/gretty): for details, see `buildSrc/README.md`
* [ERDDAP](https://coastwatch.pfeg.noaa.gov/erddap/index.html): for details, see `waterml/README.md`
* [JUnit](https://github.com/junit-team/junit4): for details, see `testUtil/README.md`

Each of these software components have their own license.
Please see `docs/src/private/licenses/third-party/`.

## How to use

The latest released and snapshot software artifacts (e.g. `.jar` files) are available from Unidata's Nexus repository:

* https://artifacts.unidata.ucar.edu/#browse/browse:unidata-all

To build netCDF-java from this repository, follow [this tutorial](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/building_from_source.html).

To use the netCDF-Java library as a dependency using maven or gradle, follow [these instructions](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/using_netcdf_java_artifacts.html).

## Previous releases

Prior to `v5.0.0`, the netCDF-Java/CDM library and the THREDDS Data Server (TDS) have been built and released together.
Starting with version 5, these two packages have been decoupled, allowing new features or bug fixes to be implemented in each package separately, and released independently.
Releases prior to `v5.0.0` were managed at <https://github.com/unidata/thredds>, which holds the combined code based used by `v4.6` and earlier.
