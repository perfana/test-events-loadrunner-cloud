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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.perfana.event.loadrunner.api.Token;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class LoadRunnerCloudClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8568).httpsPort(8569));

    @Test
    public void startTest() {
        String testToken = "8457258394";

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));
        wireMockRule.stubFor(post(urlEqualTo("/projects/1/load-tests/2/schedules?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForEmptyJson().build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey("pp", "hello", "123");
        client.startTest("1","2");
    }

    @Test
    @Ignore
    public void startRealTest() {
        LoadRunnerCloudClient client = new LoadRunnerCloudClient("https://loadrunner-cloud.saas.microfocus.com/v1", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey(System.getenv("LR_CLOUD_USER"), System.getenv("LR_CLOUD_PW"), System.getenv("LR_CLOUD_TENANTID"));
        client.startTest("1","2");
    }
}