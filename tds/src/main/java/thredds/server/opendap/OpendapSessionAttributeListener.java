/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.opendap;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import thredds.server.opendap.servlet.GuardedDataset;

public class OpendapSessionAttributeListener implements HttpSessionAttributeListener {

  // HttpSessionAttributeListener
  public void attributeRemoved(HttpSessionBindingEvent e) {

    if (e.getValue() instanceof GuardedDataset) {
      GuardedDataset gdataset = (GuardedDataset) e.getValue();
      gdataset.close();
    }
  }

  public void attributeAdded(HttpSessionBindingEvent httpSessionBindingEvent) {}

  public void attributeReplaced(HttpSessionBindingEvent httpSessionBindingEvent) {}
}
