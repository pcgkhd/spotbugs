package partialFilledArrayWithRead;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BadPartialFilledArrayWithRead {

    String readBytes(InputStream in) throws IOException {
        byte[] data = new byte[1024];
        if (in.read(data) == -1) {
            throw new EOFException();
        }
        return new String(data, "UTF-8");
    }
}
