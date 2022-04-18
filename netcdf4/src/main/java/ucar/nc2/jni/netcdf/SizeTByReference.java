/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

/**
 * Reference to SizeT, for return values
 *
 * TODO: This will move packages in version 6.
 * TODO: This may not be needed in version 6.
 *
 * @see <a href="https://github.com/twall/jna/issues/191" />
 */
public class SizeTByReference extends ByReference {
  public SizeTByReference() {
    this(new SizeT());
  }

  public SizeTByReference(SizeT value) {
    super(Native.SIZE_T_SIZE);
    setValue(value);
  }

  public void setValue(SizeT value) {
    Pointer p = getPointer();
    if (Native.SIZE_T_SIZE == 8) {
      p.setLong(0, value.longValue());
    } else {
      p.setInt(0, value.intValue());
    }
  }

  public SizeT getValue() {
    Pointer p = getPointer();
    return new SizeT(Native.SIZE_T_SIZE == 8 ? p.getLong(0) : p.getInt(0));
  }

  public long longValue() {
    return this.getValue().longValue();
  }

  public int intValue() {
    return this.getValue().intValue();
  }
}
