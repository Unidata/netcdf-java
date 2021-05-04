/*
 * (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.ft2.coverage;

import javax.annotation.Nullable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import java.util.Optional;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/**
 * factory for CoverageDataset
 * 1) Remote CdmrFeatureDataset: cdmremote:url
 * 2) GRIB collections: must be a grib file, or grib index file
 * 3) DtCoverageDataset (forked from ucar.nc2.dt.grid), the cdm IOSP / CoordSys stack
 * <p>
 * Would like to add a separate implementation for FMRC collections
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {
  /**
   * @param endpoint cdmrFeature:url, local GRIB data or index file, or NetcdfDataset location
   * 
   *        <pre>
   *        java.util.Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(location);
   *        if (!opt.isPresent()) {
   *          JOptionPane.showMessageDialog(null, opt.getErrorMessage());
   *          return false;
   *        }
   *        covDatasetCollection = opt.get();
   *        </pre>
   */
  public static Optional<FeatureDatasetCoverage> openCoverageDataset(String endpoint, Formatter errLog)
      throws IOException {

    // remote cdmrFeature datasets
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {
      Optional<FeatureDataset> opt =
          ucar.nc2.ft.remote.CdmrFeatureDataset.factory(FeatureType.COVERAGE, endpoint, errLog);
      return opt.map(featureDataset -> (FeatureDatasetCoverage) featureDataset);
    }

    DatasetUrl durl = DatasetUrl.findDatasetUrl(endpoint);
    if (durl.getServiceType() == null) { // skip GRIB check for anything not a plain ole file
      // check if its GRIB collection
      GribCoverageOpenAttempt gribCoverage = openGrib(endpoint, errLog);
      if (gribCoverage.isGrib) {
        if (gribCoverage.coverage != null) {
          return Optional.of(gribCoverage.coverage);
        } else {
          return Optional.empty();
        }
      }
    }

    // adapt a DtCoverageDataset (forked from ucar.nc2.dt.GridDataset), eg a local file
    DtCoverageDataset gds = DtCoverageDataset.open(durl);
    if (!gds.getGrids().isEmpty()) {
      Formatter errlog = new Formatter();
      FeatureDatasetCoverage result = DtCoverageAdapter.factory(gds, errlog);
      return Optional.of(result);
    }

    errLog.format("Could not open as Coverage: %s", endpoint);
    return Optional.empty();
  }

  /**
   * @param endpoint cdmrFeature:url, local GRIB data or index file, or NetcdfDataset location
   * @return FeatureDatasetCoverage or null on failure. use openCoverageDataset to get error message
   */
  @Nullable
  public static FeatureDatasetCoverage open(String endpoint) throws IOException {
    Optional<FeatureDatasetCoverage> opt = openCoverageDataset(endpoint, new Formatter());
    return opt.orElse(null);
  }

  public static class GribCoverageOpenAttempt {
    public FeatureDatasetCoverage coverage; // May or may not be a FeatureDatasetCoverage
    public boolean isGrib; // We know its grib

    GribCoverageOpenAttempt(FeatureDatasetCoverage coverage, boolean isGrib) {
      this.coverage = coverage;
      this.isGrib = isGrib;
    }
  }

  /**
   * @param endpoint local GRIB data or index file
   * @return GribCoverage.
   */
  public static GribCoverageOpenAttempt openGrib(String endpoint, Formatter errLog) {
    List<Object> notGribThrowables = Arrays.asList(IllegalAccessException.class, IllegalArgumentException.class,
        ClassNotFoundException.class, NoSuchMethodException.class, NoSuchMethodError.class);

    // Use reflection to allow grib module to not be present
    try {
      Class<?> c =
          CoverageDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.coverage.GribCoverageDataset");
      Method method = c.getMethod("open", String.class, Formatter.class);
      Formatter gribErrlog = new Formatter();
      Optional<FeatureDatasetCoverage> result =
          (Optional<FeatureDatasetCoverage>) method.invoke(null, endpoint, gribErrlog);
      if (result.isPresent()) {
        return new GribCoverageOpenAttempt(result.get(), true);
      } else if (!gribErrlog.toString().isEmpty()) {
        errLog.format("%s", gribErrlog);
        return new GribCoverageOpenAttempt(null, true);
      } else {
        return new GribCoverageOpenAttempt(null, false);
      }
    } catch (Exception e) {
      for (Object noGrib : notGribThrowables) {
        // check for possible errors that are due to the file not being grib. Need to look
        // at the error causes too, as reflection error can be buried under a InvocationTargetException
        boolean notGribTopLevel = e.getClass().equals(noGrib);
        boolean notGribBuried = e.getClass().equals(InvocationTargetException.class) && e.getCause() != null
            && e.getCause().getClass().equals(noGrib);

        if (notGribTopLevel || notGribBuried) {
          return new GribCoverageOpenAttempt(null, false);
        }
      }
      // Ok, something went wrong, and it does not appear to be related to the file *not* being a grib file.
      if (e.getCause() != null) {
        errLog.format("%s", e.getCause().getMessage());
      }
      return new GribCoverageOpenAttempt(null, true);
    }

  }

  public static Optional<FeatureDatasetCoverage> openNcmlString(String ncml) throws IOException {
    NetcdfDataset ncd = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), null, null);

    DtCoverageDataset gds = new DtCoverageDataset(ncd);
    if (!gds.getGrids().isEmpty()) {
      Formatter errlog = new Formatter();
      FeatureDatasetCoverage cc = DtCoverageAdapter.factory(gds, errlog);
      return Optional.of(cc);
    }
    return Optional.empty();
  }

}
