/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dods;

/** Common methods across Dods variable buidlers */
public interface DodsVariableBuilder<T> {

  T setCE(String CE);

  T setIsCaching(boolean caching);

}
