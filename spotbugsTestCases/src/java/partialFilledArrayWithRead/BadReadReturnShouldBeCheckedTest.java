package partialFilledArrayWithRead;


import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BadReadReturnShouldBeCheckedTest {

    String readBytes(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (in.read(data) == -1) {
            throw new EOFException();
        }
        return new String(data, "UTF-8");
    }

    public static String readBytesWithOffset(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        int offset = 0;
        if (in.read(data, offset, data.length - offset) != -1) {
            throw new EOFException();
        }
        return new String(data, "UTF-8");
    }

    public String readFromBufferedReader(BufferedReader bufferedReader) throws IOException {
        char[] data = new char[1024];

        if (bufferedReader.read(data) == -1) {
            throw new EOFException();
        }
        return new String(data);
    }

    String readBytesComparedToFloat(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (in.read(data) == -1F) {
            throw new EOFException();
        }
        return new String(data, "UTF-8");
    }
}
