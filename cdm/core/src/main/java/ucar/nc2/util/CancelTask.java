/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

/** Allows long tasks to be cancelled by the user. */
public interface CancelTask {
  static CancelTask create() {
    return new CancelTaskImpl();
  }

  /** Application asks to cancel the task. */
  void cancel();

  /** Called routine checks to see if task was cancelled. */
  boolean isCancel();

  class CancelTaskImpl implements CancelTask {
    private boolean cancel;

    @Override
    public void cancel() {
      this.cancel = true;
    }

    @Override
    public boolean isCancel() {
      return this.cancel;
    }
  }
}
