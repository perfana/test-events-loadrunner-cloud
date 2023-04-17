/*
 * Copyright (C) 2023 Peter Paul Bakker, Perfana
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
import io.perfana.event.loadrunner.api.TestRunActive;
import io.perfana.event.loadrunner.api.Token;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class LoadRunnerCloudClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8568).httpsPort(8569));

    @Test
    public void startTest() {
        String testToken = "8457258394";

        wireMockRule.resetAll();

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

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG);
        client.initApiKey("pp", "hello", "123");
        client.startRun("1","2");
    }

    @Test
    public void addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest() {
        String testToken = "8457258394";

        wireMockRule.resetAll();

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));
        wireMockRule.stubFor(post(urlEqualTo("/projects/1/load-tests/2/runs?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForEmptyJson().build());

        ScriptConfig scriptConfig = ScriptConfig.builder().id(5).scriptId(501).build();
        wireMockRule.stubFor(get(urlEqualTo("/projects/1/load-tests/2/scripts?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new ScriptConfig[] {scriptConfig}).build());

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("testName").value("testValue").description("testDescription").build();
        RuntimeAdditionalAttribute[] attributes = {attribute};
        wireMockRule.stubFor(put(urlEqualTo("/projects/1/load-tests/2/scripts/5/rts/additional-attributes?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(attributes).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG);
        client.initApiKey("pp", "hello", "123");
        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest("1","2", Arrays.asList(attributes));
    }

    @Test
    public void scriptInfoForTestRun() {
        String testToken = "8457258394";

        wireMockRule.resetAll();

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));
        wireMockRule.stubFor(get(urlEqualTo("/projects/1/load-tests/2/scripts?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.okForJson(new ScriptConfig[] { ScriptConfig.builder().build() }).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG);
        client.initApiKey("pp", "hello", "123");
        List<ScriptConfig> scriptConfigs = client.scriptsForTestRun("1", "2");
        Assert.assertEquals(1, scriptConfigs.size());
    }

    @Test
    public void testRunsActiveStates() {
        String testToken = "8457258394";

        wireMockRule.resetAll();

        long tsStart1 = Instant.parse("2020-12-03T10:15:30Z").toEpochMilli();
        long tsStart2 = Instant.parse("2020-12-03T06:17:28Z").toEpochMilli();

        String testName1 = "testName1";
        String testName2 = "testName2";

        TestRunActive testRun1a = TestRunActive.builder()
            .projectId(1).testId(1).runId(1).testName(testName1)
            .startTime(tsStart1)
            .status(TestRunActive.Status.INITIALIZING)
            .build();

        TestRunActive testRun2a = TestRunActive.builder()
            .projectId(1).testId(2).runId(10).testName(testName2)
            .startTime(tsStart2)
            .status(TestRunActive.Status.PAUSED)
            .build();

        TestRunActive testRun1b = TestRunActive.builder()
            .projectId(1).testId(1).runId(1).testName(testName1)
            .startTime(tsStart1)
            .status(TestRunActive.Status.RUNNING)
            .build();

        TestRunActive testRun2b = TestRunActive.builder()
            .projectId(1).testId(2).runId(10).testName("testName2")
            .startTime(tsStart2)
            .status(TestRunActive.Status.STOPPING)
            .build();

        Token token = Token.builder().token(testToken).build();
        wireMockRule.stubFor(post(urlEqualTo("/auth?TENANTID=123")))
            .setResponse(ResponseDefinitionBuilder.jsonResponse(token));

        // make first call return no RUNNING test, in second call, test1 is RUNNING
        String nextCall = "next-call";
        String scenarioName = "running-test";

        wireMockRule.stubFor(get(urlEqualTo("/test-runs/active?TENANTID=123&projectIds=1"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(STARTED)
                .willSetStateTo(nextCall))
            .setResponse(ResponseDefinitionBuilder.okForJson(new TestRunActive[] { testRun1a, testRun2a }).build());

        wireMockRule.stubFor(get(urlEqualTo("/test-runs/active?TENANTID=123&projectIds=1"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(nextCall))
            .setResponse(ResponseDefinitionBuilder.okForJson(new TestRunActive[] { testRun1b, testRun2b }).build());

        LoadRunnerCloudClient client = new LoadRunnerCloudClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG);
        client.initApiKey("pp", "hello", "123");

        List<TestRunActive> testRunActives1 = client.testRunsActive("1");
        System.out.println("testRunActives:" + testRunActives1);
        Assert.assertEquals(2, testRunActives1.size());
        Assert.assertFalse("no running test expected",
            testRunActives1.stream().anyMatch(t -> t.getStatus() == TestRunActive.Status.RUNNING));

        List<TestRunActive> testRunActives2 = client.testRunsActive("1");
        System.out.println("testRunActives:" + testRunActives2);
        Assert.assertEquals(2, testRunActives2.size());
        Assert.assertTrue("running test expected in second call",
            testRunActives2.stream().anyMatch(t -> t.getStatus() == TestRunActive.Status.RUNNING));
    }
}