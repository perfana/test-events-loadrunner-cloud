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

import io.perfana.event.loadrunner.api.RuntimeAdditionalAttribute;
import io.perfana.event.loadrunner.api.ScriptConfig;
import io.perfana.event.loadrunner.api.TestRunActive;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class LoadRunnerCloudClientLiveTest {

    /**
     * Add a the following to your unit test environment to do a real connected test:
     * LR_CLOUD_USER=user@example.com;LR_CLOUD_PW=my-password;LR_CLOUD_TENANTID=123456789
     *
     * @return an initialized LoadRunnerCloudClient
     */
    private LoadRunnerCloudClient createRealLoadRunnerCloudClient() {
        LoadRunnerCloudClient client =
            new LoadRunnerCloudClient("https://loadrunner-cloud.saas.microfocus.com/v1", EventLoggerStdOut.INSTANCE_DEBUG);
        client.initApiKey(System.getenv("LR_CLOUD_USER"), System.getenv("LR_CLOUD_PW"), System.getenv("LR_CLOUD_TENANTID"));
        return client;
    }

    @Test
    @Ignore
    public void startRealTest() {
        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();
        client.startRun("1","3");
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

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("perfanaTestRunId")
            .value("my-test-run-1")
            .description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))")
            .build();
        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        List<RuntimeAdditionalAttribute> returnAttributes = client.addAdditionalRuntimeSettingsAttributes("1", "1", 1, attributes);

        System.out.println(returnAttributes);
    }

    @Test
    @Ignore
    public void addAdditionalRuntimeSettingsAttributesForAllTestRunScriptsReal() {

        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("perfanaTestRunId")
            .value("my-test-run-2")
            .description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))")
            .build();
        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest("1", "1", attributes);

    }

    @Test
    @Ignore
    public void testRunsActiveLive() {
        LoadRunnerCloudClient client = createRealLoadRunnerCloudClient();
        List<TestRunActive> testRunActives = client.testRunsActive("1");
        System.out.println("testRunActives:" + testRunActives);
    }
}