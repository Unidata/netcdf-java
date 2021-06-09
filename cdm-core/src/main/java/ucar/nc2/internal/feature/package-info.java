/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * These are some classes from ucar.nc2.ft that we may want to hold on to.
 * <h3>Original</h3>
 * These are interfaces that define access to specialized "scientific feature" datasets. There are three types of
 * interfaces:
 * <ol>
 * <li>FeatureType: atomic type usually defined by its dimensionality and topology.
 * <li>Collection: a homogeneous collection of atomic types, specifying the nature of the collection.
 * <li>Dataset: one or more Collections of a particular FeatureType, with dataset metadata.
 * </ol>
 * <p>
 * A FeatureDataset and VariableSimpleIF are lightweight abstractions of NetcdfDataset and Variable, which allows
 * implementations that are not necessarily based on NetcdfDataset objects.
 */
package ucar.nc2.internal.feature;
