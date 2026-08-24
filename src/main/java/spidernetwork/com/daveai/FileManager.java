package spidernetwork.com.daveai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileManager {

    private static String filePath = "E:/DaveAI_Data/Chats";
    public static void createFile(String fileName, String content) throws Exception {
        Path path = Path.of(filePath, fileName);

        Files.writeString(path, content);
    }

    public static String readFile(String fileName) throws Exception {
        Path path = Path.of(filePath,fileName);

        return Files.readString(path);
    }

    public static void updateFile(String fileName, String newContent) throws Exception {
        Path path = Path.of(filePath,fileName);

        Files.writeString(path, newContent);
    }

    public static void appendFile(String fileName, String content) throws Exception {
        Path path = Path.of(filePath,fileName);

        Files.writeString(
                path,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    public static List<String> getFileNames() throws Exception {
        return Files.list(Path.of(filePath))
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .toList();
    }
}
