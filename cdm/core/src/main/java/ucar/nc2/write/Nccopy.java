/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import java.util.Optional;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.CancelTaskImpl;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import ucar.nc2.util.DiskCache;

/**
 * Utility to implement nccopy
 *
 * @author caron
 * @since 3/9/2015
 */
public class Nccopy {

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input dataset.", required = true)
    public String inputFile;

    @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
    public File outputFile;

    @Parameter(names = {"-f", "--format"}, description = "Output file format. Allowed values = "
        + "[netcdf3, netcdf4, netcdf4_classic, netcdf3c, netcdf3c64, ncstream]")
    public NetcdfFileWriter.Version format = NetcdfFileWriter.Version.netcdf3;

    @Parameter(names = {"-st", "--strategy"},
        description = "Chunking strategy. Only used in NetCDF 4. " + "Allowed values = [standard, grib, none]")
    public Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;

    @Parameter(names = {"-isLargeFile", "--isLargeFile"},
        description = "Write to large file format. Only used in NetCDF 3.")
    public boolean isLargeFile;

    @Parameter(names = {"-d", "--deflateLevel"}, description = "Compression level. Only used in NetCDF 4. "
        + "Allowed values = 0 (no compression, fast) to 9 (max compression, slow)")
    public int deflateLevel = 5;

    @Parameter(names = {"-sh", "--shuffle"}, description = "Enable the shuffle filter, which may improve compression. "
        + "Only used in NetCDF 4. This option is ignored unless a non-zero deflate level is specified.")
    public boolean shuffle = true;

    @Parameter(names = "--diskCacheRoot",
        description = "Set the DiskCache root. "
            + "This parameter controls where temporary files will be stored, if necessary "
            + "(e.g. intermediate uncompressed NEXRAD files created when reading compressed files). "
            + "Must be a valid filesystem path. "
            + "Note: this directory is not automatically cleaned, so be sure to clean-up as needed.")
    public File diskCacheRoot;

    // todo - add flag to autoclean diskCacheRoot

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help;


    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList("--input", "--output", "--format", "--isLargeFile",
          "--strategy", "--deflateLevel", "--shuffle", "--diskCacheRoot", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args); // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName); // Displayed in the usage information.

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }

    public Nc4Chunking getNc4Chunking() {
      return Nc4ChunkingStrategy.factory(strategy, deflateLevel, shuffle);
    }

  }

  public static void main(String[] args) {
    String progName = Nccopy.class.getName();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      String datasetIn = cmdLine.inputFile;
      String datasetOut = cmdLine.outputFile.getAbsolutePath();
      CancelTaskImpl cancel = new CancelTaskImpl();
      Formatter errlog = new Formatter();
      System.out.printf("NetcdfDatataset read from %s write %s to %s ", datasetIn, cmdLine.format, datasetOut);

      Optional<File> diskCacheDir = Optional.ofNullable(cmdLine.diskCacheRoot);
      if (diskCacheDir.isPresent()) {
        DiskCache.setRootDirectory(diskCacheDir.get().getAbsolutePath());
        // if user has set the diskCacheRootDir, then always use it over
        // trying the "normal" locations first.
        // Was seeing an issue on cloud-mounted drives in which the I/O error
        // generated by even trying to write data next to the original file
        // caused the the JVM to close out (not the case on a local write
        // protected directory).
        DiskCache.setCachePolicy(true);
      }

      try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDatasets.openFile(datasetIn, cancel)) {

        FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, datasetOut, cmdLine.format, cmdLine.getNc4Chunking());
        writer.getNetcdfFileWriter().setLargeFile(cmdLine.isLargeFile);
        NetcdfFile ncfileOut = writer.write(cancel);

        if (ncfileOut != null)
          ncfileOut.close();
        cancel.setDone(true);
        System.out.printf("%s%n", cancel);

      } catch (Exception ex) {
        System.out.printf("%s = %s %n", ex.getClass().getName(), ex.getMessage());
        String err = errlog.toString();
        if (!err.isEmpty())
          System.out.printf(" errlog=%s%n", err);
        // e.printStackTrace();
      }

    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
