/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.event;

/**
 * Extend ActionEvent to contain a String value.
 * 
 * @author John Caron
 */
public class ActionValueEvent extends java.awt.event.ActionEvent {
  private final Object value;

  public ActionValueEvent(Object source, String command, Object value) {
    super(source, java.awt.event.ActionEvent.ACTION_PERFORMED, command);
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  public String toString() {
    return "ActionValueEvent " + getActionCommand() + " " + value;
  }
}
