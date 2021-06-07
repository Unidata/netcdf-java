package examples.runtime;

public class runtimeLoadingTutorial {

    public static void register(String classNameAsString) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ucar.nc2.NetcdfFiles.registerIOProvider( classNameAsString);
    }

    public static void opentests(ucar.unidata.io.RandomAccessFile yourRandomAccessFile){
        ucar.nc2.iosp.IOServiceProvider.isValidFile( yourRandomAccessFile);
    }
}
