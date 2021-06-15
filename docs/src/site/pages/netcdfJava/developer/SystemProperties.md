---
title: System properties
last_updated: 2019-12-29
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: systemproperties.html
---

## Netcdf-Java System Properties

The netcdf-Java library defines several Java System Properties for runtime configuration:

|---
| Key |  Meaning |  Default Value | Where Used
|:-|:-|:-|:-
| "unidata.h5iosp.inflate.buffersize" |  Used by the h5iosp when uncompressing a file | 512 Bytes | ucar.nc2.iosp.hdf5.H5tiledLayoutBB
| "nj22.cache" | Set the root directory for the cache | user.home or user.dir + "/.unidata/cache/" | ucar.nc1.util.DiskCache   
| "nj22.cachePolicy" | Create the file in the cache directory | Create the file in the same directory as the original | ucar.nc1.util.DiskCache
| "nj22.cache" | Set the root directory for the cache | user.home or user.dir + "/.unidata/cache/" | ucar.nc1.util.DiskCache2
| "nj22.cachePersistRoot" | Set the root directory for the cache | user.home or user.dir + "/" | ucar.nc1.util.DiskCache2 
| "ucar.unidata.io.http.httpBufferSize" | The buffer size when reading over http | 262144 (256 KiB) | ucar.unidata.io.http.HttpRandomAccessFile
| "ucar.unidata.io.http.maxHttpBufferSize" | Deprecated. Use "ucar.unidata.io.http.httpBufferSize". | 262144 (256 KiB) | ucar.unidata.io.http.HttpRandomAccessFile
| "ucar.unidata.io.http.maxReadCacheSize" | The read cache size in bytes | 10485760 (10 MiB)| ucar.unidata.io.http.HttpRandomAccessFile
| "httpservices.urlencode" | Encode the url passed to the HTTPMethod constructor | True | ucar.httpservices.HTTPMethod
| "store" | Key store path | None | ucar.httpservices.HTTPSession
| "storepassword" | Key store password | None | ucar.httpservices.HTTPSession
| "tdsmonitor.datadir" | Path to local log storage | user.home | thredds.ui.monitor.LocalManager.java

### S3 specific properties

When reading from an AWS S3 Object store, the following properties can be set to control the behavior of `ucar.unidata.io.s3.S3RandomAccessFile`:

|---
| Key |  Meaning |  Default Value
|:-|:-|:-|:-
| "ucar.unidata.io.s3.bufferSize" | The S3 reading buffer size in bytes | 262144 (256 KiB)
| "ucar.unidata.io.s3.maxReadCacheSize" | The read cache size in bytes | 10485760 (10 MiB)
| "ucar.unidata.io.s3.httpMaxConnections" | The maximum number of connections allowed in the S3 http connection pool| 128
| "ucar.unidata.io.s3.connectionTimeout" | The amount of time in milliseconds to wait when initially establishing a connection | 100000 
| "ucar.unidata.io.s3.socketTimeout" | The amount of time in milliseconds to wait for data to be transferred | 100000