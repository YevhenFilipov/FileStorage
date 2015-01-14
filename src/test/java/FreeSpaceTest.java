import com.filipov.fileservice.FileStorage;
import com.filipov.fileservice.FileStorageImpl.FileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FreeSpaceTest {

    static FileStorage fileStorage;

    @BeforeClass
    public static void init() throws FileStorageException, FileNotFoundException {
        fileStorage = new FileStorageImpl("target/testRoot", 120l);
        File referenceFile = new File("src/test/resources/1.txt");
        for (Integer i = 10; i < 20; i++) {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(referenceFile));
            fileStorage.saveFile(i.toString(), inputStream);
        }
    }

    @Test
    public void freeSpaceTest() throws FileStorageException {
        Assert.assertTrue("Free space test", fileStorage.freeSpaceInBytes() == (120l - 90l));
    }

    @Test
    public void freeSpacePercentsTest() throws FileStorageException {
        int referenceResult = (120 - 90) / 120 * 100;
        Assert.assertTrue("Free space in percents test", fileStorage.freeSpaceInPercents() == referenceResult);
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
