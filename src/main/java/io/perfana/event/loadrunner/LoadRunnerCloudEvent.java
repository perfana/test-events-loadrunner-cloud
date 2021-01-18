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
import io.perfana.event.loadrunner.api.TestRunActive;
import nl.stokpop.eventscheduler.api.EventAdapter;
import nl.stokpop.eventscheduler.api.EventLogger;
import nl.stokpop.eventscheduler.api.message.EventMessage;
import nl.stokpop.eventscheduler.api.message.EventMessageBus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoadRunnerCloudEvent extends EventAdapter<LoadRunnerCloudEventContext> {

    private static final String LOADRUNNER_CLOUD_BASE_URL = "https://loadrunner-cloud.saas.microfocus.com/v1";
    public static final String PERFANA_LCR_PREFIX = "perfana-lcr-";
    public static final String PLUGIN_NAME = LoadRunnerCloudEvent.class.getSimpleName();

    private volatile LoadRunnerCloudClient client;

    private volatile int runId;

    public LoadRunnerCloudEvent(LoadRunnerCloudEventContext context, EventMessageBus messageBus, EventLogger logger) {
        super(context, messageBus, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("before test [" + eventContext.getTestContext().getTestRunId() + "]");

        String user = eventContext.getLoadRunnerUser();
        String password = eventContext.getLoadRunnerPassword();
        String tenantId = eventContext.getLoadRunnerTenantId();
        String projectId = eventContext.getLoadRunnerProjectId();
        String loadTestId = eventContext.getLoadRunnerLoadTestId();

        boolean useProxy = eventContext.isUseProxy();

        client = new LoadRunnerCloudClient(LOADRUNNER_CLOUD_BASE_URL, logger, useProxy, eventContext.getProxyPort());

        client.initApiKey(user, password, tenantId);

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name("perfanaTestRunId")
            .value(eventContext.getTestContext().getTestRunId())
            .description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"perfanaTestRunId\"))").build();

        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);

        client.addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest(projectId, loadTestId, attributes);

        RunReply runId = client.startRun(projectId, loadTestId);

        this.runId = runId.getRunId();

        EventMessage message = EventMessage.builder()
            .pluginName(pluginName())
            .variable(PERFANA_LCR_PREFIX + "tenantId", tenantId)
            .variable(PERFANA_LCR_PREFIX + "projectId", projectId)
            .variable(PERFANA_LCR_PREFIX + "runId", String.valueOf(this.runId))
            .build();
        eventMessageBus.send(message);

        logger.info(String.format("started polling if running for projectId: %s loadTestId: %s at %s with runId: %s",
            projectId, loadTestId, Instant.now(), this.runId));

        Runnable pollForTestRunning = () -> {

            long sleepInMillis = eventContext.getPollingPeriod().toMillis();
            long maxPollingTimestamp = System.currentTimeMillis() + eventContext.getPollingMaxDuration().toMillis();

            boolean continuePolling = true;

            while (continuePolling) {

                // now start polling if load test is running, then send Go! message
                try {
                    List<TestRunActive> testRunActives = client.testRunsActive(projectId);

                    Optional<TestRunActive> testRunActive = testRunActives.stream()
                        .filter(t -> t.getTestId() == this.runId)
                        .findFirst();

                    if (testRunActive.isPresent()) {
                        TestRunActive testRun = testRunActive.get();
                        logger.info(String.format("Found status for test id %s (%s) is %s", testRun.getStatus(), testRun.getTestId(), testRun.getTestName()));
                        if (testRun.getStatus() == TestRunActive.Status.RUNNING) {
                            continuePolling = false;
                        }
                    }
                    } catch (LoadRunnerCloudClientException e) {
                        logger.warn("Cannot call test runs active, will retry: " + e.getMessage());
                    }

                try {
                    Thread.sleep(sleepInMillis);
                } catch (InterruptedException e) {
                    logger.warn("Interrupt received, will stop polling now.");
                    continuePolling = false;
                    EventMessage stopMessage = EventMessage.builder()
                        .pluginName(pluginName())
                        .message("Stop!")
                        .build();
                    eventMessageBus.send(stopMessage);
                }

                if (System.currentTimeMillis() > maxPollingTimestamp) {
                    logger.warn("Max polling period reached (" + eventContext.getPollingPeriod() + " seconds), will stop polling now.");
                    continuePolling = false;
                    EventMessage stopMessage = EventMessage.builder()
                        .pluginName(pluginName())
                        .message("Stop!")
                        .build();
                    eventMessageBus.send(stopMessage);
                }
            }

            EventMessage goMessage = EventMessage.builder()
                .pluginName(pluginName())
                .message("Go!")
                .build();

            eventMessageBus.send(goMessage);
        };

        Executor executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "LcrPollForTestRunning"));
        executor.execute(pollForTestRunning);

        logger.info(String.format("started run with projectId: %s loadTestId: %s at %s with runId: %s. Waiting for status RUNNING.",
            projectId, loadTestId, Instant.now(), runId.getRunId()));
    }

    private String pluginName() {
        return PLUGIN_NAME + "-" + eventContext.getName();
    }

    @Override
    public void abortTest() {
        logger.info("abort test [" + eventContext.getTestContext().getTestRunId() + "] with runId [" + this.runId + "]");
        client.stopRun(runId);
    }

}