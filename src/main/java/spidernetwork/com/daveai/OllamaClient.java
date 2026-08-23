package spidernetwork.com.daveai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final HttpClient httpClient;
    private final String modelName;

    public OllamaClient(String modelName) {
        this.modelName = modelName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generateResponse(String prompt) {
        try {
            String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

            String jsonBody = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                    modelName, escapedPrompt
            );

            System.out.println("Sending request to Ollama..."); // DEBUG

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Received status: " + response.statusCode()); // DEBUG
            System.out.println("Raw body: " + response.body()); // DEBUG

            if (response.statusCode() == 200) {
                return parseResponseText(response.body());
            } else {
                return "Error: Ollama returned status code " + response.statusCode();
            }

        } catch (Exception e) {
            e.printStackTrace(); // Prints full error to IntelliJ console
            return "Error connecting to Ollama: " + e.getMessage();
        }
    }

    private String parseResponseText(String jsonResponse) {
        String key = "\"response\":\"";
        int startIndex = jsonResponse.indexOf(key);
        if (startIndex == -1) return "Error parsing AI response.";

        startIndex += key.length();
        StringBuilder result = new StringBuilder();

        boolean escaped = false;
        for (int i = startIndex; i < jsonResponse.length(); i++) {
            char c = jsonResponse.charAt(i);
            if (escaped) {
                if (c == 'n') result.append('\n');
                else if (c == '"') result.append('"');
                else if (c == '\\') result.append('\\');
                else result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString().trim();
    }
}
