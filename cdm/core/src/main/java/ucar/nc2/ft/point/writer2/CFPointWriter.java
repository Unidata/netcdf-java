/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer2;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeatureCollection;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/** Static methods to write Point Feature Collections into netcdf3/4 files in CF 1.6 point obs conventions. */
public class CFPointWriter {
  private static boolean debug = true;
  private static final Logger logger = LoggerFactory.getLogger(ucar.nc2.ft.point.writer2.CFPointWriter.class);


  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, NetcdfFileFormat format)
      throws IOException {
    return writeFeatureCollection(fdpoint, fileOut, CFPointWriterConfig.builder().setFormat(format).build());
  }

  /**
   * Write a FeatureDatasetPoint to a netcd3/4 file.
   *
   * @param fdpoint the FeatureDatasetPoint; do first FeatureCollection contained within.
   * @param fileOut write to the is file
   * @param config configuration
   * @return count of number of pointFeatures written.
   */
  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, CFPointWriterConfig config)
      throws IOException {
    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof TrajectoryFeatureCollection) {
        return writeTrajectoryFeatureCollection(fdpoint, (TrajectoryFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof StationProfileFeatureCollection) {
        return writeStationProfileFeatureCollection(fdpoint, (StationProfileFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof TrajectoryProfileFeatureCollection) {
        return writeTrajectoryProfileFeatureCollection(fdpoint, (TrajectoryProfileFeatureCollection) fc, fileOut,
            config);
      }
    }
    return 0;
  }

  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFPointCollection pointWriter = new WriterCFPointCollection(fileOut, fdpoint.attributes(),
        fdpoint.getDataVariables(), pfc.getTimeUnit(), pfc.getAltUnits(), config)) {

      pointWriter.setExtraVariables(pfc.getExtraVariables());
      pointWriter.writeHeader(fdpoint.getPointFeatureCollectionList());

      int count = 0;
      for (DsgFeatureCollection pointFeatures : fdpoint.getPointFeatureCollectionList()) {
        for (PointFeature pf : (PointFeatureCollection) pointFeatures) {
          pointWriter.writeRecord(pf);
          count++;
          if (debug && count % 100 == 0)
            logger.debug(String.format("%d ", count));
          if (debug && count % 1000 == 0)
            logger.debug(String.format("%n "));
        }
      }

      pointWriter.finish();
      return count;
    }
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint dataset, StationTimeSeriesFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationCollection cfWriter = new WriterCFStationCollection(fileOut, dataset.attributes(),
        dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {


      List<StationFeature> flattenFeatures = new ArrayList<>();
      List<Variable> extraVariables = new ArrayList<>();
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        extraVariables.addAll(featureCollection.getExtraVariables());
        flattenFeatures.addAll(((StationTimeSeriesFeatureCollection) featureCollection).getStationFeatures());
      }

      cfWriter.setExtraVariables(extraVariables);
      cfWriter.writeHeader(flattenFeatures);
      int count = 0;
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        cfWriter.resetObsIndex();
        for (PointFeatureCollection pointCollection : (StationTimeSeriesFeatureCollection) featureCollection) {
          for (PointFeature point : pointCollection) {
            cfWriter.writeObsData(point);
            count++;
            if (debug && count % 100 == 0)
              logger.debug(String.format("%d ", count));
            if (debug && count % 1000 == 0)
              logger.debug(String.format("%n "));
          }
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFProfileCollection cfWriter = new WriterCFProfileCollection(fileOut, fdpoint.attributes(),
        fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      List<ProfileFeature> flattenFeatures = new ArrayList<>();
      List<Variable> extraVariables = new ArrayList<>();

      // LOOK this is lame
      // LOOK not always needed
      int count = 0;
      int name_strlen = 0;
      int nprofiles = 0;
      for (DsgFeatureCollection featureCollection : fdpoint.getPointFeatureCollectionList()) {
        nprofiles += featureCollection.size();
        extraVariables.addAll(featureCollection.getExtraVariables());
        if (nprofiles < 0) {
          for (ProfileFeature profile : (ProfileFeatureCollection) featureCollection) {
            flattenFeatures.add(profile);
            name_strlen = Math.max(name_strlen, profile.getName().length());
            count++;
          }
          nprofiles = count;
        } else {
          for (ProfileFeature profile : (ProfileFeatureCollection) featureCollection) {
            flattenFeatures.add(profile);
            name_strlen = Math.max(name_strlen, profile.getName().length());
            count++;
            if (count > 10)
              break;
          }
        }
      }
      cfWriter.setExtraVariables(extraVariables);
      cfWriter.setFeatureAuxInfo(nprofiles, name_strlen);
      cfWriter.writeHeader(flattenFeatures);

      count = 0;
      for (DsgFeatureCollection featureCollection : fdpoint.getPointFeatureCollectionList()) {
        for (ProfileFeature profile : (ProfileFeatureCollection) featureCollection) {
          count += cfWriter.writeProfile(profile);
          if (debug && count % 10 == 0)
            logger.debug(String.format("%d ", count));
          if (debug && count % 100 == 0)
            logger.debug(String.format("%n "));
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryFeatureCollection(FeatureDatasetPoint fdpoint, TrajectoryFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryCollection cfWriter = new WriterCFTrajectoryCollection(fileOut, fdpoint.attributes(),
        fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      List<TrajectoryFeature> flattenFeatures = new ArrayList<>();
      List<Variable> extraVariables = new ArrayList<>();
      // LOOK not always needed
      int npoints = 0;
      int name_strlen = 0;
      int ntrajs = 0;
      for (DsgFeatureCollection featureCollection : fdpoint.getPointFeatureCollectionList()) {
        for (TrajectoryFeature trajectory : (TrajectoryFeatureCollection) featureCollection) {
          flattenFeatures.add(trajectory);
          extraVariables.addAll(trajectory.getExtraVariables());
          name_strlen = Math.max(name_strlen, trajectory.getName().length());
          npoints++;
        }
      }
      ntrajs = npoints;
      cfWriter.setExtraVariables(extraVariables);
      cfWriter.setFeatureAuxInfo(ntrajs, name_strlen);
      cfWriter.writeHeader(flattenFeatures);

      npoints = 0;
      for (DsgFeatureCollection featureCollection : fdpoint.getPointFeatureCollectionList()) {
        for (TrajectoryFeature trajectory : (TrajectoryFeatureCollection) featureCollection) {
          npoints += cfWriter.writeTrajectory(trajectory);
          if (debug && npoints % 10 == 0)
            logger.debug(String.format("%d ", npoints));
          if (debug && npoints % 100 == 0)
            logger.debug(String.format("%n "));
        }
      }
      cfWriter.finish();
      return npoints;
    }
  }

  private static int writeStationProfileFeatureCollection(FeatureDatasetPoint dataset,
      StationProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationProfileCollection cfWriter = new WriterCFStationProfileCollection(fileOut, dataset.attributes(),
        dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      List<Variable> extraVariables = new ArrayList<>();
      List<StationFeature> flattenFeatures = new ArrayList<>();

      int name_strlen = 0;
      int countProfiles = 0;
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        extraVariables.addAll(featureCollection.getExtraVariables());
        for (StationFeature station : ((StationProfileFeatureCollection) featureCollection).getStationFeatures()) {
          flattenFeatures.add(station);
          name_strlen = Math.max(name_strlen, station.getName().length());
          if (((StationProfileFeature) station).size() >= 0)
            countProfiles += ((StationProfileFeature) station).size();
          else {
            countProfiles += Iterables.size(((StationProfileFeature) station));
          }
        }
      }

      cfWriter.setExtraVariables(extraVariables);
      cfWriter.setStations(flattenFeatures);
      cfWriter.setFeatureAuxInfo(countProfiles, name_strlen);
      cfWriter.writeHeader(flattenFeatures);


      int count = 0;
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        cfWriter.resetObsIndex();
        for (StationFeature station : ((StationProfileFeatureCollection) featureCollection).getStationFeatures()) {
          for (ProfileFeature profile : (StationProfileFeature) station) {
            if (profile.getTime() == null) {
              continue; // assume this means its an "incomplete multidimensional"
            }
            count += cfWriter.writeProfile(profile);
            if (debug && count % 100 == 0)
              logger.debug(String.format("%d ", count));
            if (debug && count % 1000 == 0)
              logger.debug(String.format("%n "));
          }
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryProfileFeatureCollection(FeatureDatasetPoint dataset,
      TrajectoryProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryProfileCollection cfWriter = new WriterCFTrajectoryProfileCollection(fileOut,
        dataset.attributes(), dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      List<Variable> extraVariables = new ArrayList<>();
      List<TrajectoryProfileFeature> flattenFeatures = new ArrayList<>();

      int traj_strlen = 0;
      int prof_strlen = 0;
      int countTrajectories = 0;
      int countProfiles = 0;
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        for (TrajectoryProfileFeature trajProf : ((TrajectoryProfileFeatureCollection) featureCollection)) {
          flattenFeatures.add(trajProf);
          extraVariables.addAll(trajProf.getExtraVariables());
          countTrajectories++;
          traj_strlen = Math.max(traj_strlen, trajProf.getName().length());
          if (trajProf.size() >= 0)
            countProfiles += trajProf.size();
          else {
            for (ProfileFeature profile : trajProf) {
              prof_strlen = Math.max(prof_strlen, profile.getName().length());
              countProfiles++;
            }
          }
        }
      }
      cfWriter.setExtraVariables(extraVariables);
      cfWriter.setFeatureAuxInfo(countProfiles, prof_strlen);
      cfWriter.setFeatureAuxInfo2(countTrajectories, traj_strlen);
      cfWriter.writeHeader(flattenFeatures);

      int count = 0;
      for (DsgFeatureCollection featureCollection : dataset.getPointFeatureCollectionList()) {
        for (TrajectoryProfileFeature trajProf : (TrajectoryProfileFeatureCollection) featureCollection) {
          for (ProfileFeature profile : trajProf) {
            if (profile.getTime() == null)
              continue; // assume this means its a "incomplete multidimensional"
            count += cfWriter.writeProfile(trajProf, profile);
            if (debug && count % 100 == 0)
              logger.debug(String.format("%d ", count));
            if (debug && count % 1000 == 0)
              logger.debug(String.format("%n "));
          }
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input file.", required = true)
    File inputFile;

    @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
    File outputFile;

    @Parameter(names = {"-f", "--format"}, description = "Output file format. Allowed values = "
        + "[netcdf3, netcdf4, netcdf4_classic, netcdf3c, netcdf3c64, ncstream]")
    public NetcdfFileWriter.Version format = NetcdfFileWriter.Version.netcdf3;

    @Parameter(names = {"-st", "--strategy"},
        description = "Chunking strategy. Only used in NetCDF 4. " + "Allowed values = [standard, grib, none]")
    public Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;

    @Parameter(names = {"-d", "--deflateLevel"}, description = "Compression level. Only used in NetCDF 4. "
        + "Allowed values = 0 (no compression, fast) to 9 (max compression, slow)")
    public int deflateLevel = 5;

    @Parameter(names = {"-sh", "--shuffle"}, description = "Enable the shuffle filter, which may improve compression. "
        + "Only used in NetCDF 4. This option is ignored unless a non-zero deflate level is specified.")
    public boolean shuffle = true;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help;


    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames =
          Arrays.asList("--input", "--output", "--format", "--strategy", "--deflateLevel", "--shuffle", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    private final JCommander jc;

    CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args); // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName); // Displayed in the usage information.

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    void printUsage() {
      jc.usage();
    }

    Nc4Chunking getNc4Chunking() {
      return Nc4ChunkingStrategy.factory(strategy, deflateLevel, shuffle);
    }

    public CFPointWriterConfig getCFPointWriterConfig() {
      return CFPointWriterConfig.builder().setFormat(NetcdfFormatWriter.convertToNetcdfFileFormat(format))
          .setChunking(getNc4Chunking()).build();
    }
  }

  public static void main(String[] args) throws Exception {
    String progName = CFPointWriter.class.getName();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      FeatureType wantFeatureType = FeatureType.ANY_POINT;
      String location = cmdLine.inputFile.getAbsolutePath();
      CancelTask cancel = null;
      Formatter errlog = new Formatter();

      try (FeatureDatasetPoint fdPoint =
          (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(wantFeatureType, location, cancel, errlog)) {
        if (fdPoint == null) {
          System.err.println(errlog);
        } else {
          System.out.printf("CFPointWriter: reading from %s, writing to %s%n", cmdLine.inputFile, cmdLine.outputFile);
          writeFeatureCollection(fdPoint, cmdLine.outputFile.getAbsolutePath(), cmdLine.getCFPointWriterConfig());
          System.out.println("Done.");
        }
      }
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
