import com.filipov.fileservice.FileStorage;
import com.filipov.fileservice.FileStorageImpl.FileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileStoragePurgeInPercentsTest {

    static FileStorage fileStorage;

    @BeforeClass
    public static void init() throws FileStorageException {
        fileStorage = new FileStorageImpl("target/testRoot", 120l);
    }

    @Before
    public void prepareFiles() throws KeyAlreadyExistFileStorageException, NoFreeSpaceFileStorageException, FileNotFoundException {
        File referenceFile = new File("src/test/resources/1.txt");
        for (Integer i = 0; i < 10; i++) {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(referenceFile));
            fileStorage.saveFile(i.toString(), inputStream);
        }
    }

    @Test
    public void purgeInPercentsTest() throws FileStorageException {
        int targetFreeSpaceInPercents = 50;
        fileStorage.purge(targetFreeSpaceInPercents);
        boolean result = fileStorage.freeSpaceInBytes() >= 120 / 2;
        Assert.assertTrue("Purge in percents Test", result);
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
