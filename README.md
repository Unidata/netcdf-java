![netcdf-java icon](https://www.unidata.ucar.edu/images/logos/thredds_netcdf-150x150.png)
<br>
<br>

# netCDF-Java

Welcome to the development branch of the netCDF-Java library (currently version _8.x_)!

> Looking for the `7.x` line of development?
See branch [7.x](https://github.com/unidata/netcdf-java/tree/7.x).
Looking for the `6.x` line of development?
See branch [6.x](https://github.com/unidata/netcdf-java/tree/6.x)
Looking for the `5.x` line of development?
See branch [maint-5.x](https://github.com/unidata/netcdf-java/tree/maint-5.x).
Version `4.6` is no longer supported outside of the context of the THREDDS Data Server (TDS).
If you are looking for that codebase, it can be found at <https://github.com/Unidata/thredds/tree/4.6.x>.

The netCDF Java library provides an interface for scientific data access.
It can be used to read scientific data from a variety of file formats including netCDF, HDF, GRIB, and BUFR.
By itself, the netCDF-Java library can only write netCDF-3 files.
It can write netCDF-4 files by using JNA to call the netCDF-C library.
The library implements [Unidata's Common Data Model (CDM)](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/common_data_model_overview.html) to provide data geolocation capabilities.

Documentation can be found at <https://docs.unidata.ucar.edu/netcdf-java/>.
The [User's Guide](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/index.html) contains information on how to use the library in your project, a tutorial, and useful upgrade tips.

## Requirements

* Java 11 or above

Each pull requests runs a subset of tests using Java 11 and 14 (`AdoptOpenJDK` and `Zulu`).
Currently, netCDF-C version 4.7.4 is used by our netCDF-4 write tests.
The full test suite runs nightly (this take a little over an hour).
More information on our test environment can be found at <https://github.com/unidata/thredds-test-environment#thredds-test-environment-highlights>.

## Using netCDF-Java in your project

The latest release and snapshot artifacts are available from Unidata's Nexus repository.
To use the netCDF-Java artifacts as dependencies using maven or gradle, follow [these instructions](https://docs.unidata.ucar.edu/netcdf-java/dev/userguide/using_netcdf_java_artifacts.html).
At a minimum, you will need to reference the Unidata artifacts server (`https://artifacts.unidata.ucar.edu/repository/unidata-all/`) and use the `cdm-core` artifact, which has a `groupId` of `edu.ucar` and an `artifactId` of `cdm-core`.
We also provide a maven BOM (`groupId`: `edu.ucar`, `artifactId`: `netcdf-java-bom`) and a Gradle Java Platform (`groupId`: `edu.ucar`, `artifactId`: `netcdf-java-platform`) for convenience.
To build netCDF-java from this repository, follow [this tutorial](https://docs.unidata.ucar.edu/netcdf-java/dev/userguide/building_from_source.html).

## Participation

As contributors, creators, stewards, and maintainers of software managed by the Unidata Program Center, we agree to follow the UCAR [Codes of Conduct](https://www.ucar.edu/who-we-are/ethics-integrity/codes-conduct) to foster a safe, productive, welcoming and inclusive experience for everyone.
Please familiarize yourself with these Codes of Conduct, especially the [Contributor Code of Conduct](https://github.com/Unidata/.github/blob/3d84135b64c6aae4b329b8801fb3d11ddd9d09a9/CODE_OF_CONDUCT.md#contributor-code-of-conduct).
In the coming weeks, we'll be opening the GitHub Discussions area on this repository as a place for discussion of all things netCDF-Java.
Unidata will continue to host community mailing list, netcdf-java@unidata.ucar.edu, as a secondary outlet for release announcements, and as a place for those who do not wish to use GitHub.

We appreciate feedback from users of this package.
The GitHub Discussions area (once active) will be a great place to post comments and suggestions, and discuss the future direction of the library.
For bug reports, please open an issue on this repository.
Please identify the version of the package as well as the version/vendor of Java you are using.
For potential security issues, please contact security@unidata.ucar.edu directly.

## Contributors

Are you looking to contribute to the netCDF-Java efforts?
That's great!
Please see our [contributors guide](https://github.com/Unidata/netcdf-java/blob/develop/.github/CONTRIBUTING.md) for more information!

## Older versions

Prior to `v5.0.0`, the netCDF-Java library and the THREDDS Data Server (TDS) were built and released together.
Starting with version 5, the two packages were decoupled, allowing new features and bug fixes to be implemented in each package separately, and released independently.
Releases prior to `v5.0.0` were managed at <https://github.com/unidata/thredds>, which holds the combined code based used by `v4.6.x` and earlier.
If you are looking for the TDS, its new home is located at <https://github.com/unidata/tds>.
