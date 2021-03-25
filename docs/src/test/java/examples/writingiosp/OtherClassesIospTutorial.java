package examples.writingiosp;

import ucar.array.*;
import ucar.nc2.Variable;
import java.util.Iterator;

public class OtherClassesIospTutorial {

  public static void makeArray(Variable v) {
    // To make an Array object for data in a Variable, v:
    int size = (int) v.getSize();
    short[] jarray = new short[size];
    // ...
    // read data into jarray
    // ...
    Array data = Arrays.factory(v.getArrayType(), v.getShape(), jarray);
  }

  public static void arrayIterator(Array<Float> data){
    Iterator<Float> it = data.iterator();
    while(it.hasNext()) {
      float val = it.next();
      // do something
    }
  }

  public static void arrayIndices(Array<Float> data) {
    float val = data.get(5);
    val = data.get(data.getIndex());
  }
}
