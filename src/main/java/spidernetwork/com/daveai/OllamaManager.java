package spidernetwork.com.daveai;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaManager {

    private static final String BASE_URL = "http://localhost:11434";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private static Process ollamaProcess = null;

    public boolean isServerRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean startServerProcess() {
        try {
            // Find the exact path to Ollama on Windows
            String userAppData = System.getenv("LOCALAPPDATA");
            String ollamaPath = userAppData + File.separator + "Programs" + File.separator + "Ollama" + File.separator + "ollama.exe";

            File exeFile = new File(ollamaPath);
            if (!exeFile.exists()) {
                System.out.println("Ollama executable not found at: " + ollamaPath);
                return false;
            }

            ProcessBuilder pb = new ProcessBuilder(ollamaPath, "serve");
            pb.inheritIO();
            ollamaProcess = pb.start();

            Runtime.getRuntime().addShutdownHook(new Thread(OllamaManager::stopServerProcess));

            for (int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                if (isServerRunning()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not auto-start Ollama process: " + e.getMessage());
        }
        return false;
    }

    public static void stopServerProcess() {
        if (ollamaProcess != null && ollamaProcess.isAlive()) {
            try {
                System.out.println("Shutting down background Ollama server...");
                ollamaProcess.destroyForcibly();
            } catch (Exception e) {
                System.out.println("Error stopping Ollama process: " + e.getMessage());
            }
        }
    }

    public void ensureModelReady(String modelName) {
        new Thread(() -> {
            try {
                String jsonBody = String.format("{\"model\": \"%s\", \"stream\": false}", modelName);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/pull"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.out.println("Model check/pull error: " + e.getMessage());
            }
        }).start();
    }
}