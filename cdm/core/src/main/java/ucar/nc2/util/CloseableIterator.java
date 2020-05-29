/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator that must be closed.
 *
 * try (CloseableIterator iter = getIterator()) {
 * // do stuff
 * }
 *
 * TODO will move to thredds.inventory in ver6
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
}
