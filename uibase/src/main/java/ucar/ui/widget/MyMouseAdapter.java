/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.event.MouseEvent;
import java.io.Serializable;

public class MyMouseAdapter extends java.awt.event.MouseAdapter implements Serializable {
  private static final boolean debugEvent = false;

  private int startx, starty;
  private int minMove = 4;

  public void mousePressed(MouseEvent e) {
    startx = e.getX();
    starty = e.getY();

    if (debugEvent)
      System.out.println("mousePressed " + startx + " " + starty);
  }

  public void mouseReleased(MouseEvent e) {
    int deltax = e.getX() - startx;
    int deltay = e.getY() - starty;
    int total = Math.abs(deltax) + Math.abs(deltay);
    if (total <= minMove)
      click(e);
    else
      drag(e, deltax, deltay);

    if (debugEvent) {
      System.out.println("mouseReleased " + e.getX() + " " + e.getY());
      if ((deltax > 0) || (deltay > 0))
        System.out.println("  MOVED " + deltax + " " + deltay);
    }
  }

  public void setMinMove(int minMove) {
    this.minMove = minMove;
  }

  /// subclasses should override
  public void click(MouseEvent e) {}

  public void drag(MouseEvent e, int deltax, int deltay) {}
}
