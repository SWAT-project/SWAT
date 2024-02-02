package de.uzl.its.swat.request;

import de.uzl.its.swat.logger.SystemLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public abstract class Request {
    private static final SystemLogger systemLogger = new SystemLogger();
    private static final Logger logger = systemLogger.getLogger();

    public static void send(
            String host,
            int port,
            String path,
            int endpointID,
            int traceID,
            String requestBodyJson) {
        HttpClient httpClient = HttpClient.newHttpClient();

        String url =
                String.format(
                        "http://%s:%d/%s?endpointID=%d&traceID=%d",
                        host, port, path, endpointID, traceID);
        logger.info("Sending request to: " + url);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        requestBodyJson, StandardCharsets.UTF_8))
                        .build();
        CompletableFuture<HttpResponse<String>> responseFuture =
                httpClient.sendAsync(request, BodyHandlers.ofString());

        responseFuture
                .thenAccept(
                        response ->
                                System.out.println(
                                        "Response status code: " + response.statusCode()))
                .exceptionally(
                        e -> {
                            System.out.println("Request failed: " + e.getMessage());
                            return null;
                        });
        // Block and wait for the future to complete
        responseFuture.join();
    }
}
