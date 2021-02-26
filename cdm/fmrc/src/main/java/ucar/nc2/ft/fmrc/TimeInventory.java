/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

/**
 * Extract time coordinates from a FmrcInvLite.Gridset.
 * For 1D time datasets.
 *
 * @author caron
 * @since Apr 15, 2010
 * @deprecated DO NOT USE
 */
@Deprecated
public interface TimeInventory {
  String getName();

  int getTimeLength(FmrcInvLite.Gridset gridset);

  FmrcInvLite.ValueB getTimeCoords(FmrcInvLite.Gridset gridset);

  double[] getRunTimeCoords(FmrcInvLite.Gridset gridset);

  double[] getOffsetCoords(FmrcInvLite.Gridset gridset);

  Instance getInstance(FmrcInvLite.Gridset.Grid gridLite, int timeIdx);

  interface Instance {
    String getDatasetLocation();

    int getDatasetIndex();
  }

}
