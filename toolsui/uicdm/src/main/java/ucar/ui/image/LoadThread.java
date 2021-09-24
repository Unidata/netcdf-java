/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.image;

/** a class that moves all pictures of a group to a target directory */
public class LoadThread extends Thread {
  private final SourcePicture srcPic;

  /**
   * This class serves to create threads that load a SourcePicture.
   * The SourcePicture creates this thread with itself as a reference.
   * The thread spawns and then invokes the SourcePicture's loadPicture
   * method.
   *
   * @param srcPic The SourcePicture that should have it's loadPicture
   *        method called from the new thread.
   */
  LoadThread(SourcePicture srcPic) {
    this.srcPic = srcPic;
  }

  /**
   * method that is invoked by the thread which fires off the loadPicture
   * method in the srcPic object.
   */
  public void run() {
    srcPic.loadPicture();
  }
}


