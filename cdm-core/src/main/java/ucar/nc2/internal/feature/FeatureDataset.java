/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.feature;

import ucar.nc2.AttributeContainer;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.calendar.CalendarDateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.internal.cache.FileCacheable;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * Superclass for "scientific feature type" datasets.
 * These are collections of features of the same feature type.
 * <p/>
 * This interface defines general "discovery metadata".
 * Its subtypes define type-specific information.
 * Implementations may or may not have a NetcdfFile underneath.
 */
public interface FeatureDataset extends FileCacheable, Closeable {

  /**
   * Contains collections of this FeatureType.
   *
   * @return FeatureType of data
   */
  FeatureType getFeatureType();

  /**
   * Title of the dataset.
   *
   * @return the title of the dataset, or null
   */
  String getTitle();

  /**
   * Text information about this dataset.
   *
   * @return any text information about this dataset, or null.
   */
  String getDescription();

  /**
   * The URI location of the dataset
   *
   * @return the URI location of the dataset, or null
   */
  String getLocation();

  /**
   * Calendar Date range for the entire dataset.
   *
   * @return the date range for the entire dataset, or null if unknown
   */
  CalendarDateRange getCalendarDateRange();

  /**
   * The lat/lon boundingBox for the entire dataset.
   *
   * @return the lat/lon boundingBox for the entire dataset, or null if unknown.
   */
  ucar.unidata.geoloc.LatLonRect getBoundingBox();

  /** Get the global attributes. */
  AttributeContainer attributes();

  /**
   * The data Variables available in this dataset.
   * Should just be data variables others might be searching for, not metadata or coordinate
   * system variables, etc.
   * The shapes of the VariableSimpleIF do not necessarily match the StructureData member.
   *
   * @return List of subclass of VariableSimpleIF, may be empty but not null
   */
  List<VariableSimpleIF> getDataVariables();

  /**
   * Get the named data Variable.
   *
   * @param shortName of data Variable.
   * @return VariableSimpleIF or null if not found
   */
  Optional<VariableSimpleIF> findDataVariable(String shortName);

  /**
   * Return underlying NetcdfFile, or null if none.
   *
   * @return the underlying NetcdfFile, or null if none.
   */
  Optional<ucar.nc2.NetcdfFile> getNetcdfFile();

  /**
   * Close all resources associated with this dataset.
   *
   * @throws java.io.IOException on i/o error
   */
  void close() throws java.io.IOException;

  /**
   * Show debug / underlying implementation details
   *
   * @param sf append info here
   */
  void getDetailInfo(java.util.Formatter sf);

  /**
   * Show who is implementing
   * 
   * @return name of implementor
   */
  String getImplementationName();

}
