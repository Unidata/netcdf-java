/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.interfaces;

import dap4.core.dmr.DapNode;
import dap4.core.util.DapException;
import dap4.core.util.Slice;
import ucar.ma2.Index;

import java.util.List;

/**
 * For data access, we adopt a cursor model.
 * This comes from database technology where a
 * cursor object is used to walk over the
 * results of a database query. Here the cursor
 * walks the underlying data and stores enough
 * state to extract data depending on its
 * sort. The cursor may (or may not) contain
 * internal subclasses to track various kinds of
 * state.
 */

/**
 * This Interface it to allow references to Cursor functionality
 * where the cursor object is defined in some non contained code tree.
 * Note also this this Interface is shared by both client and server.
 */

public interface DataCursor {
  //////////////////////////////////////////////////
  // Kinds of Cursor

  public static enum Scheme {
    ATOMIC, STRUCTARRAY, STRUCTURE, SEQARRAY, SEQUENCE, RECORD;

    public boolean isCompoundArray() {
      return this == STRUCTARRAY || this == SEQARRAY;
    }
  }

  //////////////////////////////////////////////////
  // API

  public Scheme getScheme();

  public DapNode getTemplate();

  public boolean isScalar();
}
