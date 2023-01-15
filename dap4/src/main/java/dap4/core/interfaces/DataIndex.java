/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.interfaces;

public interface DataIndex {

  public int getRank();

  public int[] getCurrentCounter();

  public int getCurrentCounter(int i);

  public int[] getShape();

  public int getShape(int i);

  public int index();

  public boolean isScalar();
}
