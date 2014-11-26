import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileStorageTest {

    static FileStorage fileStorage;

    @BeforeClass
    public static void init() throws FileStorageException, FileNotFoundException, InterruptedException {
        fileStorage = new FileStorageImpl("target/testRoot", 120l);

        File referenceFile = new File("src/test/resources/1.txt");
        InputStream inputStream = new BufferedInputStream(new FileInputStream(referenceFile));
        fileStorage.saveFile("KillMe", inputStream);


    }

    @Test
    public void SaveReadTest() throws FileStorageException, IOException, InterruptedException {
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
        readFile.delete();
    }

    @Test

    public void deleteFileTest() throws FileStorageException {

        fileStorage.deleteFile("KillMe");
        try {
            fileStorage.readFile("KillMe");
        } catch (KeyNotExistFileStorageException e) {
            Assert.assertTrue("File deleting test", true);
        }
    }

    @AfterClass
    public static void cleanAll() throws IOException {
        deleteTestFiles();
    }

    private static void deleteTestFiles() throws IOException {

        class DeleteTestFilesVisitor extends SimpleFileVisitor<Path> {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        }

        Path testFilesPath = Paths.get("target/testRoot/userData");
        Files.walkFileTree(testFilesPath, new DeleteTestFilesVisitor());

    }
}

