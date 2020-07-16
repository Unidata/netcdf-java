/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** A named location on the earth. */
@Immutable
public class Station extends EarthLocation implements Comparable<Station> {

  /** Station name or id. Must be unique within the collection. May not be null. */
  public String getName() {
    return name;
  }

  /** Station description or null */
  @Nullable
  public String getDescription() {
    return desc;
  }

  /** WMO station id or null. */
  @Nullable
  public String getWmoId() {
    return wmoId;
  }

  /** The Number of obs at this station, or -1 if unknown */
  public int getNobs() {
    return nobs;
  }

  @Override
  public int compareTo(Station so) {
    return name.compareTo(so.getName());
  }

  public Station(String name, String desc, String wmoId, double lat, double lon, double alt, int nobs) {
    super(lat, lon, alt, null);
    this.name = name;
    this.desc = desc;
    this.wmoId = wmoId;
    this.nobs = nobs;
  }

  public static Builder builder(String name) {
    return new Builder(name);
  }

  /////////////////////////////////////////////////////////////////////

  private final String name;
  private final String desc;
  private final String wmoId;
  private final int nobs;

  private Station(Builder builder) {
    super(builder.latitude, builder.longitude, builder.altitude, builder.altUnits);
    this.name = builder.name;
    this.desc = builder.desc;
    this.wmoId = builder.wmoId;
    this.nobs = builder.nobs;
  }

  public static class Builder {
    private String name;
    private String desc;
    private String wmoId;
    private int nobs = -1;
    private double latitude;
    private double longitude;
    private double altitude;
    private String altUnits;

    Builder(String name) {
      this.name = name;
    }

    public Builder setName(String name) {
      this.name = name.trim();
      return this;
    }

    public Builder setDescription(String desc) {
      this.desc = desc != null ? desc.trim() : null;
      return this;
    }

    public Builder setWmoId(String wmoId) {
      this.wmoId = wmoId != null ? wmoId.trim() : null;
      return this;
    }

    public Builder setNobs(int nobs) {
      this.nobs = nobs;
      return this;
    }

    public Builder setLatitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder setLongitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder setAltitude(double altitude) {
      this.altitude = altitude;
      return this;
    }

    public Builder setAltUnits(String altUnits) {
      this.altUnits = altUnits;
      return this;
    }

    public Station build() {
      return new Station(this);
    }
  }

}
