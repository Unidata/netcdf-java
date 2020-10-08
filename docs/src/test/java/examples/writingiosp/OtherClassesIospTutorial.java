package examples.writingiosp;

import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

public class OtherClassesIospTutorial {

  public static void makeArray(Variable v) {
    // To make an Array object for data in a Variable, v:
    int size = (int) v.getSize();
    short[] jarray = new short[size];
    // ...
    // read data into jarray
    // ...
    Array data = Array.factory(v.getDataType(), v.getShape(), jarray);
  }

  public static void arrayIndexIterator(RandomAccessFile raf, Variable v) throws IOException {
    Array data = Array.factory(v.getDataType(), v.getShape());
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      ii.setShortNext(raf.readShort());
    }
  }

  public static void arrayIndex(RandomAccessFile raf) throws IOException {
    Array data = Array.factory(DataType.DOUBLE, new int[] {5, 190});
    Index ima = data.getIndex();
    for (int i = 0; i < 190; i++) {
      ima.set(0, i); // set index 0
      for (int j = 0; j < 5; j++) {
        ima.set(1, j); // set index 1
        data.setDouble(ima, raf.readDouble());
      }
    }
  }

  public static void arrayRankAndType(RandomAccessFile raf) throws IOException {
    ArrayDouble.D2 data = new ArrayDouble.D2(190, 5);
    for (int i = 0; i < 190; i++)
      for (int j = 0; j < 5; j++)
        data.set(i, j, raf.readDouble());
  }
}
