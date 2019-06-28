/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test;

import dap4.core.data.DSPRegistry;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.FileDSP;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;
import ucar.nc2.NetcdfFile;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.jni.netcdf.Nc4prototypes;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.List;

abstract public class DapTestCommon extends UnitTestCommon
{

    //////////////////////////////////////////////////
    // Constants

    static final String DEFAULTTREEROOT = "dap4";

    static public final String FILESERVER = "file://localhost:8080";

    static public final String CONSTRAINTTAG = "dap4.ce";
    static public final String ORDERTAG = "ucar.littleendian";
    static public final String NOCSUMTAG = "ucar.nochecksum";
    static public final String TRANSLATETAG = "ucar.translate";
    static public final String TESTTAG = "ucar.testing";

    static final String D4TESTDIRNAME = "d4tests";

    // Equivalent to the path to the webapp/d4ts for testing purposes
    static protected final String DFALTRESOURCEPATH = "/src/test/data/resources";
    static protected Class NC4IOSP = ucar.nc2.jni.netcdf.Nc4Iosp.class;

    static class TestFilter implements FileFilter
    {
        boolean debug;
        boolean strip;
        String[] extensions;

        public TestFilter(boolean debug, String[] extensions)
        {
            this.debug = debug;
            this.strip = strip;
            this.extensions = extensions;
        }

        public boolean accept(File file)
        {
            boolean ok = false;
            if(file.isFile() && file.canRead()) {
                // Check for proper extension
                String name = file.getName();
                if(name != null) {
                    for(String ext : extensions) {
                        if(name.endsWith(ext))
                            ok = true;
                    }
                }
                if(!ok && debug)
                    System.err.println("Ignoring: " + file.toString());
            }
            return ok;
        }

        static void
        filterfiles(String path, List<String> matches, String... extensions)
        {
            File testdirf = new File(path);
            assert (testdirf.canRead());
            TestFilter tf = new TestFilter(DEBUG, extensions);
            File[] filelist = testdirf.listFiles(tf);
            for(int i = 0; i < filelist.length; i++) {
                File file = filelist[i];
                if(file.isDirectory()) continue;
                String fname = DapUtil.canonicalpath(file.getAbsolutePath());
                matches.add(fname);
            }
        }
    }

    //////////////////////////////////////////////////
    // Static variables

    static protected String dap4root = null;
    static protected String dap4testroot = null;
    static protected String dap4resourcedir = null;

    static {
        dap4root = locateDAP4Root(threddsroot);
        if(dap4root == null)
            System.err.println("Cannot locate /dap4 parent dir");
        dap4testroot = canonjoin(dap4root, D4TESTDIRNAME);
        dap4resourcedir = canonjoin(dap4testroot, DFALTRESOURCEPATH);
    }

    //////////////////////////////////////////////////
    // Static methods


    static protected String getD4TestsRoot()
    {
        return dap4testroot;
    }

    static protected String getResourceRoot()
    {
        return dap4resourcedir;
    }

    static String
    locateDAP4Root(String threddsroot)
    {
        String root = threddsroot;
        if(root != null)
            root = root + "/" + DEFAULTTREEROOT;
        // See if it exists
        File f = new File(root);
        if(!f.exists() || !f.isDirectory())
            root = null;
        return root;
    }

    //////////////////////////////////////////////////
    // Instance variables


    protected String d4tsserver = null;

    protected String title = "Dap4 Testing";

    public DapTestCommon()
    {
        this("DapTest");
    }

    public DapTestCommon(String name)
    {
        super(name);

        this.d4tsserver = TestDir.dap4TestServer;
        if(DEBUG)
            System.err.println("DapTestCommon: d4tsServer=" + d4tsserver);
    }

    /**
     * Try to get the system properties
     */
    protected void setSystemProperties()
    {
        String testargs = System.getProperty("testargs");
        if(testargs != null && testargs.length() > 0) {
            String[] pairs = testargs.split("[  ]*[,][  ]*");
            for(String pair : pairs) {
                String[] tuple = pair.split("[  ]*[=][  ]*");
                String value = (tuple.length == 1 ? "" : tuple[1]);
                if(tuple[0].length() > 0)
                    System.setProperty(tuple[0], value);
            }
        }
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        if(System.getProperty("baseline") != null)
            prop_baseline = true;
        if(System.getProperty("nogenerate") != null)
            prop_generate = false;
        if(System.getProperty("debug") != null)
            prop_debug = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        if(System.getProperty("ascii") != null)
            prop_ascii = true;
        if(System.getProperty("utf8") != null)
            prop_ascii = false;
        if(prop_baseline && prop_diff)
            prop_diff = false;
        prop_controls = System.getProperty("controls", "");
    }

    //////////////////////////////////////////////////
    // Overrideable methods

    //////////////////////////////////////////////////
    // Accessor

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    //////////////////////////////////////////////////
    // Instance Utilities

    public void
    visual(String header, String captured)
    {
        if(!captured.endsWith("\n"))
            captured = captured + "\n";
        // Dump the output for visual comparison
        System.err.println("\n"+header + ":");
        System.err.println("---------------");
        System.err.print(captured);
        System.err.println("---------------");
        System.err.flush();
    }

    protected void
    findServer(String path)
            throws DapException
    {
        String svc = "http://" + this.d4tsserver + "/d4ts";
        if(!checkServer(svc))
            System.err.println("D4TS Server not reachable: " + svc);
        // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
        d4tsserver = "dap4://" + d4tsserver + "/d4ts";
    }

    //////////////////////////////////////////////////

    public String getDAP4Root()
    {
        return this.dap4root;
    }

    @Override
    public String getResourceDir()
    {
        return this.dap4resourcedir;
    }

    static protected void
    testSetup()
    {
        try {
            // Always prefer Nc4Iosp over HDF5
            NetcdfFile.iospDeRegister(NC4IOSP);
            NetcdfFile.registerIOProviderPreferred(NC4IOSP,
                    ucar.nc2.iosp.hdf5.H5iosp.class
            );
            // Print out the library version
            System.err.printf("Netcdf-c library version: %s%n", getCLibraryVersion());
            System.err.flush();
        } catch (Exception e) {
            System.err.println("Cannot load ucar.nc2.jni.netcdf.Nc4Iosp");
        }
    }

    static void
    printDir(String path)
    {
        File testdirf = new File(path);
        assert (testdirf.canRead());
        File[] filelist = testdirf.listFiles();
        System.err.println("\n*******************");
        System.err.printf("Contents of %s:%n", path);
        for(int i = 0; i < filelist.length; i++) {
            File file = filelist[i];
            String fname = file.getName();
            System.err.printf("\t%s%s%n",
                    fname,
                    (file.isDirectory() ? "/" : ""));
        }
        System.err.println("*******************");
        System.err.flush();
    }

    static public String
    getCLibraryVersion()
    {
        Nc4prototypes nc4 = getCLibrary();
        return (nc4 == null ? "Unknown" : nc4.nc_inq_libvers());
    }

    static public Nc4prototypes
    getCLibrary()
    {
        try {
            Method getclib = NC4IOSP.getMethod("getCLibrary");
            return (Nc4prototypes) getclib.invoke(null);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            return null;
        }
    }
}

