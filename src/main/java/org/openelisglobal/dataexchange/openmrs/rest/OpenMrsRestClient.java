package org.openelisglobal.dataexchange.openmrs.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Minimal JSON client for the OpenMRS REST API ({@code /ws/rest/v1}),
 * authenticated as a dedicated OpenMRS service account.
 */
@Component
public class OpenMrsRestClient {

    private static final int TIMEOUT_MS = 15000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CloseableHttpClient httpClient;

    @Value("${org.openelisglobal.openmrs.rest.uri:}")
    private String baseUri;

    @Value("${org.openelisglobal.openmrs.rest.username:}")
    private String username;

    @Value("${org.openelisglobal.openmrs.rest.password:}")
    private String password;

    public boolean isConfigured() {
        return !GenericValidator.isBlankOrNull(baseUri) && !GenericValidator.isBlankOrNull(username)
                && !GenericValidator.isBlankOrNull(password);
    }

    /**
     * @param path relative to {@code ws/rest/v1}, including any query string
     * @return the response body, or null when OpenMRS answers 404
     */
    public JsonNode get(String path) {
        return execute(new HttpGet(url(path)));
    }

    public JsonNode post(String path, Object body) {
        HttpPost post = new HttpPost(url(path));
        try {
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new OpenMrsRestException("Could not serialize OpenMRS request", e);
        }
        return execute(post);
    }

    private JsonNode execute(HttpRequestBase request) {
        request.setConfig(RequestConfig.custom().setConnectTimeout(TIMEOUT_MS).setSocketTimeout(TIMEOUT_MS)
                .setConnectionRequestTimeout(TIMEOUT_MS).build());
        request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null ? null
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (status == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            if (status < 200 || status >= 300) {
                throw new OpenMrsRestException(status, errorMessage(status, body));
            }
            return GenericValidator.isBlankOrNull(body) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        } catch (IOException e) {
            throw new OpenMrsRestException("OpenMRS is unreachable: " + e.getMessage(), e);
        }
    }

    private String errorMessage(int status, String body) {
        try {
            JsonNode message = objectMapper.readTree(body).path("error").path("message");
            if (message.isTextual() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (IOException | IllegalArgumentException e) {
            // not a JSON error body; fall through to the status line
        }
        return "OpenMRS returned HTTP " + status;
    }

    private String url(String path) {
        String base = baseUri.endsWith("/") ? baseUri : baseUri + "/";
        return base + (path.startsWith("/") ? path.substring(1) : path);
    }
}
