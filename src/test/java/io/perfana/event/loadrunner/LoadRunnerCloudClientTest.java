/*
 * Copyright (C) 2021 Peter Paul Bakker, Perfana
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
import io.perfana.event.loadrunner.api.RuntimeAdditionalAttribute;
import io.perfana.event.loadrunner.api.ScriptConfig;
import io.perfana.event.loadrunner.api.Token;
import nl.stokpop.eventscheduler.log.EventLoggerStdOut;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
        wireMockRule.stubFor(post(urlEqualTo("/projects/1/load-tests/2/runs?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForEmptyJson().build());

        ScriptConfig scriptConfig = ScriptConfig.builder().scriptId(5).build();
        wireMockRule.stubFor(get(urlEqualTo("/projects/1/load-tests/2/scripts?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new ScriptConfig[] {scriptConfig}).build());

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("testName").value("testValue").description("testDescription").build();
        wireMockRule.stubFor(put(urlEqualTo("/projects/1/load-tests/2/scripts/5/rts/additional-attributes?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new RuntimeAdditionalAttribute[] { attribute }).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey("pp", "hello", "123");
        client.startRun("1","2");
    }

    @Test
    public void addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest() {
        String testToken = "8457258394";

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));
        wireMockRule.stubFor(post(urlEqualTo("/projects/1/load-tests/2/runs?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForEmptyJson().build());

        ScriptConfig scriptConfig = ScriptConfig.builder().scriptId(5).build();
        wireMockRule.stubFor(get(urlEqualTo("/projects/1/load-tests/2/scripts?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new ScriptConfig[] {scriptConfig}).build());

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("testName").value("testValue").description("testDescription").build();
        RuntimeAdditionalAttribute[] attributes = {attribute};
        wireMockRule.stubFor(put(urlEqualTo("/projects/1/load-tests/2/scripts/5/rts/additional-attributes?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(attributes).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey("pp", "hello", "123");
        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest("1","2", Arrays.asList(attributes));
    }

    @Test
    public void scriptInfoForTestRun() {
        String testToken = "8457258394";

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));
        wireMockRule.stubFor(get(urlEqualTo("/projects/1/load-tests/2/scripts?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new ScriptConfig[] { ScriptConfig.builder().build() }).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey("pp", "hello", "123");
        List<ScriptConfig> scriptConfigs = client.scriptsForTestRun("1", "2");
        Assert.assertEquals(1, scriptConfigs.size());
    }

    /**
     * Add a the following to your unit test environment to do a real connected test:
     * LR_CLOUD_USER=user@example.com;LR_CLOUD_PW=my-password;LR_CLOUD_TENANTID=123456789
     *
     * @return an initialized LoadRunnerCloudClient
     */
    private LoadRunnerCloudClient createRealLoadRunnerCloudClient() {
        LoadRunnerCloudClient client = new LoadRunnerCloudClient("https://loadrunner-cloud.saas.microfocus.com/v1", EventLoggerStdOut.INSTANCE_DEBUG, false);
        client.initApiKey(System.getenv("LR_CLOUD_USER"), System.getenv("LR_CLOUD_PW"), System.getenv("LR_CLOUD_TENANTID"));
        return client;
    }

    @Test
    @Ignore
    public void startRealTest() {
        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();
        client.startRun("1","2");
    }

    @Test
    @Ignore
    public void scriptInfoForTestRunReal() {

        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();

        List<ScriptConfig> scriptConfigs = client.scriptsForTestRun("1", "1");
        System.out.println(scriptConfigs);
    }

    @Test
    @Ignore
    public void addAdditionalRuntimeSettingsAttributesForTestRunReal() {

        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder().name("perfanaTestRunId").value("my-test-run-1").description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))").build();
        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        List<RuntimeAdditionalAttribute> returnAttributes = client.addAdditionalRuntimeSettingsAttributes("1", "1", 1, attributes);

        System.out.println(returnAttributes);
    }

    @Test
    @Ignore
    public void addAdditionalRuntimeSettingsAttributesForAllTestRunScriptsReal() {

        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder().name("perfanaTestRunId").value("my-test-run-2").description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))").build();
        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest("1", "1", attributes);

    }
}