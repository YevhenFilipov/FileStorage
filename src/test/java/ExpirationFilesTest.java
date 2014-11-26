import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageImpl;
import org.junit.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ExpirationFilesTest {

    static FileStorage fileStorage;

    @BeforeClass
    public static void init() throws FileStorageException {
        fileStorage = new FileStorageImpl("target/testRoot", 120l);
    }

    @Before
    public void prepareExpirationFile() throws FileNotFoundException, KeyAlreadyExistFileStorageException, NoFreeSpaceFileStorageException, InterruptedException {
        File referenceFile2 = new File("src/test/resources/1.txt");
        InputStream inputStream2 = new BufferedInputStream(new FileInputStream(referenceFile2));
        final String key = "expirationFile";
        fileStorage.saveFile(key, inputStream2, 1 * 1000l);

        Thread.sleep(3 * 1000l);
    }

    @Test
    public void expirationFileAutoDeleteTest() {
        boolean result;

        try {
            fileStorage.readFile("expirationFile");
            result = false;
        } catch (KeyNotExistFileStorageException e) {
            result = true;
        }
        Assert.assertTrue("Auto delete expiration file test", result);
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
