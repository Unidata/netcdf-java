/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.image;

/**
 * a thread object that calls the ScalablePicture scalePicture method
 */
public class ScaleThread extends Thread {
  private ScalablePicture sclPic;


  /**
   * @param sclPic The picture we are doing this for
   */
  ScaleThread(ScalablePicture sclPic) {
    // Tools.log("Constructing ScaleThread object");
    this.sclPic = sclPic;
  }


  /**
   * method that is invoked by the thread to do things asynchroneousely
   */
  public void run() {
    sclPic.scalePicture();
  }

}


