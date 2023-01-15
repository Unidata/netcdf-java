/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.dmr.*;
import dap4.core.interfaces.DataCursor;
import ucar.ma2.Array;

/**
 * Wrap an Array representing a variables' data, where the variable
 * is top-level or a field. The wrapper contains some additional
 * information beside the Array.
 */

public class D4Cursor implements DataCursor {

  //////////////////////////////////////////////////
  // Mnemonics
  static final long NULLOFFSET = -1;

  static final int D4LENSIZE = 8;

  //////////////////////////////////////////////////
  // Instance Variables

  protected D4DSP dsp;
  protected Scheme scheme; // Roughly, what kind of array
  protected DapNode template;
  protected Array array = null; // the Array object for the variable
  protected Object storage = null; // The storage underlying Array

  //////////////////////////////////////////////////
  // Constructor(s)

  public D4Cursor(Scheme scheme, D4DSP dsp, DapVariable template) {
    this.scheme = scheme;
    this.template = template;
    this.dsp = dsp;
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getScheme().toString());
    if (getScheme() == Scheme.STRUCTARRAY || getScheme() == Scheme.SEQARRAY)
      buf.append("[]");
    buf.append(":");
    buf.append(getTemplate().toString());
    return buf.toString();
  }

  //////////////////////////////////////////////////
  // set/get

  public D4DSP getDSP() {
    return this.dsp;
  }

  public Scheme getScheme() {
    return this.scheme;
  }

  public DapNode getTemplate() {
    return this.template;
  }

  public Array getArray() {
    return this.array;
  }

  public Object getStorage() {
    return this.storage;
  }

  public boolean isScalar() {
    return ((DapVariable) getTemplate()).getRank() == 0;
  }

  public D4Cursor setArray(Array a) {
    this.array = a;
    return this;
  }

  public D4Cursor setStorage(Object store) {
    this.storage = store;
    return this;
  }


  static public Scheme schemeFor(DapVariable field) {
    DapType ftype = field.getBaseType();
    Scheme scheme = null;
    boolean isscalar = field.getRank() == 0;
    if (ftype.getTypeSort().isAtomic())
      scheme = Scheme.ATOMIC;
    else {
      if (ftype.getTypeSort().isStructType())
        scheme = Scheme.STRUCTARRAY;
      else if (ftype.getTypeSort().isSeqType())
        scheme = Scheme.SEQARRAY;
    }
    return scheme;
  }

}


