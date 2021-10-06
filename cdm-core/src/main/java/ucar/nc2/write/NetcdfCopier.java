/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.util.CancelTask;

/**
 * Utility class for copying a NetcdfFile object, or parts of one, to a netcdf-3 or netcdf-4 file.
 * This handles the entire CDM model (groups, etc) if you are writing to netcdf-4.
 * If copying from an extended model to classic model, Strings are converted to Chars; nested groups are not allowed.
 * <p/>
 * The fileIn may be an NcML file which has a referenced dataset in the location URL, the underlying data (modified by
 * the NcML) is written to the new file. If the NcML does not have a referenced dataset, then the new file is filled
 * with fill values, like ncgen.
 * <p/>
 * Use Nccopy for a command line interface.
 * Use NetcdfFormatWriter object for a lower level API.
 */
public class NetcdfCopier implements Closeable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfCopier.class);
  private static final long maxSize = 50 * 1000 * 1000; // 50 Mbytes
  private static boolean debug, debugWrite;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlags) {
    debug = debugFlags.isSet("ncfileWriter/debug");
    debugWrite = debugFlags.isSet("ncfileWriter/debugWrite");
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private final NetcdfFile fileIn;
  private final NetcdfFormatWriter.Builder<?> writerb;
  private final boolean extended;

  public static NetcdfCopier create(NetcdfFile fileIn, NetcdfFormatWriter.Builder<?> ncwriter) {
    Preconditions.checkNotNull(fileIn);
    Preconditions.checkNotNull(ncwriter);
    return new NetcdfCopier(fileIn, ncwriter);
  }

  private NetcdfCopier(NetcdfFile fileIn, NetcdfFormatWriter.Builder<?> writerb) {
    this.fileIn = fileIn;
    this.writerb = writerb;
    this.extended = getOutputFormat().isExtendedModel();

    // Try to do some checking
    if (!fileIn.getRootGroup().getGroups().isEmpty() && !extended) {
      throw new IllegalStateException("Input file has nested groups: cannot write to format= " + getOutputFormat());
    }
  }

  private NetcdfFileFormat getOutputFormat() {
    return writerb.format;
  }

  /**
   * Write the input file to the output file.
   *
   * @param cancel allow user to cancel; may be null.
   */
  public void write(@Nullable CancelTask cancel) throws IOException {
    if (cancel == null) {
      cancel = CancelTask.create();
    }

    Group.Builder root = fileIn.getRootGroup().toBuilder();
    convertGroup(root);
    writerb.setRootGroup(root);

    if (cancel.isCancel()) {
      return;
    }

    // create and write to the file
    try (NetcdfFormatWriter ncwriter = writerb.build()) {
      if (cancel.isCancel()) {
        return;
      }

      Count counter = new Count();
      copyVariableData(ncwriter, fileIn.getRootGroup(), ncwriter.getRootGroup(), counter, cancel);
      if (cancel.isCancel()) {
        return;
      }
      System.out.format("FileCopier done: total bytes written = %d, number of variables = %d%n", counter.bytes,
          counter.countVars);
    }
  }

  private Attribute convertAttribute(Attribute org) {
    if (extended) {
      return org;
    }
    if (org.isString() && org.getLength() > 1) {
      Formatter f = new Formatter();
      for (int count = 0; count < org.getLength(); count++) {
        if (count > 0) {
          f.format(", ");
        }
        f.format("%s", org.getStringValue(count));
      }
      return Attribute.builder().setName(org.getShortName()).setStringValue(f.toString()).build();
    }

    if (!org.getDataType().isUnsigned()) {
      return org;
    }

    Array orgValues = org.getValues();
    Array nc3Values = Array.makeFromJavaArray(orgValues.getStorage(), false);
    return Attribute.builder().setName(org.getShortName()).setValues(nc3Values).build();
  }

  private void convertAttributes(AttributeContainerMutable atts) {
    ArrayList<Attribute> newAtts = new ArrayList<>();
    for (Attribute att : atts) {
      newAtts.add(convertAttribute(att));
    }
    atts.clear().addAll(newAtts);
  }

  private void convertGroup(Group.Builder group) throws IOException {
    convertAttributes(group.getAttributeContainer());

    for (Variable.Builder<?> var : group.vbuilders) {
      convertVariable(group, var);
    }

    for (Group.Builder nested : group.gbuilders) {
      if (!extended) {
        throw new RuntimeException("Cant write nested groups to classic netcdf");
      }
      convertGroup(nested);
    }
  }

  private void convertVariable(Group.Builder parent, Variable.Builder<?> vb) throws IOException {
    // decouple from input
    vb.setSPobject(null);
    vb.setProxyReader(null);
    vb.resetCache();
    vb.resetAutoGen();

    convertAttributes(vb.getAttributeContainer());

    if (vb.dataType == ArrayType.STRUCTURE) {
      Structure.Builder<?> sb = (Structure.Builder<?>) vb;
      for (Variable.Builder<?> nested : sb.vbuilders) {
        convertVariable(parent, nested);
      }
    } else {
      if (!extended && vb.dataType == ArrayType.STRING) {
        // find maximum length
        Array data = readDataFromOriginal(vb);
        IndexIterator ii = data.getIndexIterator();
        int max_len = 0;
        while (ii.hasNext()) {
          String s = (String) ii.getObjectNext();
          max_len = Math.max(max_len, s.length());
        }

        // add last dimension
        String strlenDimName = vb.shortName + "_strlen";
        Dimension strlenDim = Dimension.builder(strlenDimName, max_len).setIsShared(false).build();
        parent.addDimension(strlenDim);
        vb.addDimension(strlenDim);
        vb.setArrayType(ArrayType.CHAR);
      }
    }

    // classic model does not support unshared dimensions. LOOK neither does netcdf4 ??
    if (!extended) {
      int count = 0;
      for (Dimension dim : vb.getDimensions()) {
        if (!dim.isShared()) {
          String dimName = vb.shortName + "_Dim" + count; // LOOK could turn these back into unshared dimensions when
                                                          // reading.
          Dimension sharedDim = Dimension.builder(dimName, dim.getLength()).setIsShared(false).build();
          parent.addDimension(sharedDim);
          vb.replaceDimension(count, sharedDim);
        }
        count++;
      }
    }
  }

  private Array readDataFromOriginal(Variable.Builder<?> vb) throws IOException {
    Variable v = fileIn.findVariable(vb.getFullName());
    if (v == null) {
      throw new RuntimeException("Cant find variable" + vb.getFullName());
    }
    return v.read();
  }

  @Override
  public void close() throws IOException {

  }

  private static class Count {
    long bytes;
    int countVars;
  }

  private void copyVariableData(NetcdfFormatWriter ncwriter, Group groupIn, Group groupOut, Count counter,
      CancelTask cancel) throws IOException {
    for (Variable oldVar : groupIn.getVariables()) {
      if (cancel.isCancel()) {
        break;
      }
      Variable newVar = groupOut.findVariableLocal(oldVar.getShortName());
      if (debug) {
        System.out.format("write var= %s size = %d type = %s%n", oldVar.getFullName(), oldVar.getSize(),
            oldVar.getArrayType());
      }

      long size = oldVar.getSize() * oldVar.getElementSize();
      counter.bytes += size;

      if (size <= maxSize) {
        copyAll(ncwriter, oldVar, newVar);
      } else {
        copySome(ncwriter, oldVar, newVar, maxSize, cancel);
      }
      counter.countVars++;
    }

    for (Group nestedIn : groupIn.getGroups()) {
      if (cancel.isCancel()) {
        break;
      }
      Group nestedOut = groupOut.findGroupLocal(nestedIn.getShortName());
      copyVariableData(ncwriter, nestedIn, nestedOut, counter, cancel);
    }
  }

  // copy all the data in oldVar to the newVar
  private void copyAll(NetcdfFormatWriter ncwriter, Variable oldVar, Variable newVar) throws IOException {
    Array data = oldVar.read();
    try {
      if (!extended && oldVar.getArrayType() == ArrayType.STRING) {
        data = convertDataToChar(newVar, data);
      }
      if (data.getSize() > 0) { // zero when record dimension = 0
        ncwriter.write(newVar, data);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException(e.getMessage() + " for Variable " + oldVar.getFullName());
    }
  }

  /**
   * Copies data from {@code oldVar} to {@code newVar}. The writes are done in a series of chunks no larger than
   * {@code maxChunkSize}
   * bytes.
   *
   * @param oldVar a variable from the original file to copy data from.
   * @param newVar a variable from the original file to copy data from.
   * @param maxChunkSize the size, <b>in bytes</b>, of the largest chunk to write.
   * @param cancel allow user to cancel, may be null.
   * @throws IOException if an I/O error occurs.
   */
  private void copySome(NetcdfFormatWriter ncwriter, Variable oldVar, Variable newVar, long maxChunkSize,
      CancelTask cancel) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    long byteWriteTotal = 0;

    ChunkingIndex index = new ChunkingIndex(oldVar.getShape());
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);

        Array data = oldVar.read(chunkOrigin, chunkShape);
        if (!getOutputFormat().isNetdf4format() && oldVar.getArrayType() == ArrayType.STRING) {
          data = convertDataToChar(newVar, data);
        }

        if (data.getSize() > 0) { // zero when record dimension = 0
          ncwriter.write(newVar, chunkOrigin, data);
          if (debugWrite) {
            System.out.println(" write " + data.getSize() + " bytes at " + new Section(chunkOrigin, chunkShape));
          }

          byteWriteTotal += data.getSize();
        }

        index.setCurrentCounter(index.currentElement() + (int) Index.computeSize(chunkShape));
        if (cancel.isCancel()) {
          return;
        }

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IOException(e.getMessage());
      }
    }
  }

  private Array convertDataToChar(Variable newVar, Array oldData) {
    ArrayChar newData = (ArrayChar) Array.factory(ucar.ma2.DataType.CHAR, newVar.getShape());
    Index ima = newData.getIndex();
    IndexIterator ii = oldData.getIndexIterator();
    while (ii.hasNext()) {
      String s = (String) ii.getObjectNext();
      int[] c = ii.getCurrentCounter();
      for (int i = 0; i < c.length; i++) {
        ima.setDim(i, c[i]);
      }
      newData.setString(ima, s);
    }
    return newData;
  }
}


