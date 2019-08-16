package ucar.nc2.ft2.coverage.writer;
/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
    import java.io.File;
    import java.io.FileNotFoundException;
    import java.io.IOException;
    import java.util.List;
    import java.util.Comparator;
    import java.util.Arrays;
    import java.util.ArrayList;
    import java.util.Scanner;

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

class NcWRFWriter {

  static final String METGRID_NAME = "Metgrid_Name";
  static final String METGRID_UNIT = "Metgrid_Unit";
  static final String METGRID_DESC = "Metgrid_Desc";

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input dataset.", required = true)
    public File inputFile = null;

    @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
    public File outputFile = null;

    @Parameter(names = {"-v", "--vtable"}, description = "Generate the output from variables found in this vtable.")
    public File vtableFile = null;

//      @Parameter(names = {"-v", "--variables"}, description = "Generate the output from this list of variables from the " +
//              "input dataset.", variableArity = true)
//      public List<String> vars = new ArrayList<>();

//      @Parameter(names = {"-f", "--vfile"}, description = "Generate the output from an xml file containing " +
//              "a list of variables (a varset).")
//      public File varsetFile = null;

    @Parameter(names = {"-s", "--showvars"}, description = "Display a list of variables from the input dataset.")
    public boolean show = false;

    @Parameter(names = {"-h", "--help"}, description = "You must provide an input dataset name, an output file name, " +
        "and ONE option: -v (--variables), -f (--vfile), or -s(--showvars).", help = true)
    public boolean help = false;

    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {

      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList(
          "--input", "--output", "--variables", "--vfile", "--showvars", "--help");

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

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new NcWRFWriter.CommandLine.ParameterDescriptionComparator());
    }


    public void printUsage() {
      jc.usage();
    }

    public void validateParms() {
      if (!inputFile.exists())
        throw new ParameterException("The input file: " + inputFile + " was not found.");

      //     if(!vtableFile.exists()){
      //         throw new ParameterException("The vTable file: " + vtableFile + " was not found.");
      //     }
      // optional arguments, only one is allowed, we don't care which one at this point.
      //     int a = vars.isEmpty() ? 0 : 1;
      //     int b = varsetFile == null ? 0 : 1;
      //     int c = show ? 1 : 0;

      //     int res = a+b+c;
      //     if( res != 1)
      //         throw new ParameterException("Incorrect number of optional arguments, use one and only one.");
    }
  }

  // returns a list of vars not found in the input dataset or provided on the command line
  // the requestedVars are subset of the Coverage collection
  private static List<String> validateRequestedVars(CoverageCollection cc, List<String>requestedVars) {
    List<String> notFound = new ArrayList<>();
    for( String s : requestedVars ) {
      System.out.println("requestedVar is: " + s);
      Coverage grid = cc.findCoverage(s);

      if (grid == null) {
        System.out.println("grid is null");
        notFound.add(s);
      }
    }
    return notFound;
  }

  private static void displayVtable(ArrayList<VtableEntry> entries){

    System.out.println("\nThe vtable is:");
    entries.forEach( e -> System.out.println( e ) );
  }

  private static void displayCoverages(ArrayList<Coverage> cov) {
    System.out.println("\nCoverages found:");
    cov.forEach( e ->System.out.println(e));
  }

  private static void displayDatasetVars(CoverageCollection c, String fName){

    System.out.println("\nThe following grids (variables) were found in the" + fName + " dataset:\n");
    for (Coverage var : c.getCoverages())
      System.out.println(var.getName());
  }

  private static ArrayList<VtableEntry> readVtable(String fname) {

    ArrayList<VtableEntry> entries = new ArrayList<>();

    String line;
    String begin;
    final String regex = "[^a-zA-Z]";

    try (Scanner scanner = new Scanner(new File(fname))) {

      while (scanner.hasNext()) {
        line = scanner.nextLine();
        begin = line.substring(0, 1);
        if (begin.matches(regex)) {
          if ((!line.startsWith("-") && (!line.startsWith("#")))) {
            VtableEntry vt = new VtableEntry(line);
            entries.add(vt);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return entries;
  }

  private static boolean hasAllRequired( CoverageCollection cv ){

    boolean requirementsMet = false;


    /**
    Temperature
    U	and	V	components	of	wind
    Geopotential Height
    Relative	Humidity/Specific	Humidity
• 2D	Data
    Surface	pressure
    Mean	sea-level	pressure
    Skin	temperature/SST
    2	meter	temperature	and	relative	humidity
    10	meter	U	and	V	components	of	wind
    Soil	data	(temperature	and	moisture)	and	soil	height
*/
    return requirementsMet;
  }

  private static ArrayList<Coverage> createCoverageListGrib1(CoverageCollection cv, ArrayList<VtableEntry> vt){

    final String GRIB1_PARAMETER = "Grib1_Parameter";
    final String GRIB1_LEVEL_TYPE = "Grib1_Level_Type";

    ArrayList<Coverage> theCoverages = new ArrayList<>();

    for (VtableEntry v : vt) {

      int i = Integer.parseInt(v.getParam());
      int j = Integer.parseInt(v.getLevelType());

      for(Coverage c: cv.getCoverages()) {

        Attribute ab = c.findAttributeIgnoreCase(GRIB1_PARAMETER);
        if (ab != null) {
          Number num = ab.getNumericValue();
          int n = num.intValue();
          if (n == i) {
            Attribute level = c.findAttributeIgnoreCase(GRIB1_LEVEL_TYPE);
            if (level != null) {
              Number nLevel = level.getNumericValue();
              int iLevel = nLevel.intValue();
              if (iLevel == j) {
                c.getAttributes().add(new Attribute(METGRID_NAME, v.getMetgridName()));
                c.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
                c.getAttributes().add(new Attribute(METGRID_DESC, v.getMetgridDesc()));
                theCoverages.add(c);
              }
            }
          }
        }
      }
    }
    return theCoverages;
  }

  private static ArrayList<Coverage> createCoverageListGrib2(CoverageCollection cv, ArrayList<VtableEntry> vt) {
    final String GRIB_VAR_ID =  "Grib_Variable_Id";

    ArrayList<Coverage> theCoverages = new ArrayList<>();

    for (VtableEntry v : vt) {

      String varId = v.getGRIB2Var();
      Coverage cov = cv.findCoverageByAttribute(GRIB_VAR_ID, varId);
      if(cov !=null ) {
        cov.getAttributes().add(new Attribute(METGRID_NAME, v.getMetgridName()));
        cov.getAttributes().add(new Attribute(METGRID_UNIT, v.getMetgridUnits()));
        cov.getAttributes().add(new Attribute(METGRID_DESC,v.getMetgridDesc()));
      }
      theCoverages.add(cov);
    }
    return theCoverages;
  }

  public static void main(String[] args) throws Exception {

    final String GRIB_1_FILE_VER = "GRIB-1";
    final String GRIB_2_FILE_VER = "GRIB-2";
    final String FILE_TYPE_ATTR = "file_format";
    final String GRIB1_GENERATING_PROCESS = "Generating_process_or_model";
    final String ORIGINATOR = "Originating_or_generating_Center";
    final String GRIB2_GENERATING_PROCESS = "Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre";

    String mapSource;

    String progName = NcWRFWriter.class.getName();

    try {
      NcWRFWriter.CommandLine cmdLine = new NcWRFWriter.CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      cmdLine.validateParms();

      String datasetIn = cmdLine.inputFile.getAbsolutePath();

      if (!datasetIn.isEmpty()) {
        Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(datasetIn);
        if (!opt.isPresent())
          throw new FileNotFoundException("Not a Grid Dataset " + datasetIn + " err=" + opt.getErrorMessage());

        CoverageCollection covColl = opt.get().getSingleCoverageCollection();
        if (cmdLine.show) {
          displayDatasetVars(covColl,datasetIn);
        }
        if( cmdLine.vtableFile != null ){
          String vtble = cmdLine.vtableFile.getAbsolutePath();
          ArrayList<VtableEntry> table = readVtable(vtble);
          displayVtable(table);
          // validate the required vtable items against the coverages in the collection
          //
          List<Attribute> atts = covColl.getGlobalAttributes();
          Attribute fileVersion = covColl.findAttribute(FILE_TYPE_ATTR);

          if( fileVersion.getStringValue().equals(GRIB_1_FILE_VER)){

            String genProc = covColl.findAttribute(GRIB1_GENERATING_PROCESS).getStringValue();
            mapSource = genProc + "," + covColl.findAttribute(ORIGINATOR).getStringValue();
            // do the coverages
            ArrayList<Coverage> theCoverages = createCoverageListGrib1(covColl, table);
            displayCoverages(theCoverages);
          }
          else{
            String genProc = covColl.findAttribute(GRIB2_GENERATING_PROCESS).getStringValue();
            mapSource = genProc + "," + covColl.findAttribute(ORIGINATOR).getStringValue();
            ArrayList<Coverage> theCoverages = createCoverageListGrib2(covColl, table);
            displayCoverages(theCoverages);
          }
          // WRFWriter.write(theCoverages, String mapSource, boolean isWindEarthRel, output file);
        }
      }  // if !datasetIn
    } catch (ParameterException | IOException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
