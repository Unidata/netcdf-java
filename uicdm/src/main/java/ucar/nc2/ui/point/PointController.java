/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui.point;

import ucar.nc2.ui.geoloc.NPController;
import ucar.nc2.ft.PointFeature;
import javax.swing.*;
import java.io.IOException;
import java.awt.event.ActionEvent;
import ucar.ui.widget.BAMutil;

/**
 * Widget for drawing point data
 *
 * @author caron
 * @since Nov 17, 2009
 */
public class PointController extends NPController {
  private PointRenderer pointRenderer;
  private boolean drawConnectingLine;

  public PointController() {
    pointRenderer = new PointRenderer();
    addRenderer(pointRenderer);

    AbstractAction useReaderAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        setDrawConnectingLine(state);
      }
    };
    BAMutil.setActionProperties(useReaderAction, "nj22/AddCoords", "draw connecting lines", true, 'C', -1);
    useReaderAction.putValue(BAMutil.STATE, drawConnectingLine);
    BAMutil.addActionToContainer(toolPanel, useReaderAction);

  }

  public void setPointFeatures(java.util.List<PointFeature> obs) throws IOException {
    pointRenderer.setPointFeatures(obs);
    redraw(true);
  }

  public void setDrawConnectingLine(boolean drawConnectingLine) {
    this.drawConnectingLine = drawConnectingLine;
    pointRenderer.setDrawConnectingLine(drawConnectingLine);
  }
}
