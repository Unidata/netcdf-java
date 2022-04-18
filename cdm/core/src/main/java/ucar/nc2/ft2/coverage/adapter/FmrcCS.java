/*
 * Copyright (c) 2012-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.adapter;

import ucar.nc2.dataset.*;

/**
 * FMRC Coordinate System Implementation
 *
 * @author John
 * @since 12/25/12
 */
public class FmrcCS extends DtCoverageCS {
  private CoordinateAxis1DTime[] timeAxisForRun;

  protected FmrcCS(DtCoverageCSBuilder builder) {
    super(builder);
  }

  // 2D time coordinate system rework made this override unnecessary. Even though this class is now pointless, we'll
  // keep it so that FMRC continue to have coordinate systems of type FmrcCS.
  // @Override
  // public CoordinateAxis getTimeAxis() {
  // if (builder.timeOffsetAxis != null)
  // return builder.timeOffsetAxis;
  // return builder.timeAxis;
  // }

  /*
   * public CoordinateAxis1DTime getTimeAxisForRun(CalendarDate runTime) {
   * CoordinateAxis1DTime runTimeAxis = getRunTimeAxis();
   * if (runTimeAxis == null) return null;
   * int runIndex = runTimeAxis.findTimeIndexFromCalendarDate(runTime);
   * 
   * int nruns = (int) runTimeAxis.getSize();
   * if ((runIndex < 0) || (runIndex >= nruns))
   * throw new IllegalArgumentException("getTimeAxisForRun index out of bounds= " + runIndex);
   * 
   * if (timeAxisForRun == null)
   * timeAxisForRun = new CoordinateAxis1DTime[nruns];
   * 
   * if (timeAxisForRun[runIndex] == null)
   * timeAxisForRun[runIndex] = makeTimeAxisForRun(runIndex);
   * 
   * return timeAxisForRun[runIndex];
   * }
   * 
   * private CoordinateAxis1DTime makeTimeAxisForRun(int run_index) {
   * CoordinateAxis tAxis = getTimeAxis();
   * VariableDS section;
   * try {
   * section = (VariableDS) tAxis.slice(0, run_index);
   * return CoordinateAxis1DTime.factory(ds, section, null);
   * } catch (InvalidRangeException | IOException e) {
   * e.printStackTrace();
   * }
   * 
   * return null;
   * }
   */


}
