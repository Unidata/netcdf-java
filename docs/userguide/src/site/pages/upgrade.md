---
title: Upgrading to netCDF-Java version 6.x
last_updated: 2021-05-20
sidebar: userguide_sidebar
toc: false
permalink: upgrade.html
---

## Requirements

* Java 8 or later is required

## Overview

Version 6 of the netCDF-Java Library continues down the path of making core objects immutable, which is now mostly the case with this release.
The use of immutable core objects allows us to clearly define the boundaries of the library such that:

* When reading data, we will return immutable objects.
  You will only be able to make changes to copies of those objects, which will mark the handoff of control between the netCDF-Java library and your code.
* When writing data to netCDF files using the netCDF-Java library, the API will require immutable objects to be used.
  This is our signal to you that the objects and data you give us are the ones we will use to produce the file we write.

In order to achieve these boundaries, a new array package has been created, called `ucar.array`, which will replace `ucar.ma2`.
Interacting with data after reading files is slightly different, but writing data is very different.

## Quick Navigation

* [Summary of changes for v6.0.x](#netcdf-java-api-changes-60x)
* [Previous versions](#previous-releases)

## netCDF-Java API Changes (6.0.x)

* `ucar.ma2` is deprecated in favor of `ucar.array`
* A new Scientific Feature Type called grid (`ucar.nc2.grid`), is now available for testing (**experimental, subject to change**).

## Previous Releases

* [5.x](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/upgrade.html)