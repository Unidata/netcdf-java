/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.Structure;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import java.io.IOException;

/**
 * Use contiguous or linked lists to iterate over members of a Structure
 *
 * @author caron
 * @since Mar 26, 2008
 */
public class StructureDataIteratorLinked implements StructureDataIterator {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDataIteratorLinked.class);

  private final Structure s;
  private final int firstRecord;
  private int nextRecno;
  private final int numRecords;
  private final String linkVarName;
  private int currRecno;
  private final boolean isContiguous;

  public StructureDataIteratorLinked(Structure s, int firstRecord, int numRecords, String linkVarName) {
    this.s = s;
    this.firstRecord = firstRecord;
    this.nextRecno = firstRecord;
    this.numRecords = numRecords; // contiguous only
    this.linkVarName = linkVarName;
    this.isContiguous = (linkVarName == null);
  }

  @Override
  public StructureData next() throws IOException {
    StructureData sdata;
    currRecno = nextRecno;
    try {
      sdata = s.readStructure(currRecno);
    } catch (ucar.ma2.InvalidRangeException e) {
      log.error("StructureDataLinkedIterator.nextStructureData recno=" + currRecno, e);
      throw new IOException(e.getMessage());
    }

    if (isContiguous) {
      nextRecno++;

    } else {
      nextRecno = sdata.getScalarInt(linkVarName);
      if (currRecno == nextRecno) // infinite loop
        throw new IllegalStateException("Infinite loop in linked list at recno= " + nextRecno);
    }

    return sdata;
  }

  @Override
  public boolean hasNext() {
    return isContiguous ? nextRecno < firstRecord + numRecords : nextRecno >= 0;
  }

  @Override
  public StructureDataIterator reset() {
    this.nextRecno = firstRecord;
    return this;
  }

  @Override
  public int getCurrentRecno() {
    return currRecno;
  }

}
