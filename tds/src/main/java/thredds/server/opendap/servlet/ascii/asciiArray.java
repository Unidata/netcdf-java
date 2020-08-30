/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servlet.ascii;

import java.io.PrintWriter;
import opendap.dap.BaseType;
import opendap.dap.BaseTypePrimitiveVector;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DString;
import opendap.dap.PrimitiveVector;

/**
 */
public class asciiArray extends DArray implements toASCII {

  private static boolean _Debug = false;

  /**
   * Constructs a new <code>asciiArray</code>.
   */
  public asciiArray() {
    this(null);
  }

  /**
   * Constructs a new <code>asciiArray</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public asciiArray(String n) {
    super(n);
  }


  /**
   * Returns a string representation of the variables value. This
   * is really foreshadowing functionality for Server types, but
   * as it may come in useful for clients it is added here. Simple
   * types (example: DFloat32) will return a single value. DConstuctor
   * and DVector types will be flattened. DStrings and DURL's will
   * have double quotes around them.
   *
   * @param addName is a flag indicating if the variable name should
   *        appear at the begining of the returned string.
   */
  public void toASCII(PrintWriter pw, boolean addName, String rootName, boolean newLine) {

    if (_Debug) {
      System.out.println("asciiArray.toASCII(" + addName + ",'" + rootName + "')  getName(): " + getEncodedName());
      System.out.println("  PrimitiveVector size = " + getPrimitiveVector().getLength());
    }

    if (addName)
      pw.print("\n");

    int dims = numDimensions();
    int shape[] = new int[dims];
    int i = 0;

    for (DArrayDimension dad : getDimensions()) {
      shape[i++] = dad.getSize();
    }

    if (newLine)
      pw.print("\n");

  }

  public String toASCIIFlatName(String rootName) {
    StringBuilder s = new StringBuilder();
    if (rootName != null) {
      s.append(rootName);
      s.append(".");
      s.append(getEncodedName());
    } else {
      s.append(getEncodedName());
    }

    String s2 = "";
    PrimitiveVector pv = getPrimitiveVector();
    if (pv instanceof BaseTypePrimitiveVector) {
      BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(0);
      if (bt instanceof DString) {
        for (DArrayDimension d : getDimensions()) {
          s.append("[");
          s.append(d.getSize());
          s.append("]");
        }
        s2 = s.toString();
      } else {
        s2 = ((toASCII) bt).toASCIIFlatName(s.toString());
      }
    } else {
      for (DArrayDimension dad : getDimensions()) {
        s.append("[");
        s.append(dad.getSize());
        s.append("]");
      }
      s2 = s.toString();
    }
    return (s2);
  }


  public String toASCIIAddRootName(PrintWriter pw, boolean addName, String rootName) {

    if (addName) {
      rootName = toASCIIFlatName(rootName);
      pw.print(rootName);
    }
    return (rootName);

  }


  /**
   * Print an array. This is a private member function.
   *
   * @param os is the stream used for writing
   * @param index is the index of VEC to start printing
   * @param dims is the number of dimensions in the array
   * @param shape holds the size of the dimensions of the array.
   * @param offset holds the current offset into the shape array.
   * @return the number of elements written.
   */
  private int asciiArray(PrintWriter os, boolean addName, String label, int index, int dims, int shape[], int offset) {

    // os.println("\n\n");
    // os.println("\tdims: " + dims);
    // os.println("\toffset: " + offset);
    // os.println("\tshape["+offset+"]: " + shape[offset]);
    // os.println("\tindex: " + index);
    // os.println("\n");

    if (dims == 1) {

      if (addName)
        os.print(label);

      for (int i = 0; i < shape[offset]; i++) {


        PrimitiveVector pv = getPrimitiveVector();

        if (pv instanceof BaseTypePrimitiveVector) {

          BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(index++);

          if (i > 0) {
            if (bt instanceof DString)
              os.print(", ");
            else
              os.println("");
          }
          ((toASCII) bt).toASCII(os, false, null, false);


        } else {

          if (i > 0)
            os.print(", ");
          pv.printSingleVal(os, index++);
        }


      }
      if (addName)
        os.print("\n");

      return index;
    } else {
      for (int i = 0; i < shape[offset]; i++) {
        StringBuilder s = new StringBuilder();
        s.append(label);
        s.append("[");
        s.append(i);
        s.append("]");
        if ((dims - 1) == 1)
          s.append(", ");
        index = asciiArray(os, addName, s.toString(), index, dims - 1, shape, offset + 1);
      }
      return index;
    }
  }


}


