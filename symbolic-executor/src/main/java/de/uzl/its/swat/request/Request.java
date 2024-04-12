package de.uzl.its.swat.request;

import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public abstract class Request {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Request.class);

    public static void send(String host, int port, String path, int endpointID, int traceID, String requestBodyJson) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String url = String.format("http://%s:%d/%s?endpointID=%d&traceID=%d", host, port, path, endpointID, traceID);
        logger.info("Sending request to: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Log the response status code and body
            logger.info("Response status code: {}", response.statusCode());
            logger.info("Response body: {}", response.body());
        } catch (Exception e) {
            // Log the exception details
            logger.error("Request failed: ", e);
        }
    }
}
