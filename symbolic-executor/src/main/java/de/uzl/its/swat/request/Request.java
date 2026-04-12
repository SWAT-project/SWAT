package de.uzl.its.swat.request;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.logging.GlobalLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public abstract class Request {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

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
        logger.info("Sending request to: {}", url);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .version(HttpClient.Version.HTTP_1_1)
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        requestBodyJson, StandardCharsets.UTF_8))
                        .build();
        logger.trace("Request body: {}", requestBodyJson);
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
