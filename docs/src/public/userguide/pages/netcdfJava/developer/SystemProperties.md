---
title: System Properties
last_updated: 2019-12-29
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: systemproperties.html
---

## Netcdf-java System Properties

The netcdf-java library defines several Java System Properties for runtime configuration:

|---
| Key |  Meaning |  Default Value | Where Used
|:-|:-|:-|:-
| "unidata.h5iosp.inflate.buffersize" |  used by the h5iosp when uncompressing a file | 512 Bytes | ucar.nc2.iosp.hdf5.H5tiledLayoutBB
| "nj22.cache" | Set the root directory for the cache | user.home or user.dir + "/.unidata/cache/" | ucar.nc1.util.DiskCache   
| "nj22.cachePolicy" | Create the file in the cache directory | Create the file in the same directory as the original | ucar.nc1.util.DiskCache
| "nj22.cache" | Set the root directory for the cache | user.home or user.dir + "/.unidata/cache/" | ucar.nc1.util.DiskCache2
| "nj22.cachePersistRoot" | Set the root directory for the cache | user.home or user.dir + "/" | ucar.nc1.util.DiskCache2 
| "ucar.unidata.maxHttpBufferSize" | Buffer size when reading over http | 10000000 | ucar.unidata.io.http.HttpRandomAccessFile
| "httpservices.urlencode" | encode the url passed to the HTTPMethod constructor | true | ucar.httpservices.HTTPMethod
| "store" | key store path | None | ucar.httpservices.HTTPSession
| "storepassword" | key store password | None | ucar.httpservices.HTTPSession
| "tdsmonitor.datadir" | Path to local log storage | user.home | thredds.ui.monitor.LocalManager.java

### S3 specific properties

When reading from an AWS S3 Object store, the following properties can be set to control the behavior of `ucar.unidata.io.s3.S3RandomAccessFile`

|---
| Key |  Meaning |  Default Value
|:-|:-|:-|:-
| "ucar.unidata.io.s3.defaultS3BufferSize" | The S3 reading buffer size in bytes | 524288 (512 Kibibytes)
| "ucar.unidata.io.s3.defaultMaxCacheSize" | The internal RAF cache size in bytes | 33554432 (32 Mibibytes)
| "ucar.unidata.io.s3.httpMaxConnections" | The maximum number of connections allowed in the S3 http connection pool| 128
| "ucar.unidata.io.s3.connectionTimeout" | The amount of time in milliseconds to wait when initially establishing a connection | 100000 
| "ucar.unidata.io.s3.socketTimeout" | The amount of time to wait for data to be transferred | 100000
