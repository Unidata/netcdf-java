/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib.cdm.nc2;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;

import java.io.IOException;

public class D4StructureDataIterator implements StructureDataIterator {

  protected StructureData[] list;
  protected int position;

  public D4StructureDataIterator() {
    this.list = null;
    this.position = 0;
  }

  //////////////////////////////////////////////////
  // Accessors

  public D4StructureDataIterator setList(StructureData[] list) {
    this.list = list;
    return this;
  }

  //////////////////////////////////////////////////
  // StructureDataIterator Interface Implementation

  public boolean hasNext() throws IOException {
    return position < list.length;
  }

  public StructureData next() throws IOException {
    if (position >= list.length)
      throw new IOException("No next element");
    return list[position++];
  }

  public StructureDataIterator reset() {
    position = 0;
    return this;
  }

  public int getCurrentRecno() {
    return position;
  }

}
