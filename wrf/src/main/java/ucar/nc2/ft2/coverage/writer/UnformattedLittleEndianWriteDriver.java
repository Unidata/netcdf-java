package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.Array;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UnformattedLittleEndianWriteDriver {
    public static void main(String[] args) throws IOException {

        //read a file to get slab array
        String readFile = "/Users/chedrick/Desktop/wrf_intermediate_format/intermediate_format/FILE:2018-08-16_06";
        DataInputStream dataStreamRead = new DataInputStream(
                new FileInputStream(readFile));
        UnformattedRead.readRecordLine1(dataStreamRead);
        UnformattedRead.readRecordLine2(dataStreamRead);
        UnformattedRead.readRecordLine3(dataStreamRead,0);
        UnformattedRead.readRecordLine4(dataStreamRead);
        float[][] slab = UnformattedRead.readRecordLine5(dataStreamRead);

//        float[][] slab = new float[2][4];
//        float val = (float) 2.0;
//        for(int i=0; i<2; i++){
//            for(int j=0; j<4; j++){
//
//                val++;
//                slab[i][j] = val;
//            }
//        }
        //System.out.println(Arrays.deepToString(slab));
        System.out.println(slab[1][2]);
        //write file
        String file = "/Users/chedrick/code/unformatWrite/fortran/test";
        OutputStream dataStream = new FileOutputStream(file);
        byte[] RecordLine1bytes = UnformattedWrite.writeRecordLine1();
        byte[] RecordLine2bytes = UnformattedWrite.writeRecordLine2();
        byte[] RecordLine3bytes = UnformattedWrite.writeRecordLine3();
        byte[] RecordLine4bytes = UnformattedWrite.writeRecordLine4();
        byte[] RecordLine5bytes = UnformattedWrite.writeRecordLine5(slab);
        dataStream.write(RecordLine1bytes);
        dataStream.write(RecordLine2bytes);
        dataStream.write(RecordLine3bytes);
        dataStream.write(RecordLine4bytes);
        dataStream.write(RecordLine5bytes);




        // write line 1


    }

}

