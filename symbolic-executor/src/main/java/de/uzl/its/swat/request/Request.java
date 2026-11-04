package de.uzl.its.swat.request;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.logging.GlobalLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class Request {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /**
     * POSTs the JSON body to the explorer and waits for its response.
     *
     * <p>Uses {@link HttpURLConnection}, not {@code java.net.http.HttpClient}: every executor run
     * sends one request and then exits, and creating an HttpClient (its SSL context with the CA
     * certificates, selector thread, executor and some 200 classes) cost about a fifth of a short
     * run's CPU time, although the request is plain HTTP to localhost.
     */
    public static void send(
            String host,
            int port,
            String path,
            int endpointID,
            int traceID,
            String requestBodyJson) {
        String url =
                String.format(
                        "http://%s:%d/%s?endpointID=%d&traceID=%d",
                        host, port, path, endpointID, traceID);
        logger.info("Sending request to: {}", url);
        logger.trace("Request body: {}", requestBodyJson);
        byte[] body = requestBodyJson.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            int status = connection.getResponseCode();
            // Read the response to its end (as the previous HttpClient implementation did)
            try (InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                if (in != null) {
                    in.readAllBytes();
                }
            }
            System.out.println("Response status code: " + status);
        } catch (IOException e) {
            System.out.println("Request failed: " + e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
