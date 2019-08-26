package ucar.nc2.ft2.coverage.writer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;

public class UnformattedWrite {

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

}
