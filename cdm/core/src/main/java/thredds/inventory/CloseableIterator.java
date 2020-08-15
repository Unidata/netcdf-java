/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An iterator that must be closed.
 *
 * try (CloseableIterator iter = getIterator()) {
 * // do stuff
 * }
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {
}
