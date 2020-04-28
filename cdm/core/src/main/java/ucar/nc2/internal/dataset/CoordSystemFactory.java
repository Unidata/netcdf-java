package ucar.nc2.internal.dataset;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.spi.CoordSystemBuilderFactory;
import ucar.nc2.internal.dataset.conv.CF1Convention;
import ucar.nc2.internal.dataset.conv.DefaultConventions;
import ucar.nc2.internal.ncml.NcMLReaderNew;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.StringUtil2;

/** Static methods for managing CoordSystemBuilderFactory classes */
public class CoordSystemFactory {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSystemFactory.class);

  // Place where resources can be placed, eg NcML files used to wrap datasets.
  public static final String resourcesDir = "resources/nj22/coords/";

  private static List<Convention> conventionList = new ArrayList<>();
  private static Map<String, String> ncmlHash = new HashMap<>();
  private static boolean useMaximalCoordSys = true;

  /**
   * Allow plug-ins to determine if it owns a file based on the file's Convention attribute.
   */
  public interface ConventionNameOk {

    /**
     * Do you own this file?
     *
     * @param convName name in the file
     * @param wantName name passed into registry
     * @return true if you own this file
     */
    boolean isMatch(String convName, String wantName);
  }

  // These get precedence
  static {
    registerConvention(_Coordinate.Convention, new CoordSystemBuilder.Factory());
    registerConvention("CF-1.", new CF1Convention.Factory(), String::startsWith);
    registerConvention("CDM-Extended-CF", new CF1Convention.Factory());
    // this is to test DefaultConventions, not needed when we remove old convention builders.
    registerConvention("MARS", new DefaultConventions.Factory());
  }

  /**
   * Register an NcML file that implements a Convention by wrapping the dataset in the NcML.
   * It is then processed by CoordSystemBuilder, using the _Coordinate attributes.
   *
   * @param conventionName name of Convention, must be in the "Conventions" global attribute.
   * @param ncmlLocation location of NcML file, may be local file or URL.
   * @see ucar.nc2.ncml.NcMLReader#wrapNcML
   */
  public static void registerNcML(String conventionName, String ncmlLocation) {
    ncmlHash.put(conventionName, ncmlLocation);
  }

  /**
   * Register a class that implements a Convention. Will match (ignoring case) the COnvention name.
   *
   * @param conventionName name of Convention.
   *        This name will be used to look in the "Conventions" global attribute.
   * @param c implementation of CoordSystemBuilderFactory that parses those kinds of netcdf files.
   */
  public static void registerConvention(String conventionName, CoordSystemBuilderFactory c) {
    registerConvention(conventionName, c, null);
  }

  /**
   * Register a class that implements a Convention.
   *
   * @param conventionName name of Convention.
   *        This name will be used to look in the "Conventions" global attribute.
   *        Otherwise, you must implement the isMine() static method.
   * @param match pass in your own matcher. if null, equalsIgnoreCase() will be used.
   * @param factory implementation of CoordSystemBuilderFactory that parses those kinds of netcdf files.
   */
  public static void registerConvention(String conventionName, CoordSystemBuilderFactory factory,
      ConventionNameOk match) {
    conventionList.add(new Convention(conventionName, factory, match));
  }

  @Nullable
  private static CoordSystemBuilderFactory matchConvention(String convName) {
    for (Convention c : conventionList) {
      if ((c.match == null) && c.convName.equalsIgnoreCase(convName))
        return c.factory;
      if ((c.match != null) && c.match.isMatch(convName, c.convName))
        return c.factory;
    }
    return null;
  }

  /**
   * If true, assign implicit CoordinateSystem objects to variables that dont have one yet.
   * Default value is false.
   *
   * @param b true if if you want to guess at Coordinate Systems
   */
  public static void setUseMaximalCoordSys(boolean b) {
    useMaximalCoordSys = b;
  }

  /**
   * Get whether to make records into Structures.
   *
   * @return whether to make records into Structures.
   */
  public static boolean getUseMaximalCoordSys() {
    return useMaximalCoordSys;
  }

  /**
   * Breakup list of Convention names in the Convention attribute in CF compliant way.
   *
   * @param convAttValue original value of Convention attribute
   * @return list of Convention names
   */
  public static List<String> breakupConventionNames(String convAttValue) {
    ArrayList<String> names = new ArrayList<>();

    if ((convAttValue.indexOf(',') > 0) || (convAttValue.indexOf(';') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convAttValue, ",;");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    } else if ((convAttValue.indexOf('/') > 0)) {
      StringTokenizer stoke = new StringTokenizer(convAttValue, "/");
      while (stoke.hasMoreTokens()) {
        String name = stoke.nextToken();
        names.add(name.trim());
      }
    } else {
      return ImmutableList.of(convAttValue);
    }
    return names;
  }

  /**
   * Build a list of Conventions
   *
   * @param mainConv this is the main convention
   * @param convAtts list of others, only use "extra" Conventions
   * @return comma separated list of Conventions
   */
  public static String buildConventionAttribute(String mainConv, String... convAtts) {
    List<String> result = new ArrayList<>();
    result.add(mainConv);
    for (String convs : convAtts) {
      if (convs == null)
        continue;
      List<String> ss = breakupConventionNames(convs); // may be a list
      for (String s : ss) {
        if (matchConvention(s) == null) // only add extra ones, not ones that compete with mainConv
          result.add(s);
      }
    }

    // now form comma separated result
    boolean start = true;
    Formatter f = new Formatter();
    for (String s : result) {
      if (start)
        f.format("%s", s);
      else
        f.format(", %s", s);
      start = false;
    }
    return f.toString();
  }

  /**
   * Get a CoordSystemBuilder whose job it is to add Coordinate information to a NetcdfDataset.Builder.
   *
   * @param ds the NetcdfDataset.Builder to modify
   * @param cancelTask allow user to bail out.
   * @return the builder to be used
   * @throws java.io.IOException on io error
   */
  @Nonnull
  public static Optional<CoordSystemBuilder> factory(NetcdfDataset.Builder ds, CancelTask cancelTask)
      throws IOException {

    // look for the Conventions attribute
    Group.Builder root = ds.rootGroup;
    String convName = root.getAttributeContainer().findAttValueIgnoreCase(CDM.CONVENTIONS, null);
    if (convName == null) {
      // common mistake Convention instead of Conventions
      convName = root.getAttributeContainer().findAttValueIgnoreCase("Convention", null);
    }
    if (convName != null) {
      convName = convName.trim();
    }

    // look for ncml first
    if (convName != null) {
      String convNcML = ncmlHash.get(convName);
      if (convNcML != null) {
        CoordSystemBuilder csb = new CoordSystemBuilder(ds);
        NcMLReaderNew.wrapNcML(ds, convNcML, cancelTask);
        return Optional.of(csb);
      }
    }
    CoordSystemBuilderFactory coordSysFactory = null;

    // Try to match on convention name. Must be first in case NcML has set Convention name.
    if (convName != null) {
      coordSysFactory = findConventionByName(convName);
    }

    // Try to match on isMine() TODO: why use orgFile instead of ds?
    if (coordSysFactory == null && ds.orgFile != null) {
      coordSysFactory = findConventionByIsMine(ds.orgFile);
    }

    boolean isDefault = false;
    // if no convention class found, use the default
    if (coordSysFactory == null) {
      coordSysFactory = new DefaultConventions.Factory();
      isDefault = true;
    }

    // Now process it.
    CoordSystemBuilder coordSystemBuilder = coordSysFactory.open(ds);
    if (convName == null)
      coordSystemBuilder.addUserAdvice("No 'Conventions' global attribute.");
    else if (isDefault)
      coordSystemBuilder.addUserAdvice("No CoordSystemBuilder is defined for Conventions= '" + convName + "'\n");
    else {
      coordSystemBuilder.setConventionUsed(coordSysFactory.getConventionName());
    }

    ds.rootGroup.addAttribute(new Attribute(_Coordinate._CoordSysBuilder, coordSystemBuilder.getClass().getName()));
    return Optional.of(coordSystemBuilder);
  }

  private static CoordSystemBuilderFactory findConventionByIsMine(NetcdfFile orgFile) {
    // Look for Convention using isMine()
    for (Convention conv : conventionList) {
      CoordSystemBuilderFactory candidate = conv.factory;
      if (candidate.isMine(orgFile)) {
        return candidate;
      }
    }

    // Use service loader mechanism isMine()
    for (CoordSystemBuilderFactory csb : ServiceLoader.load(CoordSystemBuilderFactory.class)) {
      if (csb.isMine(orgFile)) {
        return csb;
      }
    }

    return null;
  }

  private static CoordSystemBuilderFactory findConventionByName(String convName) {
    // Try to match on convention name as is
    CoordSystemBuilderFactory coordSysFactory = findRegisteredConventionByName(convName);
    if (coordSysFactory != null)
      return coordSysFactory;

    coordSysFactory = findLoadedConventionByName(convName);
    if (coordSysFactory != null)
      return coordSysFactory;

    // Try splitting up the Convention string.
    List<String> names = breakupConventionNames(convName);
    for (String name : names) {
      coordSysFactory = findRegisteredConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }
    for (String name : names) {
      coordSysFactory = findLoadedConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }

    // last ditch desperate - split on white space
    Iterable<String> tokens = StringUtil2.split(convName);
    for (String name : tokens) {
      coordSysFactory = findRegisteredConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }
    for (String name : tokens) {
      coordSysFactory = findLoadedConventionByName(name);
      if (coordSysFactory != null)
        return coordSysFactory;
    }

    return null;
  }

  private static CoordSystemBuilderFactory findRegisteredConventionByName(String convName) {
    // look for registered conventions using convention name
    return matchConvention(convName);
  }

  private static CoordSystemBuilderFactory findLoadedConventionByName(String convName) {
    // use service loader mechanism
    for (CoordSystemBuilderFactory csb : ServiceLoader.load(CoordSystemBuilderFactory.class)) {
      if (convName.equals(csb.getConventionName())) {
        return csb;
      }
    }
    return null;
  }

  private static class Convention {
    String convName;
    CoordSystemBuilderFactory factory;
    ConventionNameOk match;

    Convention(String convName, CoordSystemBuilderFactory factory, ConventionNameOk match) {
      this.convName = convName;
      this.factory = factory;
      this.match = match;
    }
  }

}
