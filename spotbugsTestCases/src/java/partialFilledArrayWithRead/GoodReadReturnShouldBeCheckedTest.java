package partialFilledArrayWithRead;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GoodReadReturnShouldBeCheckedTest {
    public static String readBytes(InputStream in) throws IOException {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[1024];
        while ((bytesRead = in.read(data, offset, data.length - offset))
                != -1) {
            offset += bytesRead;
            if (offset >= data.length) {
                break;
            }
        }
        String str = new String(data, 0, offset, "UTF-8");
        return str;
    }

    public static String readFullyBytes(FileInputStream fis)
            throws IOException {
        byte[] data = new byte[1024];
        DataInputStream dis = new DataInputStream(fis);
        dis.readFully(data);
        String str = new String(data, "UTF-8");
        return str;
    }
}
