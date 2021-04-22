/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/**
 * <pre>
 *  DEPRECATED API - THIS AND ALL SUBPACKAGES WILL BE REPLACED / MOVE: Scientific data types package.
 * </pre>
 * <p>
 * These are interfaces that define access to specialized "scientific datatype" datasets. There are three types of
 * interfaces:
 * <ol>
 * <li>Datatype: atomic datatype usually defined by its dimensionality and topology.
 * <li>Collection: a homogenous collection of atomic types, specifying the nature of the collection.
 * <li>Dataset: usually a Collection of a particular Datatype.
 * </ol>
 * <p>
 * A TypedDataset and TypedVariable are lightweight abstractions of NetcdfDataset and Variable, which allows
 * implementations that are not necessarily based on NetcdfDataset objects.
 */
package ucar.nc2.dt;
