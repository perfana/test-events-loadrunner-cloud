/*
 * Copyright (C) 2020 Peter Paul Bakker, Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.event.loadrunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.perfana.event.loadrunner.api.*;
import nl.stokpop.eventscheduler.api.EventLogger;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

class LoadRunnerCloudClient {
    
    public static final String PARAM_TENANTID = "TENANTID";
    private static final String PARAM_RUN_ACTION = "action";

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ObjectReader tokenReader = objectMapper.readerFor(Token.class);
    private final ObjectReader scheduleReplyReader = objectMapper.readerFor(ScheduleReply.class);
    private final ObjectReader runReplyReader = objectMapper.readerFor(RunReply.class);
    private final ObjectWriter authWriter = objectMapper.writerFor(Auth.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final EventLogger logger;

    private final BasicCookieStore cookieStore = new BasicCookieStore();
    private final String host;
    private volatile boolean isCookiePresent = false;
    private volatile String tenantId;

    public LoadRunnerCloudClient(String baseUrl, EventLogger logger, boolean useProxy) {
        try {
            URL url = new URL(baseUrl);
            this.host = url.getHost();
        } catch (MalformedURLException e) {
            throw new LoadRunnerCloudClientException("Invalid base url provided: " + baseUrl, e);
        }
        this.baseUrl = removeLastSlashIfPresent(baseUrl);
        this.logger = logger;
        this.httpClient = createHttpClient(useProxy);
    }

    private String removeLastSlashIfPresent(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Adds the api key to the cookie store.
     * @param user LoadRunner user id
     * @param password LoadRunner password
     * @param tenantId LoadRunner tenantId
     */
    public void initApiKey(String user, String password, String tenantId) {

        notEmpty(user, "user");
        notEmpty(password, "password");
        notEmpty(tenantId, "tenantId");

        String apiKey = fetchApiKey(baseUrl, user, password, tenantId);
        this.tenantId = tenantId;

        BasicClientCookie cookie = new BasicClientCookie("LWSSO_COOKIE_KEY", apiKey);
        cookie.setDomain(host);
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        isCookiePresent = true;
    }

    private void notEmpty(String user, String name) {
        if (user == null || user.isEmpty()) {
            throw new LoadRunnerCloudClientException(name + " is null or empty");
        }
    }

    private String fetchApiKey(String baseUrl, String user, String password, String tenantId) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl + "/auth");
            uriBuilder.addParameter(PARAM_TENANTID, tenantId);

            HttpPost httpPost = new HttpPost(uriBuilder.build());

            Auth auth = Auth.builder().user(user).password(password).build();
            String json = authWriter.writeValueAsString(auth);

            StringEntity data = new StringEntity(json, ContentType.APPLICATION_JSON);

            httpPost.setEntity(data);

            HttpResponse response = executeRequest(httpPost);

            String result = responseToString(response);
            logger.debug(result);

            Token token = tokenReader.readValue(result);
            return token.getToken();
        } catch (URISyntaxException | IOException e) {
            throw new LoadRunnerCloudClientException("call to LoadRunner cloud failed", e);
        }
    }

    private HttpClient createHttpClient(boolean useProxy) {

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(1000)
            .setConnectTimeout(1000)
            .setSocketTimeout(5000).build();

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(requestConfig);

        if (useProxy) {
            HttpHost httpProxy = new HttpHost("localhost", 8888);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(httpProxy);
            httpClientBuilder.setRoutePlanner(routePlanner);
        }

        return httpClientBuilder.build();
    }

    private static String responseToString(HttpResponse response) throws IOException {
        StringBuilder result = new StringBuilder(1024);
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()))) {

            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private HttpResponse executeRequest(HttpUriRequest request) throws IOException {
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode > 299) {
            String result = responseToString(response);
            throw new LoadRunnerCloudClientException(String.format("Unexpected status code: %d for request: %s. Contents: %s", statusCode, request, result));
        }
        return response;
    }

    /**
     * Schedules a run one minute from now.
     *
     * @param projectId number of the project
     * @param loadTestId number of the loadTest
     */
    public ScheduleReply createSchedule(String projectId, String loadTestId) {
        checkApiKey();

        String uri = String.format("%s/projects/%s/load-tests/%s/schedules", baseUrl, projectId, loadTestId);

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(PARAM_TENANTID, tenantId);

            HttpPost httpPost = new HttpPost(uriBuilder.build());

            // need to provide UTC time
            ZonedDateTime startTime = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(1);
            Schedule schedule = Schedule.builder().timestamp(startTime).build();

            String json = objectMapper.writeValueAsString(schedule);
            StringEntity data = new StringEntity(json, ContentType.APPLICATION_JSON);

            httpPost.setEntity(data);

            HttpResponse response = executeRequest(httpPost);
            String result = responseToString(response);
            logger.debug(result);

            return scheduleReplyReader.readValue(result);

        } catch (URISyntaxException | IOException e) {
            throw new LoadRunnerCloudClientException("call to LoadRunner cloud failed", e);
        }
    }

    /**
     * Start a run immediately.
     *
     * @param projectId number of the project
     * @param loadTestId number of the loadTest
     */
    public RunReply startRun(String projectId, String loadTestId) {
        checkApiKey();

        String uri = String.format("%s/projects/%s/load-tests/%s/runs", baseUrl, projectId, loadTestId);

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(PARAM_TENANTID, tenantId);

            HttpPost httpPost = new HttpPost(uriBuilder.build());

            HttpResponse response = executeRequest(httpPost);
            String result = responseToString(response);
            logger.debug(result);

            return runReplyReader.readValue(result);

        } catch (URISyntaxException | IOException e) {
            throw new LoadRunnerCloudClientException("call to LoadRunner cloud failed", e);
        }
    }

    /**
     * Stop a run immediately.
     *
     * @param runId number of the run
     */
    public RunReply stopRun(int runId) {
        checkApiKey();

        String uri = String.format("%s/test-runs/%d", baseUrl, runId);

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(PARAM_TENANTID, tenantId);
            uriBuilder.addParameter(PARAM_RUN_ACTION, "STOP");

            HttpPut httpPut = new HttpPut(uriBuilder.build());

            HttpResponse response = executeRequest(httpPut);
            String result = responseToString(response);
            logger.debug(result);

            return runReplyReader.readValue(result);

        } catch (URISyntaxException | IOException e) {
            throw new LoadRunnerCloudClientException("call to LoadRunner cloud failed", e);
        }
    }

    private void checkApiKey() {
        if (!isCookiePresent) throw new LoadRunnerCloudClientException("No LoadRunner cloud client api key present. First call initApiKey with credentials.");
    }
}
