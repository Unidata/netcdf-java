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
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeatureCollection;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/** Static methods to write Point Feature Collections into netcdf3/4 files in CF 1.6 point obs conventions. */
public class CFPointWriter {
  private static boolean debug = true;

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

    try (WriterCFPointCollection pointWriter = new WriterCFPointCollection(fileOut, fdpoint.getGlobalAttributes(),
        fdpoint.getDataVariables(), pfc.getTimeUnit(), pfc.getAltUnits(), config)) {

      pointWriter.setExtraVariables(pfc.getExtraVariables());

      int count = 0;
      for (PointFeature pf : pfc) {
        if (count == 0)
          pointWriter.writeHeader(pf);

        pointWriter.writeRecord(pf, pf.getFeatureData());
        count++;
        if (debug && count % 100 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 1000 == 0)
          System.out.printf("%n ");
      }

      pointWriter.finish();
      return count;
    }
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint dataset, StationTimeSeriesFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationCollection cfWriter = new WriterCFStationCollection(fileOut, dataset.getGlobalAttributes(),
        dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      // write all data, but no need to sort by station
      PointFeatureCollection pfc = fc.flatten(null, null, null);

      int count = 0;
      for (PointFeature pf : pfc) {
        StationPointFeature spf = (StationPointFeature) pf;
        if (count == 0)
          cfWriter.writeHeader(fc.getStationFeatures(), spf);

        cfWriter.writeRecord(spf.getStation(), pf, pf.getFeatureData());
        count++;
        if (debug && count % 100 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 1000 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFProfileCollection cfWriter = new WriterCFProfileCollection(fileOut, fdpoint.getGlobalAttributes(),
        fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      // LOOK this is lame
      // LOOK not always needed
      int count = 0;
      int name_strlen = 0;
      int nprofiles = fc.size();
      if (nprofiles < 0) {
        for (ProfileFeature pf : fc) {
          name_strlen = Math.max(name_strlen, pf.getName().length());
          count++;
        }
        nprofiles = count;
      } else {
        for (ProfileFeature pf : fc) {
          name_strlen = Math.max(name_strlen, pf.getName().length());
          count++;
          if (count > 10)
            break;
        }
      }
      cfWriter.setFeatureAuxInfo(nprofiles, name_strlen);

      count = 0;
      for (ProfileFeature profile : fc) {
        count += cfWriter.writeProfile(profile);
        if (debug && count % 10 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 100 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryFeatureCollection(FeatureDatasetPoint fdpoint, TrajectoryFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryCollection cfWriter = new WriterCFTrajectoryCollection(fileOut,
        fdpoint.getGlobalAttributes(), fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      // LOOK not always needed
      int count = 0;
      int name_strlen = 0;
      int ntrajs = fc.size();
      if (ntrajs < 0) {
        for (TrajectoryFeature traj : fc) {
          name_strlen = Math.max(name_strlen, traj.getName().length());
          count++;
        }
        ntrajs = count;
      }
      cfWriter.setFeatureAuxInfo(ntrajs, name_strlen);

      count = 0;
      for (TrajectoryFeature traj : fc) {
        count += cfWriter.writeTrajectory(traj);
        if (debug && count % 10 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 100 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeStationProfileFeatureCollection(FeatureDatasetPoint dataset,
      StationProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationProfileCollection cfWriter = new WriterCFStationProfileCollection(fileOut,
        dataset.getGlobalAttributes(), dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());
      cfWriter.setStations(fc.getStationFeatures());

      int name_strlen = 0;
      int countProfiles = 0;
      for (StationProfileFeature spf : fc) {
        name_strlen = Math.max(name_strlen, spf.getName().length());
        if (spf.size() >= 0)
          countProfiles += spf.size();
        else {
          countProfiles = Iterables.size(spf);
        }
      }
      cfWriter.setFeatureAuxInfo(countProfiles, name_strlen);

      int count = 0;
      for (StationProfileFeature spf : fc) {
        for (ProfileFeature pf : spf) {
          if (pf.getTime() == null) {
            continue; // assume this means its an "incomplete multidimensional"
          }

          count += cfWriter.writeProfile(spf, pf);
          if (debug && count % 100 == 0)
            System.out.printf("%d ", count);
          if (debug && count % 1000 == 0)
            System.out.printf("%n ");
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryProfileFeatureCollection(FeatureDatasetPoint dataset,
      TrajectoryProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryProfileCollection cfWriter = new WriterCFTrajectoryProfileCollection(fileOut,
        dataset.getGlobalAttributes(), dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      int traj_strlen = 0;
      int prof_strlen = 0;
      int countTrajectories = 0;
      int countProfiles = 0;
      for (TrajectoryProfileFeature spf : fc) {
        countTrajectories++;
        traj_strlen = Math.max(traj_strlen, spf.getName().length());
        if (spf.size() >= 0)
          countProfiles += spf.size();
        else {
          for (ProfileFeature profile : spf) {
            prof_strlen = Math.max(prof_strlen, profile.getName().length());
            countProfiles++;
          }
        }
      }
      cfWriter.setFeatureAuxInfo(countProfiles, prof_strlen);
      cfWriter.setFeatureAuxInfo2(countTrajectories, traj_strlen);

      int count = 0;
      for (TrajectoryProfileFeature spf : fc) {
        for (ProfileFeature profile : spf) {
          if (profile.getTime() == null)
            continue; // assume this means its a "incomplete multidimensional"

          count += cfWriter.writeProfile(spf, profile);
          if (debug && count % 100 == 0)
            System.out.printf("%d ", count);
          if (debug && count % 1000 == 0)
            System.out.printf("%n ");
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
