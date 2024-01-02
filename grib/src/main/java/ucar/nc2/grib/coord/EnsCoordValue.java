package ucar.nc2.grib.coord;

import java.util.Formatter;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.util.Misc;

@Immutable
public class EnsCoordValue implements Comparable<EnsCoordValue> {
  private final int code;
  private final int ensMember;
  private final int ensNumber;

  public EnsCoordValue(int code, int ensMember) {
    this(code, ensMember, 0);
  }

  public EnsCoordValue(int code, int ensMember, int ensNumber) {
    this.code = code;
    this.ensMember = ensMember;
    this.ensNumber = ensNumber;
  }

  public int getCode() {
    return code;
  }

  public int getEnsMember() {
    return ensMember;
  }

  public int getEnsNumber() {
    return ensNumber;
  }

  @Override
  public int compareTo(@Nonnull EnsCoordValue o) {
    int r = Integer.compare(code, o.code);
    if (r != 0)
      return r;
    r = Integer.compare(ensNumber, o.ensNumber);
    if (r != 0)
      return r;
    return Integer.compare(ensMember, o.ensMember);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnsCoordValue that = (EnsCoordValue) o;
    return code == that.code && ensMember == that.ensMember && ensNumber == that.ensNumber;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * ensMember;
    result += 31 * code;
    result += 31 * ensNumber;
    return result;
  }

  public String toString() {
    try (Formatter out = new Formatter()) {
      out.format("(%d %d %d)", code, ensMember, ensNumber);
      return out.toString();
    }
  }
}
