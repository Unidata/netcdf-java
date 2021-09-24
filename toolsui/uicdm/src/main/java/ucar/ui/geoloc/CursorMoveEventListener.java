/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

/**
 * Listeners for "Cursor Move" events.
 * 
 * @author John Caron
 */
public interface CursorMoveEventListener extends java.util.EventListener {
  void actionPerformed(CursorMoveEvent e);
}
