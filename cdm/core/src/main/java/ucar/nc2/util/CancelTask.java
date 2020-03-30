/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

/**
 * Allows long tasks to be cancelled. Used in potentially long method calls which the user may want the option to
 * cancel.
 *
 * LOOK this will become an (abstract?) class in ver6, and API will be added to.
 */
public interface CancelTask {
  /**
   * Calling routine may cancel, called routine checks this and return asap.
   *
   * @return true if task was cancelled
   */
  boolean isCancel();

  /**
   * Called routine sets whether operation successfully completed. LOOK not sure.
   */
  default void setSuccess() {}

  /**
   * Called routine got an error, so it sets a message for calling program to show to user.
   * 
   * @param msg message to show user
   */
  void setError(String msg);

  /**
   * Called routine may optionally show a progress message for calling program to show to user.
   * 
   * @param msg message to show user
   * @param progress count of progress
   */
  void setProgress(String msg, int progress);
}
