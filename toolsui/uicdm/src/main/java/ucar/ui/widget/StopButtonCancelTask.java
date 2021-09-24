/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import ucar.nc2.util.CancelTask;
import ucar.ui.widget.StopButton;

public class StopButtonCancelTask extends StopButton implements CancelTask {
  @Override
  public void cancel() {
    setCancel(true);
  }
}
