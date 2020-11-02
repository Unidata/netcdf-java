package ucar.nc2.grid;

import com.google.common.collect.Iterables;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.grid.GridDatasetImpl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

public class GridDatasetFactory {

  @Nullable
  public static GridDataset openGridDataset(String endpoint, Formatter errLog) throws IOException {

    /* Grib is handled specially
    DatasetUrl durl = DatasetUrl.findDatasetUrl(endpoint);
    if (durl.getServiceType() == null) { // skip GRIB check for anything not a plain ole file
      // check if its GRIB collection
      GribDatasetOpenAttempt gribCoverage = openGrib(endpoint, errLog);
      if (gribCoverage.isGrib) {
        if (gribCoverage.gribDataset != null) {
          return gribCoverage.gribDataset;
        } else {
          return null;
        }
      }
    } */

    // Otherwise, wrap a NetcdfDataset
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(endpoint);
    Optional<GridDatasetImpl> result =
        GridDatasetImpl.create(ds, errLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
    if (!result.isPresent()) {
      errLog.format("Could not open as GridDataset: %s", endpoint);
      ds.close();
      return null;
    }

    return result.get();
  }

  static class GribDatasetOpenAttempt {
    @Nullable
    GridDataset gribDataset;
    boolean isGrib; // We know its grib

    public GribDatasetOpenAttempt(@Nullable GridDataset gribDataset, boolean isGrib) {
      this.gribDataset = gribDataset;
      this.isGrib = isGrib;
    }
  }

  private static GribDatasetOpenAttempt openGrib(String endpoint, Formatter errLog) {
    List<Object> notGribThrowables = Arrays.asList(IllegalAccessException.class, IllegalArgumentException.class,
        ClassNotFoundException.class, NoSuchMethodException.class, NoSuchMethodError.class);

    // Use reflection to allow grib module to not be present
    try {
      Class<?> c = GridDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.coverage.GribDatasetFactory");
      Method method = c.getMethod("open", String.class, Formatter.class);
      Formatter gribErrlog = new Formatter();
      Optional<GridDataset> result = (Optional<GridDataset>) method.invoke(null, endpoint, gribErrlog);
      if (result.isPresent()) {
        return new GribDatasetOpenAttempt(result.get(), true);
      } else if (!gribErrlog.toString().isEmpty()) {
        errLog.format("%s", gribErrlog);
        return new GribDatasetOpenAttempt(null, true);
      } else {
        return new GribDatasetOpenAttempt(null, false);
      }
    } catch (Exception e) {
      for (Object noGrib : notGribThrowables) {
        // check for possible errors that are due to the file not being grib. Need to look
        // at the error causes too, as reflection error can be buried under a InvocationTargetException
        boolean notGribTopLevel = e.getClass().equals(noGrib);
        boolean notGribBuried = e.getClass().equals(InvocationTargetException.class) && e.getCause() != null
            && e.getCause().getClass().equals(noGrib);

        if (notGribTopLevel || notGribBuried) {
          return new GribDatasetOpenAttempt(null, false);
        }
      }
      // Ok, something went wrong, and it does not appear to be related to the file *not* being a grib file.
      if (e.getCause() != null) {
        errLog.format("%s", e.getCause().getMessage());
      }
      return new GribDatasetOpenAttempt(null, true);
    }

  }


}
