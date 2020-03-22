/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.log;

/**
 * Interface for generating org.slf4j.Logger objects.
 * Allows us to keep log4j dependencies out of the cdm
 *
 * TODO used by TDS?
 */
public interface LoggerFactory {

  org.slf4j.Logger getLogger(String name);

}
