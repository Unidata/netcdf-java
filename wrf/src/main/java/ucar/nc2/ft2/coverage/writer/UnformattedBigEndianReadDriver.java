package ucar.nc2.ft2.coverage.writer;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class UnformattedBigEndianReadDriver {

    public static void main(String[] args) throws IOException {
        String file = "/Users/chedrick/Desktop/wrf_intermediate_format/intermediate_format/FILE:2018-08-16_06";
        DataInputStream dataStream = new DataInputStream(
                new FileInputStream(file));
        // read line 1
        UnformattedRead.readRecordLine1(dataStream);
        UnformattedRead.readRecordLine2(dataStream);
        UnformattedRead.readRecordLine3(dataStream,0);
        UnformattedRead.readRecordLine4(dataStream);
        UnformattedRead.readRecordLine5(dataStream);



    }
}
