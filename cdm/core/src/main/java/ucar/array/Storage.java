/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Abstraction of using java arrays to store Array data in. Allows multiple java arrays. */
@Immutable
abstract class Storage<T> implements Iterable<T> {

  static <T> Storage<T> factoryArrays(DataType dataType, List<Array> dataArrays) {
    switch (dataType) {
      case DOUBLE:
        return (Storage<T>) new ArrayDouble.StorageDM(dataArrays);
      case FLOAT:
        return (Storage<T>) new ArrayFloat.StorageFM(dataArrays);
      default:
        throw new RuntimeException();
    }
  }

  static <T> Storage<T> factory(DataType dataType, Object dataArray) {
    switch (dataType) {
      case DOUBLE:
        return (Storage<T>) new ArrayDouble.StorageD((double[]) dataArray);
      case FLOAT:
        return (Storage<T>) new ArrayFloat.StorageF((float[]) dataArray);
      default:
        throw new RuntimeException();
    }
  }

  abstract long getLength();

  abstract T get(long elem);

  abstract Object getPrimitiveArray();

}
