/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui;

import java.io.IOException;

public interface GetDataRunnable {
  void run(Object o) throws IOException;
}

