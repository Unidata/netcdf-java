package ucar.nc2.ft2.coverage.writer;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnformattedRead {
    static final int FLOAT_AND_INT_SIZE = 4;
    static final int HDATE_LENGTH = 24;
    static final int MAP_SOURCE_LENGTH = 32;
    static final int FIELD_LENGTH = 9;
    static final int UNITS_LENGTH = 25;
    static final int DESC_LENGTH = 46;
    static final int STARTLOC_LENGTH = 8;

    public static void readRecordLine1(DataInputStream dataStream ) throws IOException {
        byte[] inputBuffer = new byte[4];
        dataStream.read(inputBuffer);
        int header = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        dataStream.read(inputBuffer);
        int intValue = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        dataStream.read(inputBuffer);
        int trailer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        System.out.println("header =" + header);
        System.out.println("int = " + intValue);
        System.out.println("trailer = " + trailer);

    }

    public static void readRecordLine2(DataInputStream dataStream) throws IOException {

        // read record header
        byte[] inputBuffer = new byte[FLOAT_AND_INT_SIZE];
        dataStream.read(inputBuffer);
        int header = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read hdate
        char[] hdate = new char[HDATE_LENGTH];
        for (int i = 0; i < HDATE_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            hdate[i] = recordChar;
        }
        // read xfcst
        dataStream.read(inputBuffer);
        float xfcst = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        // read map_source
        char[] mapSource = new char[MAP_SOURCE_LENGTH];
        for (int i = 0; i < MAP_SOURCE_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            mapSource[i] = recordChar;
        }
        // read field
        char[] field = new char[FIELD_LENGTH];
        for (int i = 0; i < FIELD_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            field[i] = recordChar;
        }
        // read units
        char[] units = new char[UNITS_LENGTH];
        for (int i = 0; i < UNITS_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            units[i] = recordChar;
        }
        // read desc
        char[] desc = new char[DESC_LENGTH];
        for (int i = 0; i < DESC_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            desc[i] = recordChar;
        }
        // read xlvl
        dataStream.read(inputBuffer);
        float xlvl = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        // read nx
        dataStream.read(inputBuffer);
        int nx = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read ny
        dataStream.read(inputBuffer);
        int ny = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read iproj
        dataStream.read(inputBuffer);
        int iproj = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read tailer
        dataStream.read(inputBuffer);
        int trailer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();


        System.out.println("header =" + header);
        System.out.println(hdate);
        System.out.println(xfcst);
        System.out.println(mapSource);
        System.out.println(field);
        System.out.println(units);
        System.out.println(desc);
        System.out.println(xlvl);
        System.out.println(nx);
        System.out.println(ny);
        System.out.println(iproj);
        System.out.println("trailer =" + trailer);

    }

    public static void readRecordLine3(DataInputStream dataStream, int projection) throws IOException{
        // read record header
        byte[] inputBuffer = new byte[FLOAT_AND_INT_SIZE];
        dataStream.read(inputBuffer);
        int header = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read startloc
        char[] startloc = new char[STARTLOC_LENGTH];
        for (int i = 0; i < STARTLOC_LENGTH; i++){
            byte charByte = dataStream.readByte();
            char recordChar = (char) charByte;
            startloc[i] = recordChar;
        }
        //read startlat
        dataStream.read(inputBuffer);
        float startlat = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        //read startlon
        dataStream.read(inputBuffer);
        float startlon = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        float deltaLat = 0;
        float deltaLon = 0;
        if(projection == 0){
            // Cylindrical Equidistant (lat/lon) projection

            // read delta lat
            dataStream.read(inputBuffer);
            deltaLat = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
            //read delta lon
            dataStream.read(inputBuffer);
            deltaLon = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();

        }else if (projection == 1){
            // Mercator projection

        }else if (projection == 3){
            // Lambert Conformal projection

        }else if (projection == 4){
            // Gaussian projection

        }else if (projection == 5){
            // Polar Stereographic projection

        }
        // read earth radius
        dataStream.read(inputBuffer);
        float earthRadius = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        // read trailer
        dataStream.read(inputBuffer);
        int trailer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();

        System.out.println("header = " + header);
        System.out.println(startloc);
        System.out.println(startlat);
        System.out.println(startlon);
        System.out.println(deltaLat);
        System.out.println(deltaLon);
        System.out.println(earthRadius);
        System.out.println("trailer =" + trailer);




    }

    public static void readRecordLine4(DataInputStream dataStream) throws IOException {
        // read record header
        byte[] inputBuffer = new byte[FLOAT_AND_INT_SIZE];
        dataStream.read(inputBuffer);
        int header = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read isWindEarthRel
        dataStream.read(inputBuffer);
        int isWindEarthRel = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // read trailer
        dataStream.read(inputBuffer);
        int trailer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();

        System.out.println("header = " + header);
        System.out.println(isWindEarthRel);
        System.out.println("trailer =" + trailer);

    }

    public static void readRecordLine5(DataInputStream dataStream) throws IOException {
        // read record header
        byte[] inputBuffer = new byte[FLOAT_AND_INT_SIZE];
        dataStream.read(inputBuffer);
        int header = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
        // get data from slab array
        int numOfData = (header - 8)/4;
        float[] data = new float[numOfData];
        for (int i = 0; i < numOfData; i++){
            dataStream.read(inputBuffer);
            data[i] = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getFloat();
        }
        // read trailer
        dataStream.read(inputBuffer);
        int trailer = ByteBuffer.wrap(inputBuffer).order(ByteOrder.BIG_ENDIAN).getInt();

        System.out.println("header = " + header);
       // System.out.println("data= "+ Arrays.toString(data));
        System.out.println("trailer =" + trailer);

    }








}
