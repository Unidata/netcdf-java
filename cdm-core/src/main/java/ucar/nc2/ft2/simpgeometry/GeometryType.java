package ucar.nc2.ft2.simpgeometry;

public enum GeometryType {
  POINT("Point"), LINE("Line"), POLYGON("Polygon");

  private final String description;

  public String getDescription() {
    return this.description;
  }

  GeometryType(String description) {
    this.description = description;
  }
}
