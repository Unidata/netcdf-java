package ucar.nc2;

import com.google.common.base.Preconditions;

/**
 * A dimension whose length can change, thus mutable.
 * Only use when writing.
 */
public class UnlimitedDimension extends Dimension {
  private int ulength;

  UnlimitedDimension(Builder builder, int length) {
    super(builder);
    this.ulength = length;
  }

  /**
   * Set the Dimension length.
   *
   * @param n length of Dimension
   */
  public void setUnlimitedLength(int n) {
    Preconditions.checkArgument(n >= 0);
    this.ulength = n;
  }

  /**
   * Get the length of the Unlimited Dimension.
   * @return length of Dimension
   */
  public int getLength() {
    return ulength;
  }

}
