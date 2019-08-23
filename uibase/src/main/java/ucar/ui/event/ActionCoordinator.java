/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.event;

import ucar.util.ListenerManager;

/**
 * An ActionCoordinator helps manage the set of objects that send and receive
 * an ActionValueEvent. It is assumed that each event generator is also
 * interested in recieving the event if its from someone else; these objects are
 * of type ActionSourceListener.
 *
 * For each kind of event, an ActionCoordinator
 * object is created. When it gets an event, it sends it to all others who have
 * registered except not to the event source.
 *
 * @see ActionValueEvent
 * @see ActionSourceListener
 *
 * @author John Caron
 */

// LOOK could replace with guava EventBus probably
public class ActionCoordinator implements ActionValueListener {
  private ListenerManager lm;
  private String eventType;

  public ActionCoordinator(String eventType) {
    this.eventType = eventType;

    // manage Action Listeners
    lm = new ListenerManager("ucar.ui.event.ActionValueListener", "ucar.ui.event.ActionValueEvent", "actionPerformed");
  }

  public void actionPerformed(ActionValueEvent e) {
    lm.sendEventExcludeSource(e);
  }

  /** add an ActionSource listener */
  public void addActionSourceListener(ActionSourceListener l) {
    if (!eventType.equals(l.getEventTypeName()))
      throw new IllegalArgumentException(
          "ActionCoordinator: tried to add ActionSourceListener for wrong kind of Action " + eventType + " != "
              + l.getEventTypeName());

    lm.addListener(l);
    l.addActionValueListener(this);
  }

  /** remove an ActionSource listener */
  public void removeActionSourceListener(ActionSourceListener l) {
    lm.removeListener(l);
    l.removeActionValueListener(this);
  }

}
