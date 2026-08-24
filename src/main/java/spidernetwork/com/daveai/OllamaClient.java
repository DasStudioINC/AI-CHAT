package spidernetwork.com.daveai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private final HttpClient httpClient;

    // Removed final modelName from constructor so it can change dynamically
    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Accept the active modelName directly from your UI dropdown when chat() is called
    public String chat(String modelName, List<ChatMessage> history) {
        try {
            // Build JSON messages array manually to avoid extra library dependencies
            StringBuilder jsonMessages = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                String escapedContent = msg.getContent()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n");

                jsonMessages.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}", msg.getRole(), escapedContent));
                if (i < history.size() - 1) {
                    jsonMessages.append(",");
                }
            }
            jsonMessages.append("]");

            String jsonBody = String.format(
                    "{\"model\": \"%s\", \"messages\": %s, \"stream\": false}",
                    modelName, jsonMessages.toString()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseChatResponseText(response.body());
            } else {
                return "Error: Ollama returned status code " + response.statusCode();
            }

        } catch (Exception e) {
            return "Error connecting to Ollama: " + e.getMessage();
        }
    }

    private String parseChatResponseText(String jsonResponse) {
        // Looks for the assistant response structure in /api/chat output
        String key = "\"content\":\"";
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