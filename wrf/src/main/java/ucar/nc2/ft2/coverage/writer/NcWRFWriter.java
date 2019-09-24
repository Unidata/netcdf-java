/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Comparator;
import java.util.Arrays;
import java.util.ArrayList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.util.Optional;


/**
 * Utility to create WRF intermediate files from netCDF files using Coverages.
 *
 * @author hvandam
 * @since 6/1/2019
 */

public class NcWRFWriter {

  static final String METGRID_NAME = "Metgrid_Name";
  static final String METGRID_UNIT = "Metgrid_Unit";
  static final String METGRID_DESC = "Metgrid_Desc";

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input dataset name.", required = true)
    public File inputFile = null;

    @Parameter(names = {"-o", "--output"}, description = "Output file name.", required = true)
    public String outputFileName;

    @Parameter(names = {"-v", "--vtable"}, description = "Generate the output from variables found in this vtable.",
        required = true)
    public File vtableFile = null;

    @Parameter(names = {"-a", "--variablefile"}, description = "Generate the output from this list of variables from the " +
        "input dataset.", variableArity = true)
    public File variableFile = null;

    @Parameter(names = {"-f", "--vfile"}, description = "Generate the output from an xml file containing " +
        "a list of variables (a varset).")
    public File varsetFile = null;

    @Parameter(names = {"-s", "--showvars"}, description = "Display a list of variables from the input dataset.")
    public boolean show = false;

    @Parameter(names = {"-h", "--help"}, description = "You must provide an input dataset name, an output file name, " +
        "and ONE option: -v (--vtable), -a (--variablefile), -f (--vfile), or -s(--showvars).", help = true)
    public boolean help = false;

    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {

      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList(
          "--input", "--output", "--vtable", "--variablefile", "--vfile", "--showvars", "--help");

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

      this.jc = new JCommander(this, null,args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.

      // Set the ordering of parameters in the usage information.
      jc.setParameterDescriptionComparator(new NcWRFWriter.CommandLine.ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }

    public void validateParms() {
      if (!inputFile.exists())
        throw new ParameterException("The input file: " + inputFile + " was not found.");

      if (outputFileName.isEmpty())
        throw new ParameterException("The output file name: " + outputFileName + " was not found.");

      if (!vtableFile.exists())
        throw new ParameterException("The vtable file: " + vtableFile + " was not found.");

      // optional arguments, only one is allowed, we don't care which one at this point.
      //     int a = vars.isEmpty() ? 0 : 1;
      //     int b = varsetFile == null ? 0 : 1;
      //     int c = show ? 1 : 0;

      //     int res = a+b+c;
      //     if( res != 1)
      //         throw new ParameterException("Incorrect number of optional arguments, use one and only one.");
    }
  }  // end class CommandLine

  private static void displayCoverages(ArrayList<Coverage> cov) {

    System.out.println("\nCoverages found = " + cov.size());
    cov.forEach( c ->System.out.println(c));
  }

  private static void displayDatasetVars(CoverageCollection c, String fName){

    System.out.println("The following grids (variables) were found in the" + fName + " dataset:\n");
    for (Coverage var : c.getCoverages())
      System.out.println(var.getFullName());
  }

  public static void main(String ... args) {

    ArrayList<String> varsList;

    final String FILE_TYPE_ATTR = "file_format";
    final String GRIB1_GENERATING_PROCESS = "Generating_process_or_model";
    final String ORIGINATOR = "Originating_or_generating_Center";
    final String GRIB2_GENERATING_PROCESS = "Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre";

    String progName = NcWRFWriter.class.getName();
    ArrayList<Coverage> filteredCoverages = new ArrayList<>();
    String errors;

    try {

      NcWRFWriter.CommandLine cmdLine = new NcWRFWriter.CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
      } else {

        cmdLine.validateParms();
        String datasetIn = cmdLine.inputFile.getAbsolutePath();

        if (!datasetIn.isEmpty()) {

          Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(datasetIn);
          if (!opt.isPresent())
            throw new FileNotFoundException("Not a Grid Dataset " + datasetIn + " err=" + opt.getErrorMessage());

          CoverageCollection covColl = opt.get().getSingleCoverageCollection();

          if (cmdLine.show) {
            displayDatasetVars(covColl, datasetIn);
            return;
          }

          String vtble = cmdLine.vtableFile.getAbsolutePath();
          Vtable theTable = new Vtable(vtble, false);
          String mapSource;
          ArrayList<VtableEntry> table = theTable.getTable();
          ArrayList<Coverage> theCoverages;

          WRFWriterImpl writer = new WRFWriterImpl();

    //      ArrayList<Coverage> filteredCoverages = new ArrayList<>();
          theTable.displayVtable(table);

          Attribute fileVersion = covColl.findAttribute(FILE_TYPE_ATTR);

          if (fileVersion.getStringValue().equals(WRFProcessingUtils.GRIB_1_FILE_VER)) {
            String genProc = covColl.findAttribute(GRIB1_GENERATING_PROCESS).getStringValue();
            mapSource = genProc + "," + covColl.findAttribute(ORIGINATOR).getStringValue();

            theCoverages = WRFProcessingUtils.createCoverageListGrib1(covColl, table);

          } else {   // GRIB2
            String genProc = covColl.findAttribute(GRIB2_GENERATING_PROCESS).getStringValue();
            mapSource = genProc + "," + covColl.findAttribute(ORIGINATOR).getStringValue();

            theCoverages = WRFProcessingUtils.createCoverageListGrib2(covColl, table);
          }  // end else grib2

          if (cmdLine.varsetFile != null) {
            varsList = WRFProcessingUtils.readSingleVarsetXMLFile(cmdLine.varsetFile);
            filteredCoverages = WRFProcessingUtils.filterCoverageListWithVarset(theCoverages, varsList);
          } else if (cmdLine.variableFile != null) {
            varsList = WRFProcessingUtils.readVariablesFromfile(cmdLine.variableFile);
            filteredCoverages = WRFProcessingUtils.filterCoverageListWithVarset(theCoverages, varsList);
          } else {   // no variables or varset, create the WRF file based on the vtable
            filteredCoverages = theCoverages;
          }

          displayCoverages(filteredCoverages);
          errors = WRFProcessingUtils.validateRequiredElements(filteredCoverages);

          if (!errors.isEmpty()) {
            System.out.println(errors);
          } else {
      //      writer.writeFile(filteredCoverages, mapSource, false, cmdLine.outputFileName);
          }
        }  // if !dataset
      }
      } catch(Exception e){
        System.err.println(e.getMessage());
        System.err.printf("Try \"%s --help\" for more information.%n", progName);
      }
  }
}
