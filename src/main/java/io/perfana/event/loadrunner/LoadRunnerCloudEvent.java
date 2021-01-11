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

import io.perfana.event.loadrunner.api.RunReply;
import io.perfana.event.loadrunner.api.RuntimeAdditionalAttribute;
import nl.stokpop.eventscheduler.api.EventAdapter;
import nl.stokpop.eventscheduler.api.EventLogger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class LoadRunnerCloudEvent extends EventAdapter<LoadRunnerCloudEventConfig> {

    private static final String LOADRUNNER_CLOUD_BASE_URL = "https://loadrunner-cloud.saas.microfocus.com/v1";

    private volatile LoadRunnerCloudClient client;

    private volatile int runId;

    public LoadRunnerCloudEvent(LoadRunnerCloudEventConfig eventConfig, EventLogger logger) {
        super(eventConfig, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("before test [" + eventConfig.getTestConfig().getTestRunId() + "]");

        String user = eventConfig.getLoadRunnerUser();
        String password = eventConfig.getLoadRunnerPassword();
        String tenantId = eventConfig.getLoadRunnerTenantId();
        String projectId = eventConfig.getLoadRunnerProjectId();
        String loadTestId = eventConfig.getLoadRunnerLoadTestId();

        boolean useProxy = eventConfig.isUseProxy();

        client = new LoadRunnerCloudClient(LOADRUNNER_CLOUD_BASE_URL, logger, useProxy);

        client.initApiKey(user, password, tenantId);

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("perfanaTestRunId")
            .value(eventConfig.getTestConfig().getTestRunId())
            .description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))").build();

        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest(projectId, loadTestId, attributes);

        RunReply runId = client.startRun(projectId, loadTestId);

        this.runId = runId.getRunId();

        logger.info(String.format("started run with projectId: %s loadTestId: %s at %s with runId: %s",
            projectId, loadTestId, Instant.now(), runId.getRunId()));
    }

    @Override
    public void abortTest() {
        logger.info("abort test [" + eventConfig.getTestConfig().getTestRunId() + "] with runId [" + this.runId + "]");

        client.stopRun(runId);
    }

}