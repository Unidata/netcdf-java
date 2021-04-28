# Documentation

All gradle commands shown below must be run from the top level of the netCDF-Java repository.

## Jekyll-based documentation

* Common Data Model
  * docset_name: `cdm`
  * location: `cdm/`, `top_level_files/cdm/`
  * raw repository name: `docs-cdm`
  * version: `4.0` (not tied to netCDF-java)
  * versioned top level url: `https://docs.unidata.ucar.edu/thredds/cdm/4.0/index.html`
  * types: `versioned` or `current`

* netCDF Markup Language
  * docset_name: `ncml`
  * location: `ncml/`, `top_level_files/ncml/`
  * raw repository name: `docs-ncml`
  * version: `2.2` (not tied to netCDF-java)
  * versioned top level url: `https://docs.unidata.ucar.edu/thredds/ncml/2.2/index.html`
  * types: `versioned` or `current`

* netCDF-Java (Developer Guide - for developers of netCDF-Java)
  * docset_name: `developer`
  * location: `developer/`
  * raw repository name: `docs-netcdf-java`
  * version:  `major.minor` version of netCDF-java of current branch
  * versioned top level url: `https://docs.unidata.ucar.edu/netcdf-java/developer/major.minor/index.html`
  * types: `versioned`, `current` (if not on `develop` branch), or `dev` (if on `develop` branch)

* netCDF-Java (User Guide - for users of netCDF-Java)
  * docset_name: `userguide`
  * location: `userguide/`, `top_level_files/userguide/`
  * raw repository name: `docs-netcdf-java`
  * version:  `major.minor` version of netCDF-java of current branch
  * versioned top level url: `https://docs.unidata.ucar.edu/netcdf-java/major.minor/userguide/index.html`
  * types: `versioned`, `current` (if not on `develop` branch), or `dev` (if on `develop` branch)

* Shared Content
  * location: `shared/`
  * description: content shared between the jekyll builds of the docsets above.
    For more information, see [shared/README.md](shared/README.md)

### Gradle Tasks

* Build
  * `cdm`: `./gradlew :docs:buildCdm`
  * `developer`: `./gradlew :docs:buildDeveloper`
  * `ncml`: `./gradlew :docs:buildNcml`
  * `userguide`: `./gradlew :docs:buildUserGuide`

* Serve
  * same as `build*` tasks above, but using `serve*` (e.g. `./gradlew :docs:serveUserGuide`)

* Build all Jekyll-based sites (shortcut for building all jekyll-based documentation sets)
  * `./gradlew :docs:buildAllJekyllSites`

* Publish to nexus
  * same as `build*` tasks above, but using `publishAsVersioned*` or `publishAsCurrent*` (e.g. `./gradlew :docs:PublishAsVersionedUserGuide`)
  * use `./gradlew :docs:publishAllJekyllSitesAsVersioned` or `./gradlew :docs:publishAllJekyllSitesAsCurrent` to publish all Jekyll-based docsets to nexus

* Remove from nexus
  * same as `build*` tasks above, but using `deleteVersioned*FromNexus` or `deleteCurrent*FromNexus` (e.g. `./gradlew :docs:deleteVersionedUserGuideFromNexus`)
  * use `./gradlew :docs:deleteAllJekyllSitesVersionedFromNexus` or `./gradlew :docs:deleteAllJekyllSitesCurrentFromNexus` to remove all Jekyll-based docsets from nexus

## Javadocs

* Public API
  * raw repository name: `docs-netcdf-java`
  * version:  `major.minor` version of netCDF-java of current branch
  * versioned top level url: `https://docs.unidata.ucar.edu/netcdf-java/major.minor/javadoc/index.html`
  * types: `versioned`, `current` (if not on `develop` branch), or `dev` (if on `develop` branch)

* Public API (with deprecations)
  * raw repository name: `docs-netcdf-java`
  * version:  `major.minor` version of netCDF-java of current branch
  * versioned top level url: `https://docs.unidata.ucar.edu/netcdf-java/major.minor/javadoc-with-deprecations/index.html`
  * types: `versioned`, `current` (if not on `develop` branch), or `dev` (if on `develop` branch)

* All javadocs
  * raw repository name: `docs-netcdf-java`
  * version:  `major.minor` version of netCDF-java of current branch
  * versioned top level url: `https://docs.unidata.ucar.edu/netcdf-java/major.minor/javadocAll/index.html`
  * types: `versioned`, `current` (if not on `develop` branch), or `dev` (if on `develop` branch)

### Gradle Tasks

From the top level of the netCDF-Java repository:

* Build
  * Public API: `./gradlew :docs:buildJavadocPublicApi`
  * Public API (with deprecations): `./gradlew :docs:buildJavadocPublicApiWithDeps`
  * All javadocs: `./gradlew :docs:javadoc`

* Build all javadocs (shortcut)
  * `./gradlew :docs:javadoc`

* Publish to nexus
  * same as `build*` tasks above, but using `publishAsVersioned*` or `publishAsCurrent*` (e.g. `./gradlew :docs:PublishAsVersionedJavadocPublicApi`)
  * use `./gradlew :docs:publishAllJavadocsAsVersioned` or `./gradlew :docs:publishAllJavadocsAsCurrent` to publish all Javadocs to nexus

* Remove from nexus
  * same as `build*` tasks above, but using `deleteVersioned*FromNexus` or `deleteCurrent*FromNexus` (e.g. `./gradlew :docs:deleteVersionedJavadocPublicApiFromNexus`)
  * use `./gradlew :docs:deleteAllJavadocsVersionedFromNexus` or `./gradlew :docs:deleteAllJavadocsCurrentFromNexus` to remove all Javadoc sets from nexus
  
## Convenience tasks for managing all documentation at once

* Build
  * `./gradlew :docs:build`

* Publish to nexus
  * `./gradlew :docs:publishAllDocsAsVersioned` or `./gradlew :docs:publishAllDocsAsCurrent`

* Remove from nexus
  * `./gradlew :docs:deleteAllDocsVersionedFromNexus` or `./gradlew :docs:deleteAllDocsCurrentFromNexus`

## Delete Tasks

When running a `delete` task, `dryRun` mode will be used by default for safety.
A summary of what would be removed from nexus will be shown in the console, but nothing will be removed from the server.
To actually delete the files from nexus, be sure to set the `dryRun` property to false when running gradle, like so:

~~~shell
./gradlew -PdryRun=false :docs:deleteAllDocsVersionedFromNexus
~~~
