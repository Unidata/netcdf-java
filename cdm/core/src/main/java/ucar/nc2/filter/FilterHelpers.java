package ucar.nc2.filter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;

import java.nio.*;

import static ucar.ma2.DataType.*;

public class FilterHelpers {

  public static Array bytesToArray(byte[] in, DataType type, ByteOrder order) {
    return Array.factory(type, new int[] {in.length / type.getSize()}, convertToType(in, type, order));
  }

  public static byte[] arrayToBytes(Array arr, DataType type, ByteOrder order) {
    ByteBuffer bb = ByteBuffer.allocate((int) arr.getSize() * type.getSize());
    bb.order(order);

    IndexIterator ii = arr.getIndexIterator();
    while (ii.hasNext()) {
      switch (type) {
        case BYTE:
        case UBYTE:
          bb.put(ii.getByteNext());
          break;
        case SHORT:
        case USHORT:
          bb.putShort(ii.getShortNext());
          break;
        case INT:
        case UINT:
          bb.putInt(ii.getIntNext());
          break;
        case LONG:
        case ULONG:
          bb.putLong(ii.getLongNext());
          break;
        case FLOAT:
          bb.putFloat(ii.getFloatNext());
          break;
        case DOUBLE:
          bb.putDouble(ii.getDoubleNext());
          break;
      }
    }
    return bb.array();
  }

  private static Object convertToType(byte[] dataIn, DataType wantType, ByteOrder bo) {
    if (wantType.getSize() == 1) {
      return dataIn;
    } // no need for conversion

    ByteBuffer bb = ByteBuffer.wrap(dataIn);
    bb.order(bo);
    switch (wantType) {
      case SHORT:
      case USHORT:
        ShortBuffer sb = bb.asShortBuffer();
        short[] shortArray = new short[sb.limit()];
        sb.get(shortArray);
        return shortArray;
      case INT:
      case UINT:
        IntBuffer ib = bb.asIntBuffer();
        int[] intArray = new int[ib.limit()];
        ib.get(intArray);
        return intArray;
      case LONG:
      case ULONG:
        LongBuffer lb = bb.asLongBuffer();
        long[] longArray = new long[lb.limit()];
        lb.get(longArray);
        return longArray;
      case FLOAT:
        FloatBuffer fb = bb.asFloatBuffer();
        float[] floatArray = new float[fb.limit()];
        fb.get(floatArray);
        return floatArray;
      case DOUBLE:
        DoubleBuffer db = bb.asDoubleBuffer();
        double[] doubleArray = new double[db.limit()];
        db.get(doubleArray);
        return doubleArray;
      default:
        return bb.array();
    }
  }

  /**
   * Returns the smallest numeric data type that:
   * <ol>
   * <li>can hold a larger integer than {@code dataType} can</li>
   * <li>if integral, has the same signedness as {@code dataType}</li>
   * </ol>
   * <p/>
   * <table border="1">
   * <tr>
   * <th>Argument</th>
   * <th>Result</th>
   * </tr>
   * <tr>
   * <td>BYTE</td>
   * <td>SHORT</td>
   * </tr>
   * <tr>
   * <td>UBYTE</td>
   * <td>USHORT</td>
   * </tr>
   * <tr>
   * <td>SHORT</td>
   * <td>INT</td>
   * </tr>
   * <tr>
   * <td>USHORT</td>
   * <td>UINT</td>
   * </tr>
   * <tr>
   * <td>INT</td>
   * <td>LONG</td>
   * </tr>
   * <tr>
   * <td>UINT</td>
   * <td>ULONG</td>
   * </tr>
   * <tr>
   * <td>LONG</td>
   * <td>DOUBLE</td>
   * </tr>
   * <tr>
   * <td>ULONG</td>
   * <td>DOUBLE</td>
   * </tr>
   * <tr>
   * <td>Any other data type</td>
   * <td>Just return argument</td>
   * </tr>
   * </table>
   * <p/>
   * The returned type is intended to be just big enough to hold the result of performing an unsigned conversion of a
   * value of the smaller type. For example, the {@code byte} value {@code -106} equals {@code 150} when interpreted
   * as unsigned. That won't fit in a (signed) {@code byte}, but it will fit in a {@code short}.
   *
   * @param dataType an integral data type.
   * @return the next larger type.
   */
  public static DataType nextLarger(DataType dataType) {
    switch (dataType) {
      case BYTE:
        return SHORT;
      case UBYTE:
        return USHORT;
      case SHORT:
        return INT;
      case USHORT:
        return UINT;
      case INT:
        return LONG;
      case UINT:
        return ULONG;
      case LONG:
      case ULONG:
        return DOUBLE;
      default:
        return dataType;
    }
  }

  // Get the data type of an attribute. Make it unsigned if the variable is unsigned.
  public static DataType getAttributeDataType(Attribute attribute, DataType.Signedness signedness) {
    DataType dataType = attribute.getDataType();
    if (signedness == Signedness.UNSIGNED) {
      // If variable is unsigned, make its integral attributes unsigned too.
      dataType = dataType.withSignedness(signedness);
    }
    return dataType;
  }


  public static int rank(DataType dataType) {
    if (dataType == null) {
      return -1;
    }

    switch (dataType) {
      case BYTE:
        return 0;
      case UBYTE:
        return 1;
      case SHORT:
        return 2;
      case USHORT:
        return 3;
      case INT:
        return 4;
      case UINT:
        return 5;
      case LONG:
        return 6;
      case ULONG:
        return 7;
      case FLOAT:
        return 8;
      case DOUBLE:
        return 9;
      default:
        return -1;
    }
  }

  public static DataType largestOf(DataType... dataTypes) {
    DataType widest = null;
    for (DataType dataType : dataTypes) {
      if (widest == null) {
        widest = dataType;
      } else if (rank(dataType) > rank(widest)) {
        widest = dataType;
      }
    }
    return widest;
  }
}
