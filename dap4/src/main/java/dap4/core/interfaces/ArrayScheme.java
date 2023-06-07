/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.interfaces;


import dap4.core.dmr.DapType;
import dap4.core.dmr.DapVariable;

/**
 * Track kinds of Arrays
 */

public enum ArrayScheme {
  ATOMIC, STRUCTARRAY, STRUCTURE, SEQARRAY, SEQUENCE, RECORD;

  //////////////////////////////////////////////////
  // Static methods

  static public ArrayScheme schemeFor(DapVariable field) {
    DapType ftype = field.getBaseType();
    ArrayScheme scheme = null;
    boolean isscalar = field.getRank() == 0;
    if (ftype.getTypeSort().isAtomic())
      scheme = ArrayScheme.ATOMIC;
    else {
      if (ftype.getTypeSort().isStructType())
        scheme = ArrayScheme.STRUCTARRAY;
      else if (ftype.getTypeSort().isSeqType())
        scheme = ArrayScheme.SEQARRAY;
    }
    return scheme;
  }
}

