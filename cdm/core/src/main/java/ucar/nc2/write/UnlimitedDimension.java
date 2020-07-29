/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import ucar.nc2.Dimension;

/** A Dimension whose length may change. Only used when writing. */
public class UnlimitedDimension extends Dimension {
  private int unlimitedLength;

  public UnlimitedDimension(String name, int length) {
    super(name, length, true, true, false);
    this.unlimitedLength = length;
  }

  /** Set the Dimension length. */
  public void setLength(int length) {
    Preconditions.checkArgument(length >= 0, "Unlimited Dimension length =" + length + " must >= 0");
    this.unlimitedLength = length;
  }

  /** Get the length of the Dimension. */
  @Override
  public int getLength() {
    return unlimitedLength;
  }

}
