/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import opendap.dap.BaseType;
import opendap.dap.BaseTypePrimitiveVector;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DByte;
import opendap.dap.DConstructor;
import opendap.dap.DDS;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import opendap.dap.NoSuchVariableException;
import opendap.dap.PrimitiveVector;
import thredds.server.opendap.servers.ServerMethods;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class AsciiWriter {


  /**
   * @param pw The PrintWirter to which output will be written.
   * @param dds The DDS which must have it's data values displayed as ASCII
   *        text.
   * @param specialO This <code>Object</code> is a goody that is used by a
   *        Server implementations to deliver important, and as yet unknown, stuff
   *        to the read method. If you don't need it, make it a <code>null</code>.
   * @throws NoSuchVariableException When a variable can't be found.
   * @throws IOException When data can't be read.
   */
  public void toASCII(PrintWriter pw, DDS dds, Object specialO) throws NoSuchVariableException, IOException {
    for (BaseType bt : dds.getVariables()) {
      writeAsc(bt, dds.getEncodedName(), pw, specialO);
    }

  }


  /**
   * @param bt The variable for which to write the ASCII representation of
   *        it's data.
   * @param datasetName String identifying the file or other data store
   *        from which to read a vaue for this variable.
   * @param pw The PrintWrtiter to which to write.
   * @param specialO This <code>Object</code> is a goody that is used by a
   *        Server implementations to deliver important, and as yet unknown, stuff
   *        to the read method. If you don't need it, make it a <code>null</code>.
   * @throws NoSuchVariableException When a variable can't be found.
   * @throws IOException When data can't be read.
   */
  private void writeAsc(BaseType bt, String datasetName, PrintWriter pw, Object specialO)
      throws IOException, NoSuchVariableException {

    if (!((ServerMethods) bt).isProject())
      return;

    if (bt instanceof DSequence) {
      DSequence dseq = (DSequence) bt;
      boolean moreToRead = true;
      while (moreToRead) {
        moreToRead = ((ServerMethods) bt).read(datasetName, specialO);

        for (BaseType member : dseq.getVariables()) {
          writeAsc(member, datasetName, pw, specialO);
        }
      }

    } else {
      if (!((ServerMethods) bt).isRead()) // make sure data is in memory, but don't read it twice!
        ((ServerMethods) bt).read(datasetName, specialO);
      toASCII(bt, pw);
    }
  }


  public void toASCII(BaseType dtype, PrintWriter pw) {
    toASCII(dtype, pw, true, null, true);
  }

  private void toASCII(BaseType dtype, PrintWriter pw, boolean addName, String rootName, boolean newLine) {
    if (dtype instanceof DArray)
      showArray((DArray) dtype, pw, addName, rootName, newLine);
    else if (dtype instanceof DGrid)
      showGrid((DGrid) dtype, pw, addName, rootName, newLine);
    else if (dtype instanceof DSequence)
      showSequence((DSequence) dtype, pw, addName, rootName, newLine);
    else if (dtype instanceof DStructure)
      showStructure((DStructure) dtype, pw, addName, rootName, newLine);
    else
      showPrimitive(dtype, pw, addName, rootName, newLine);
  }

  public void showPrimitive(BaseType data, PrintWriter pw, boolean addName, String rootName, boolean newLine) {
    if (addName) {
      pw.print(toASCIIFlatName(data, rootName));
      pw.print(", ");
    }

    if (data instanceof DString) // covers DURL case
      showString(pw, ((DString) data).getValue());
    else if (data instanceof DFloat32)
      pw.print((new Float(((DFloat32) data).getValue())).toString());
    else if (data instanceof DFloat64)
      pw.print((new Double(((DFloat64) data).getValue())).toString());
    else if (data instanceof DUInt32)
      pw.print((new Long(((DUInt32) data).getValue() & ((long) 0xFFFFFFFF))).toString());
    else if (data instanceof DUInt16)
      pw.print((new Integer(((DUInt16) data).getValue() & 0xFFFF)).toString());
    else if (data instanceof DInt32)
      pw.print((new Integer(((DInt32) data).getValue())).toString());
    else if (data instanceof DInt16)
      pw.print((new Short(((DInt16) data).getValue())).toString());
    else if (data instanceof DByte)
      pw.print((new Integer(((DByte) data).getValue() & 0xFF)).toString());
    else
      pw.print("Not implemented type = " + data.getTypeName() + " " + data.getEncodedName() + "\n");

    if (newLine)
      pw.print("\n");
  }

  private void showString(PrintWriter pw, String s) {
    // Get rid of null terminations on strings LOOK needs improvement, use substring anyway !!
    if ((s.length() > 0) && s.charAt(s.length() - 1) == ((char) 0)) { // jc mod
      char cArray[] = s.toCharArray();
      s = new String(cArray, 0, cArray.length - 1);
    }
    pw.print("\"" + s + "\"");
  }

  private void showArray(DArray data, PrintWriter pw, boolean addName, String rootName, boolean newLine) {

    if (addName) {
      pw.print(toASCIIFlatName(data, rootName));
      pw.print("\n");
    }

    int dims = data.numDimensions();
    int shape[] = new int[dims];
    int i = 0;
    for (DArrayDimension dad : data.getDimensions()) {
      shape[i++] = dad.getSize();
    }
    asciiArray(data, pw, addName, "", 0, dims, shape, 0);

    if (newLine)
      pw.print("\n");
  }

  /**
   * Print an array. This is a private member function.
   *
   * @param data The DArray to print.
   * @param os is the stream used for writing
   * @param addName Switch to toggle writing the array name.
   * @param label
   * @param index is the index of VEC to start printing
   * @param dims is the number of dimensions in the array
   * @param shape holds the size of the dimensions of the array.
   * @param offset holds the current offset into the shape array.
   * @return the number of elements written.
   */
  private int asciiArray(DArray data, PrintWriter os, boolean addName, String label, int index, int dims, int shape[],
      int offset) {

    if (dims == 1) {

      if (addName)
        os.print(label);

      for (int i = 0; i < shape[offset]; i++) {
        PrimitiveVector pv = data.getPrimitiveVector();
        if (pv instanceof BaseTypePrimitiveVector) {
          BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(index++);
          if ((i > 0) && (bt instanceof DString))
            os.print(", ");
          toASCII(bt, os, false, null, false);
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
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < shape[offset]; i++) {
        buf.setLength(0);
        buf.append(label);
        buf.append("[");
        buf.append(i);
        buf.append("]");
        if ((dims - 1) == 1)
          buf.append(", ");
        index = asciiArray(data, os, addName, buf.toString(), index, dims - 1, shape, offset + 1);
      }
      return index;
    }
  }

  private void showStructure(DStructure dstruct, PrintWriter pw, boolean addName, String rootName, boolean newLine) {

    if (rootName != null)
      rootName += "." + dstruct.getEncodedName();
    else
      rootName = dstruct.getEncodedName();

    boolean firstPass = true;
    for (BaseType ta : dstruct.getVariables()) {
      if (!ta.isProject())
        continue;
      if (!newLine && !firstPass)
        pw.print(", ");
      firstPass = false;

      toASCII(ta, pw, addName, rootName, newLine);
    }

    if (newLine)
      pw.print("\n");
  }

  private void showGrid(DGrid dgrid, PrintWriter pw, boolean addName, String rootName, boolean newLine) {

    int subprojections = dgrid.projectedComponents(true);

    if (rootName != null)
      rootName += "." + dgrid.getEncodedName();
    else if (subprojections > 1)
      rootName = dgrid.getEncodedName();
    else
      rootName = null;

    boolean firstPass = true;
    for (BaseType ta : dgrid.getVariables()) {
      if (!ta.isProject())
        continue;
      if (!newLine && !firstPass)
        pw.print(", ");
      firstPass = false;

      toASCII(ta, pw, addName, rootName, newLine);
    }

    if (newLine)
      pw.print("\n");
  }

  private void showSequence(DSequence dseq, PrintWriter pw, boolean addName, String rootName, boolean newLine) {

    if (rootName != null)
      rootName += "." + dseq.getEncodedName();
    else
      rootName = dseq.getEncodedName();

    pw.println(toASCIIFlatName(dseq, rootName));

    for (int row = 0; row < dseq.getRowCount(); row++) {
      List<BaseType> v = dseq.getRow(row);

      int j = 0;
      for (BaseType ta : v) {
        if (j > 0)
          pw.print(", ");
        toASCII(ta, pw, false, rootName, false);
        j++;
      }
      pw.println("");
    }

    if (newLine)
      pw.print("\n");
  }

  private String toASCIIFlatName(BaseType data, String rootName) {
    String result;

    StringBuffer s = new StringBuffer();
    if (rootName != null)
      s.append(rootName).append(".");
    s.append(data.getEncodedName());

    if (data instanceof DArray) {
      DArray darray = (DArray) data;
      PrimitiveVector pv = darray.getPrimitiveVector();
      if (pv instanceof BaseTypePrimitiveVector) {
        BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(0);
        if (bt instanceof DString) {
          for (DArrayDimension d : darray.getDimensions()) {
            s.append("[").append(d.getSize()).append("]");
          }
          result = s.toString();
        } else {
          result = toASCIIFlatName(bt, s.toString());
        }

      } else {
        for (DArrayDimension d : darray.getDimensions()) {
          s.append("[").append(d.getSize()).append("]");
        }
        result = s.toString();
      }
      return result;

    } else if (data instanceof DSequence) {
      DSequence dseq = (DSequence) data;
      s.setLength(0);

      boolean firstPass = true;
      for (int row = 0; row < dseq.getRowCount(); row++) {
        List<BaseType> v = dseq.getRow(row);
        for (BaseType ta : v) {
          if (!firstPass)
            s.append(", ");
          firstPass = false;
          s.append(toASCIIFlatName(ta, rootName));
        }
      }

    } else if (data instanceof DConstructor) {
      DConstructor dcon = (DConstructor) data;
      s.setLength(0);

      boolean firstPass = true;
      for (BaseType ta : dcon.getVariables()) {
        if (!firstPass)
          s.append(", ");
        firstPass = false;
        s.append(toASCIIFlatName(ta, rootName));
      }
    }

    return s.toString();
  }


}
