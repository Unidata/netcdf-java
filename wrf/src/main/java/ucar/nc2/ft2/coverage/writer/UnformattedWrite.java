package ucar.nc2.ft2.coverage.writer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;

public class UnformattedWrite {
    static final int FLOAT_AND_INT_SIZE = 4;
    static final int HDATE_LENGTH = 24;
    static final int MAP_SOURCE_LENGTH = 32;
    static final int FIELD_LENGTH = 9;
    static final int UNITS_LENGTH = 25;
    static final int DESC_LENGTH = 46;
    static final int STARTLOC_LENGTH = 8;

    public static byte[] writeRecordLine1() throws IOException {
        int headerTailer = 4;
        int IFV = 5;
        byte[] outputBuffer = new byte[12];
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(IFV);
        byteBuffer.putInt(headerTailer);
        return byteBuffer.array();




//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        DataOutputStream dataStream = new DataOutputStream(outputStream);
//        dataStream.writeInt(headerTailer);
//        dataStream.writeInt(IFV);
//        dataStream.writeInt(headerTailer);
//        dataStream.flush();
//        return outputStream.toByteArray();
    }
    public static byte[] writeRecordLine2() throws IOException {
        int headerTailer = 156;
        byte[] outputBuffer = new byte[156 + 8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
        char[] hdate = "2018-08-16_06:00:00".toCharArray();
        float xfcst = (float) 0.0;
        char [] mapSource = "NCEP GFS Analysis".toCharArray();
        char [] field = "PMSL".toCharArray();
        char [] units = "Pa".toCharArray();
        char [] description = "Sea-level Pressure".toCharArray();
        float xLevel = (float) 201300.0;
        int nx = 1440;
        int ny = 721;
        int projection = 0;
        //write header
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        //write hdate
        for (int i=0; i< HDATE_LENGTH; i++) {
            if ( i < hdate.length) {
                byte b = (byte) hdate[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }
        }
        //write xfcst
        byte[] bytes = new byte[4];

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(xfcst);
        //write map source
        for(int i = 0; i < MAP_SOURCE_LENGTH; i++){
            if(i< mapSource.length) {
                byte b = (byte) mapSource[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }

        }
        //write field

        for(int i = 0; i < FIELD_LENGTH; i++){
            if(i< field.length) {
                byte b = (byte) field[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }

        }
        // write units
        for(int i = 0; i < UNITS_LENGTH; i++){
            if(i< units.length) {
                byte b = (byte) units[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }

        }
        // write desc
        for(int i = 0; i < DESC_LENGTH; i++){
            if(i< description.length) {
                byte b = (byte) description[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }

        }
        //write xLevel
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(xLevel);
        // write nx
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(nx);
        // write ny
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(ny);
        // write projection
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(projection);
        //write tailer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        return byteBuffer.array();



    }
    public static byte[] writeRecordLine3() throws IOException{
        int headerTailer = 28;
        byte[] outputBuffer = new byte[28 + 8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
        char[] startLocation = "SWCORNER".toCharArray();
        float startLatitude = (float) 90.0;
        float startLongitude = (float) 0.0;
        float deltaLatitude = (float) -0.25;
        float deltaLongitude = (float) 0.25;
        float earthRadius = (float) 6371.2295;


        //write header
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        //write start loc
        for(int i = 0; i < STARTLOC_LENGTH; i++){
            if(i< startLocation.length) {
                byte b = (byte) startLocation[i];
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }else {
                byte b = (byte) ' ';
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).put(b);
            }

        }

        //write start lat
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(startLatitude);
        //write start lon
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(startLongitude);
        //write delta lat
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(deltaLatitude);
        //write delta lon
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(deltaLongitude);
        //write earth radius
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(earthRadius);
        //write tailer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        return byteBuffer.array();



    }
    public static byte[] writeRecordLine4() throws IOException {
        int headerTailer = 4;
        byte[] outputBuffer = new byte[headerTailer + 8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
        //write header
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        //write isWindEarthRel
        int isWindEarthRelative = 1;
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(isWindEarthRelative);
        //write tailer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        return byteBuffer.array();

    }
    public static byte[] writeRecordLine5(float[][] slab) throws IOException {
        int headerTailer = 1440 * 721 * 4;
        byte[] outputBuffer = new byte[headerTailer + 8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
        //write header
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        //write data slab
        for (int i = 0; i < 721; i++) {
            for (int j = 0; j < 1440; j++) {
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(slab[i][j]);
            }
        }
        //write tailer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
        return byteBuffer.array();
    }
//public static byte[] writeRecordLine5(float[][] slab) throws IOException {
//    int headerTailer = 4 * 2 * 4;
//    byte[] outputBuffer = new byte[headerTailer + 8];
//    ByteBuffer byteBuffer = ByteBuffer.wrap(outputBuffer);
//    //write header
//    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
//    //write slab
//    for(int i=0; i<2; i++) {
//        for (int j = 0; j < 4; j++) {
//            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(slab[i][j]);
//
//        }
//    }
//    //write tailer
//    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(headerTailer);
//    return byteBuffer.array();
//
//}

}
