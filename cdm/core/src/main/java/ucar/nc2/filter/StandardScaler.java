package ucar.nc2.filter;

import java.io.IOException;

public class StandardScaler extends Filter {

  private static final String name = "Standard Scaler";

  private static final int id = -1;


  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    return dataIn;
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    return dataIn;
  }
}
