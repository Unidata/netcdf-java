/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.image;

/**
 * This interface allows an object to inform another object that the status it is listening on has
 * changed.
 */

public interface SourcePictureListener {

  /**
   * inform the listener that the status has changed
   */
  void sourceStatusChange(int statusCode, String statusMessage, SourcePicture sp);

  /**
   * inform the listener of progress on the loading of the image
   */
  void sourceLoadProgressNotification(int statusCode, int percentage);

}

