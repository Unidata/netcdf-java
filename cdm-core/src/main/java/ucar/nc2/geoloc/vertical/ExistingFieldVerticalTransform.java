/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * Create a Vertical Transform from a variable in the dataset.
 */
@Immutable
public class ExistingFieldVerticalTransform extends AbstractVerticalTransform {
  public static final String transform_name = "explicit_field";
  public static final String existingDataField = "existingDataField";

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {

    String existingField = params.findAttributeString(existingDataField, null);
    if (existingField == null) {
      errlog.format("ExistingFieldVerticalTransform %s: existingField attribute not present%n", params.getName());
      return Optional.empty();
    }

    int rank = getRank(ds, existingField);
    if (rank != 3 && rank != 4) {
      errlog.format("ExistingFieldVerticalTransform %s: existingField has rank %d should be 3 or 4%n", params.getName(),
          rank);
      return Optional.empty();
    }
    String units = getUnits(ds, existingField);

    try {
      return Optional.of(new ExistingFieldVerticalTransform(ds, params.getName(), units, existingField, rank));
    } catch (IOException e) {
      errlog.format("OceanSigma %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String existingField;
  private final int rank;

  private ExistingFieldVerticalTransform(NetcdfDataset ds, String ctvName, String units, String existingField, int rank)
      throws IOException {
    super(ds, CF.ocean_sigma_coordinate, ctvName, units);

    this.existingField = existingField;
    this.rank = rank;
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> result = (rank == 4) ? readArray(ds, existingField, timeIndex) : readArray(ds, existingField);
    Preconditions.checkArgument(result.getRank() == 3);
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return ExistingFieldVerticalTransform.create(ds, params, errlog);
    }
  }
}

