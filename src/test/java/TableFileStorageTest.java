import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TableFileStorageTest {

    FileStorage fileStorage;

    @Before
    public void init() throws FileStorageException {
        fileStorage = new FileStorageImpl("target/testRoot/", 100l);
    }


    @Test
    public void SaveReadTest() throws FileStorageException, IOException, InterruptedException {
        File referenceFile = new File("src/test/resources/1.txt");
        for (Integer i = 0; i < 10; i++) {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(referenceFile));
            fileStorage.saveFile(i.toString(), inputStream);
            Thread.sleep(2);
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
    public void freeSpaceTest() throws FileStorageException {
        Assert.assertTrue("Free space test", fileStorage.freeSpaceInBytes() == (100l - 90l));
    }

    @Test

    public void deleteFileTest() throws FileStorageException {

        fileStorage.deleteFile("9");
        try {
            fileStorage.readFile("9");
        } catch (KeyNotExistFileStorageException e) {
            Assert.assertTrue("File deleting test", e.getIncorrectArgument().equals("9"));
        }
    }

    @Test
    public void purgeTest() throws FileStorageException {
        long currentFreeSpace = fileStorage.freeSpaceInBytes();
        long targetFreeSpace = currentFreeSpace + 10;
        fileStorage.purge(targetFreeSpace);
        boolean result = fileStorage.freeSpaceInBytes() > targetFreeSpace && fileStorage.freeSpaceInBytes() > 0;
        Assert.assertTrue("Purge Test", result);
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

        Path testFilesPath = Paths.get("target/testRoot");
        Files.walkFileTree(testFilesPath, new DeleteTestFilesVisitor());

    }
}

