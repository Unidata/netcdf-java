/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.featurecollection;

/**
 * Interface for user configured change to time unit.
 *
 * @author caron
 * @since 1/23/12
 */
public interface TimeUnitConverter {
  int convertTimeUnit(int timUnit);
}
