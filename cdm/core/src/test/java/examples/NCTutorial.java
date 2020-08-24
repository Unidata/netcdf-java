package examples;

import ucar.nc2.NetcdfFile;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCTutorial {
    // TODO: make test logger that returns string
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static NetcdfFile ncfile;

    // NOTE: these functions are used in the NetCdf tutorial docs, so formatting matters!
    public static void openNCFileTutorial() {
        String filename = "C:/data/my/file.nc";
        try {
            ncfile = NetcdfFile.open(filename);
        } catch (IOException ioe) {
            logger.info("exception trying to open " + filename, ioe);
        }
    }
}
