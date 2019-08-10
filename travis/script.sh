#!/usr/bin/env bash

if [[ $TASK == "test-ncj" ]]
then
    echo "Testing netCDF-Java PR"
    $TRAVIS_BUILD_DIR/gradlew --info --stacktrace testAll --refresh-dependencies
elif [[ $TASK == "test-tds" ]]
then
    echo "Testing the THREDDS Data Server against the netCDF-Java PR"
    # publish netcdf-java artifacts to local maven repo (~/.m2/repository)
    echo "build netcdf-java and publish artifacts to local maven repository"
    $TRAVIS_BUILD_DIR/gradlew publishToMavenLocal

    # clone latest tds repo
    TDS_BUILD_DIR=$TRAVIS_BUILD_DIR/tds_git_repo
    echo "Clone the TDS repo..."
    git clone --depth 1 https://github.com/Unidata/tds.git $TDS_BUILD_DIR

    # tell tds build to use snapshots from the local maven repo
    echo "Tell the TDS build to look in the local maven repository for artifacts first..."
    sed -i 's/\/\/mavenLocal()/mavenLocal()/g' $TDS_BUILD_DIR/build.gradle
    sed -i 's/\/\/mavenLocal()/mavenLocal()/g' $TDS_BUILD_DIR/gradle/any/dependencies.gradle

    # setup env vars for tds build
    CONTENT_ROOT="-Dtds.content.root.path=$TDS_BUILD_DIR/tds/src/test/content"
    DOWNLOAD_DIR="-Dtds.download.dir=/tmp/download"
    UPLOAD_DIR="-Dtds.upload.dir=/tmp/upload"
    SYSTEM_PROPS="$CONTENT_ROOT $DOWNLOAD_DIR $UPLOAD_DIR"

    # run tds tests
    cd $TDS_BUILD_DIR
    echo "run the TDS tests"
    ./gradlew $SYSTEM_PROPS --info --stacktrace testAll --refresh-dependencies
else
    echo "I do not understand TASK = ${TASK}"
    echo "TASK must be either test-ncj or test-tds"
fi
