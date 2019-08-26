package ucar.nc2.ft2.coverage.writer;

import java.io.*;
import java.nio.ByteBuffer;

public class UnformattedLittleEndianWriteDriver {
    public static void main(String[] args) throws IOException {
        String file = "/Users/chedrick/code/unformatWrite/fortran/test";
        OutputStream dataStream = new FileOutputStream(file);
        byte[] bytes = UnformattedWrite.writeRecordLine1();
        dataStream.write(bytes);




        // write line 1


    }

}

