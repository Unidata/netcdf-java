/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.Iterables;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.grid2.GridNetcdfDataset;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** A factory of Grid Datasets. */
public class GridDatasetFactory {

  // LOOK since we want GridDataset in a try-with-resource block, use Nullable instead of Optional. Can use orElse(null)
  @Nullable
  public static GridDataset openGridDataset(String endpoint, Formatter errLog) throws IOException {

    // LOOK could be a gcdm endpoint ??

    // check if its a GRIB collection
    DatasetUrl durl = DatasetUrl.findDatasetUrl(endpoint);
    if (durl.getServiceType() == null) { // skip GRIB check for anything not a plain ole file
      GribOpenAttempt openAttempt = openGrib(endpoint, errLog);
      if (openAttempt.isGrib) {
        return openAttempt.coverage;
      }
    }
    // this will still open a GRIB Collection, but it will be built on top of NetcdfDataset.
    // probably ok for small collections, though it differs from the direct GRIB.
    // if tests start failing, check if GRIB module is installed

    return openNetcdfAsGrid(endpoint, errLog);
  }

  // Open a NetcdfDataset and wrap as a GridDataset if possible.
  @Nullable
  public static GridDataset openNetcdfAsGrid(String endpoint, Formatter errLog) throws IOException {
    // Otherwise, wrap a NetcdfDataset
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(endpoint);
    Optional<GridNetcdfDataset> result =
        GridNetcdfDataset.create(ds, errLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
    if (result.isEmpty()) {
      errLog.format("Could not open as GridDataset: %s", endpoint);
      ds.close();
      return null;
    }

    return result.get();
  }

  // Wrap an already open NetcdfDataset as a GridDataset if possible.
  public static Optional<GridDataset> wrapGridDataset(NetcdfDataset ds, Formatter errLog) throws IOException {
    Optional<GridNetcdfDataset> result =
        GridNetcdfDataset.create(ds, errLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
    if (result.isEmpty()) {
      errLog.format("Could not open as GridDataset: %s", ds.getLocation());
      return Optional.empty();
    }
    return Optional.of(result.get());
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // call Grib with reflection, to decouple the modules

  private static class GribOpenAttempt {
    @Nullable
    public GridDataset coverage;
    public boolean isGrib; // We know if its grib or not

    GribOpenAttempt(@Nullable GridDataset coverage, boolean isGrib) {
      this.coverage = coverage;
      this.isGrib = isGrib;
    }
  }

  public static GribOpenAttempt openGrib(String endpoint, Formatter errLog) throws IOException {
    List<Object> notGribThrowables = Arrays.asList(IllegalAccessException.class, IllegalArgumentException.class,
        ClassNotFoundException.class, NoSuchMethodException.class, NoSuchMethodError.class);

    // LOOK what happens when grib module is not present?
    try {
      Class<?> c = GridDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.grid.GribGridDataset");
      Method method = c.getMethod("open", String.class, Formatter.class);
      Formatter gribErrlog = new Formatter();
      Optional<GridDataset> result = (Optional<GridDataset>) method.invoke(null, endpoint, gribErrlog);
      if (result.isPresent()) {
        return new GribOpenAttempt(result.get(), true);
      } else if (!gribErrlog.toString().isEmpty()) {
        errLog.format("%s", gribErrlog);
        return new GribOpenAttempt(null, true);
      } else {
        return new GribOpenAttempt(null, false);
      }
    } catch (Exception e) {
      // propagate IOException
      if (e instanceof InvocationTargetException) {
        InvocationTargetException ite = (InvocationTargetException) e;
        if (ite.getCause() instanceof IOException) {
          throw (IOException) ite.getCause();
        }
      }
      for (Object noGrib : notGribThrowables) {
        // check for possible errors that are due to the file not being grib. Need to look
        // at the error causes too, as reflection error can be buried under a InvocationTargetException
        boolean notGribTopLevel = e.getClass().equals(noGrib);
        boolean notGribBuried = e.getClass().equals(InvocationTargetException.class) && e.getCause() != null
            && e.getCause().getClass().equals(noGrib);

        if (notGribTopLevel || notGribBuried) {
          return new GribOpenAttempt(null, false);
        }
      }
      // Ok, something went wrong, and it does not appear to be related to the file *not* being a grib file.
      if (e.getCause() != null) {
        errLog.format("%s", e.getCause().getMessage());
      }
      return new GribOpenAttempt(null, true);
    }
  }
}
