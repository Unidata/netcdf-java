/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import java.io.IOException;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CancelTaskImpl;

/**
 * Utility class for copying a NetcdfFile object, or parts of one, to a netcdf-3 or netcdf-4 disk file.
 * This handles the entire CDM model (groups, etc) if you are writing to netcdf-4.
 * If copying from an extended model to classic model, Strings are converted to Chars; nested groups are not allowed.
 * <p/>
 * The fileIn may be an NcML file which has a referenced dataset in the location URL, the underlying data (modified by
 * the NcML) is written to the new file. If the NcML does not have a referenced dataset, then the new file is filled with
 * fill values, like ncgen.
 * <p/>
 * Use Nccopy for a command line interface.
 * Use NetcdfFormatWriter object for a lower level API.
 */
public class NetcdfCopier {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfCopier.class);
  private static final long maxSize = 50 * 1000 * 1000; // 50 Mbytes
  private static boolean debug, debugWrite;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlags) {
    debug = debugFlags.isSet("ncfileWriter/debug");
    debugWrite = debugFlags.isSet("ncfileWriter/debugWrite");
  }

  //////////////////////////////////////////////////////////////////////////////////////
  private final NetcdfFile fileIn;
  private final NetcdfFormatWriter ncwriter;
  private final boolean extended;

  public static NetcdfCopier create(NetcdfFile fileIn, NetcdfFormatWriter ncwriter) {
    Preconditions.checkNotNull(fileIn);
    Preconditions.checkNotNull(ncwriter);
    return new NetcdfCopier(fileIn, ncwriter);
  }

  private NetcdfCopier(NetcdfFile fileIn, NetcdfFormatWriter ncwriter) {
    this.fileIn = fileIn;
    this.ncwriter = ncwriter;
    this.extended = getOutputFormat().isExtendedModel();

    // Try to do some checking
    if (!fileIn.getRootGroup().getGroups().isEmpty() && !extended) {
      throw new IllegalStateException("Input file has nested groups: cannot write to format= " + getOutputFormat());
    }
  }

  private NetcdfFileFormat getOutputFormat() {
    return ncwriter.getFormat();
  }

  /**
   * /////////////////////////////////////////////////////////////////////////////////////////////
   * // might be better to push these next up into NetcdfCFWriter, but we want to use copyVarData
   *
   * Specify which variable will get written
   *
   * @param oldVar add this variable, and all parent groups
   * @return new Variable.
   *
   *         public Variable addVariable(Variable oldVar) {
   *         List<Dimension> newDims = getNewDimensions(oldVar);
   * 
   *         Variable newVar;
   *         if ((oldVar.getDataType() == DataType.STRING) && (!getFormat().isExtendedModel())) {
   *         newVar = ncwriter.addStringVariable(null, oldVar, newDims);
   *         } else {
   *         newVar = ncwriter.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), newDims);
   *         }
   *         varMap.put(oldVar, newVar);
   *         varList.add(oldVar);
   * 
   *         for (Attribute orgAtt : oldVar.attributes())
   *         ncwriter.addVariableAttribute(newVar, convertAttribute(orgAtt));
   * 
   *         return newVar;
   *         }
   * 
   *         private List<Dimension> getNewDimensions(Variable oldVar) {
   *         List<Dimension> result = new ArrayList<>(oldVar.getRank());
   * 
   *         // dimensions
   *         for (Dimension oldD : oldVar.getDimensions()) {
   *         Dimension newD = gdimHash.get(oldD.getShortName());
   *         if (newD == null) {
   *         newD = ncwriter.addDimension(null, oldD.getShortName(), oldD.isUnlimited() ? 0 : oldD.getLength(),
   *         oldD.isUnlimited(), oldD.isVariableLength());
   *         gdimHash.put(oldD.getShortName(), newD);
   *         if (debug)
   *         System.out.println("add dim= " + newD);
   *         }
   *         result.add(newD);
   *         }
   *         return result;
   *         }
   */

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Write the input file to the output file.
   *
   * @param cancel allow user to cancel; may be null.
   * @return the open output file.
   */
  public NetcdfFile write(@Nullable CancelTask cancel) throws IOException {
    if (cancel == null) {
      cancel = new CancelTaskImpl();
    }
    try {
      Group.Builder root = copyGroup(fileIn.getRootGroup(), null);
      NetcdfFile.Builder netcdfBuilder = NetcdfFile.builder().setRootGroup(root);

      if (cancel.isCancel()) {
        return null;
      }

      // create the file
      NetcdfFile fileOut = netcdfBuilder.build();
      ncwriter.create(fileOut);
      if (cancel.isCancel()) {
        return null;
      }

      Count counter = new Count();
      copyVariableData(fileIn.getRootGroup(), fileOut.getRootGroup(), counter, cancel);
      if (cancel.isCancel()) {
        return null;
      }

      ncwriter.flush();
      System.out.format("FileCopier done: total bytes written = %d, number of variables = %d%n", counter.bytes,
          counter.countVars);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      ncwriter.abort(); // clean up
      throw ioe;
    }

    cancel.setSuccess();
    return ncwriter.getOutputFile();
  }

  private Group.Builder copyGroup(Group oldGroup, Group.Builder parent) throws IOException {
    Group.Builder newGroup = Group.builder().setParentGroup(parent).setName(oldGroup.getShortName());
    if (debug) {
      System.out.println("add group= " + oldGroup.getShortName());
    }

    // attributes
    for (Attribute att : oldGroup.attributes()) {
      newGroup.addAttribute(convertAttribute(att));
      if (debug) {
        System.out.println("add groupAtt= " + att);
      }
    }

    // typedefs
    for (EnumTypedef td : oldGroup.getEnumTypedefs()) {
      newGroup.addEnumTypedef(td); // td are immutable
      if (debug) {
        System.out.println("add typedef= " + td);
      }
    }

    // dimensions
    for (Dimension oldD : oldGroup.getDimensions()) {
      Dimension newD = Dimension.builder().setName(oldD.getShortName()).setIsShared(oldD.isShared())
          .setIsUnlimited(oldD.isUnlimited()).setIsVariableLength(oldD.isVariableLength())
          .setLength(oldD.isUnlimited() ? 0 : oldD.getLength()) // unlimited already set, 0 allowed
          .build();
      newGroup.addDimension(newD);
      if (debug) {
        System.out.println("add dim= " + newD);
      }
    }

    // Variables
    for (Variable oldVar : oldGroup.getVariables()) {
      Variable.Builder newVar = copyVariable(newGroup, oldVar);
      if (debug) {
        System.out.println("add var= " + oldVar.getShortName());
      }
      newGroup.addVariable(newVar);
    }

    // nested groups
    for (Group nested : oldGroup.getGroups()) {
      newGroup.addGroup(copyGroup(nested, newGroup));
    }
    return newGroup;
  }

  private Variable.Builder copyVariable(Group.Builder parent, Variable oldVar) throws IOException {
    Variable.Builder vb;
    DataType newType = oldVar.getDataType();
    String dimNames = Dimensions.makeDimensionsString(oldVar.getDimensions());

    if (newType == DataType.STRUCTURE) {
      Structure oldStruct = (Structure) oldVar;
      Structure.Builder sb = Structure.builder().setName(oldVar.getShortName());
      for (Variable nested : oldStruct.getVariables()) {
        sb.addMemberVariable(copyVariable(parent, nested));
      }
      vb = sb;
    } else {
      vb = Variable.builder().setName(oldVar.getShortName()).setDataType(newType);
      if (!extended && newType == DataType.STRING) {
        // find maximum length
        Array data = oldVar.read();
        IndexIterator ii = data.getIndexIterator();
        int max_len = 0;
        while (ii.hasNext()) {
          String s = (String) ii.getObjectNext();
          max_len = Math.max(max_len, s.length());
        }

        // add last dimension
        String strlenDimName = oldVar.getShortName() + "_strlen";
        Dimension anon = Dimension.builder(strlenDimName, max_len).setIsShared(false).build();

        newType = DataType.CHAR;
        dimNames += " " + strlenDimName;
      }
    }
    vb.setParentGroupBuilder(parent).setDimensionsByName(dimNames);

    if (newType.isEnum()) {
      EnumTypedef en = oldVar.getEnumTypedef();
      vb.setEnumTypeName(en.getShortName());
    }

    // attributes
    for (Attribute att : oldVar.attributes()) {
      vb.addAttribute(convertAttribute(att));
      if (debug) {
        System.out.println("add varAtt= " + att);
      }
    }

    return vb;
  }

  // LOOK munge attribute if needed
  private Attribute convertAttribute(Attribute org) {
    if (extended || !org.getDataType().isUnsigned()) {
      return org;
    }
    Array orgValues = org.getValues();
    Array nc3Values = Array.makeFromJavaArray(orgValues.getStorage(), false);
    return new Attribute(org.getShortName(), nc3Values);
  }

  private class Count {
    long bytes;
    int countVars;
  }

  private void copyVariableData(Group groupIn, Group groupOut, Count counter, CancelTask cancel) throws IOException {
    for (Variable oldVar : groupIn.getVariables()) {
      if (cancel.isCancel()) {
        break;
      }
      Variable newVar = groupOut.findVariable(oldVar.getShortName());
      if (debug) {
        System.out.format("write var= %s size = %d type = %s%n", oldVar.getFullName(), oldVar.getSize(),
            oldVar.getDataType());
      }
      cancel.setProgress("writing " + oldVar.getFullName(), counter.countVars++);

      long size = oldVar.getSize() * oldVar.getElementSize();
      counter.bytes += size;

      if (size <= maxSize) {
        copyAll(oldVar, newVar);
      } else {
        copySome(oldVar, newVar, maxSize, cancel);
      }
    }

    for (Group nestedIn : groupIn.getGroups()) {
      if (cancel.isCancel()) {
        break;
      }
      Group nestedOut = groupOut.findGroupLocal(nestedIn.getShortName());
      copyVariableData(nestedIn, nestedOut, counter, cancel);
    }
  }

  // copy all the data in oldVar to the newVar
  void copyAll(Variable oldVar, Variable newVar) throws IOException {
    Array data = oldVar.read();
    try {
      if (!extended && oldVar.getDataType() == DataType.STRING) {
        data = convertDataToChar(newVar, data);
      }
      if (data.getSize() > 0) // zero when record dimension = 0
      {
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
  private void copySome(Variable oldVar, Variable newVar, long maxChunkSize, CancelTask cancel) throws IOException {
    long maxChunkElems = maxChunkSize / oldVar.getElementSize();
    long byteWriteTotal = 0;

    ChunkingIndex index = new ChunkingIndex(oldVar.getShape());
    while (index.currentElement() < index.getSize()) {
      try {
        int[] chunkOrigin = index.getCurrentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);

        cancel.setProgress(
            "Reading chunk " + new Section(chunkOrigin, chunkShape) + " from variable: " + oldVar.getShortName(), -1);

        Array data = oldVar.read(chunkOrigin, chunkShape);
        if (!getOutputFormat().isNetdf4format() && oldVar.getDataType() == DataType.STRING) {
          data = convertDataToChar(newVar, data);
        }

        if (data.getSize() > 0) { // zero when record dimension = 0
          cancel.setProgress(
              "Writing chunk " + new Section(chunkOrigin, chunkShape) + " from variable: " + oldVar.getShortName(), -1);

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
    ArrayChar newData = (ArrayChar) Array.factory(DataType.CHAR, newVar.getShape());
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


