import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

public class TableFileStorageTest {

    @Test
    public void SaveReadTest() throws FileStorageException, IOException {
        FileStorage fileStorage = new FileStorageImpl("target/testRoot/", 1024);
        File referenceFile = new File("src/test/resources/1.txt");
        for (Integer i = 0; i < 10; i++) {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(referenceFile));
            fileStorage.saveFile(i.toString(), inputStream);
        }
        InputStream inputStream = fileStorage.readFile("9");
        File readFile = new File("target/result.txt");
        readFile.createNewFile();
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(readFile));
        int buffer;
        while (true) {
            buffer = inputStream.read();
            if (buffer == -1)
                break;
            outputStream.write(buffer);
        }
        inputStream.close();
        outputStream.flush();
        outputStream.close();

        Assert.assertTrue("Write and read file test", readFile.length() == referenceFile.length());
        File testDirs = new File("target/testRoot/");
        readFile.delete();

    }
}

