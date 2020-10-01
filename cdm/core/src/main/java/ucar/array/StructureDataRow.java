/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import ucar.array.ArrayDouble.StorageD;
import ucar.array.ArrayFloat.StorageF;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;

/** Experimental, used in Cdmr */
public class StructureDataRow extends StructureData {
  private final ByteBuffer bbuffer;

  public StructureDataRow(StructureMembers members) {
    super(members);
    this.bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
  }

  public Array getMemberData(Member m) {
    DataType dataType = m.getDataType();
    int offset = m.getOffset();
    int size = m.length();

    switch (dataType) {
      case DOUBLE:
        double[] darray = new double[size];
        for (int count = 0; count < size; count++) {
          darray[count] = bbuffer.getDouble(offset + 8 * count);
        }
        return new ArrayDouble(m.getShape(), new StorageD(darray));
      case FLOAT:
        float[] farray = new float[size];
        for (int count = 0; count < size; count++) {
          farray[count] = bbuffer.getFloat(offset + 4 * count);
        }
        return new ArrayFloat(m.getShape(), new StorageF(farray));
      default:
        throw new RuntimeException("unknown dataType " + dataType);
    }
  }

  public void setMemberData(Member m, Array data) {
    Preconditions.checkArgument(data.length() == m.length());
    DataType dataType = m.getDataType();
    int offset = m.getOffset();
    int count = 0;

    switch (dataType) {
      case DOUBLE:
        Array<Double> ddata = (Array<Double>) data;
        for (double val : ddata) {
          bbuffer.putDouble(offset + 8 * count, val);
          count++;
        }
        break;
      case FLOAT:
        Array<Float> fdata = (Array<Float>) data;
        for (float val : fdata) {
          bbuffer.putFloat(offset + 4 * count, val);
          count++;
        }
        break;
      default:
        throw new RuntimeException("unknown dataType " + dataType);
    }
  }

}
