/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.iosp.NetcdfFileFormat;

/**
 * Configuration for CFPointWriter
 */
@Immutable
public class CFPointWriterConfig {
  private final Nc4Chunking chunking; // for netcdf-4
  private final boolean noTimeCoverage; // does not have a time dimension
  private final NetcdfFileFormat format;

  public Nc4Chunking getChunking() {
    return chunking;
  }

  boolean isNoTimeCoverage() {
    return noTimeCoverage;
  }

  public NetcdfFileFormat getFormat() {
    return format;
  }

  private CFPointWriterConfig(Builder builder) {
    this.format = builder.format;
    this.chunking = builder.chunking;
    this.noTimeCoverage = builder.noTimeCoverage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    NetcdfFileFormat format = NetcdfFileFormat.NETCDF3; // netcdf file version
    Nc4Chunking chunking = new Nc4ChunkingDefault(); // for netcdf-4
    boolean noTimeCoverage; // does not have a time dimension

    public Builder setFormat(NetcdfFileFormat format) {
      this.format = format;
      return this;
    }

    public Builder setChunking(Nc4Chunking chunking) {
      this.chunking = chunking;
      return this;
    }

    public Builder setNoTimeCoverage(boolean noTimeCoverage) {
      this.noTimeCoverage = noTimeCoverage;
      return this;
    }

    public CFPointWriterConfig build() {
      return new CFPointWriterConfig(this);
    }
  }
}
