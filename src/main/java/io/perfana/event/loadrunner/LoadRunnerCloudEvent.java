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

import io.perfana.event.loadrunner.api.RunReply;
import io.perfana.event.loadrunner.api.RuntimeAdditionalAttribute;
import io.perfana.event.loadrunner.api.TestRunActive;
import io.perfana.eventscheduler.api.EventAdapter;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessage;
import io.perfana.eventscheduler.api.message.EventMessageBus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class LoadRunnerCloudEvent extends EventAdapter<LoadRunnerCloudEventContext> {

    private static final String LOADRUNNER_CLOUD_BASE_URL = "https://loadrunner-cloud.saas.microfocus.com/v1";
    public static final String PERFANA_LRC_PREFIX = "perfana-lrc-";
    public static final String PLUGIN_NAME = LoadRunnerCloudEvent.class.getSimpleName();
    public static final String TRACING_HEADER_NAME = "perfanaTestRunId";

    private final AtomicReference<LoadRunnerCloudClient> client = new AtomicReference<>();

    private volatile int runId;

    public LoadRunnerCloudEvent(LoadRunnerCloudEventContext context, TestContext testContext, EventMessageBus messageBus, EventLogger logger) {
        super(context, testContext, messageBus, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("before test [" + testContext.getTestRunId() + "]");

        String user = eventContext.getLoadRunnerUser();
        String password = eventContext.getLoadRunnerPassword();
        String tenantId = eventContext.getLoadRunnerTenantId();
        String projectId = eventContext.getLoadRunnerProjectId();
        String loadTestId = eventContext.getLoadRunnerLoadTestId();

        boolean useProxy = eventContext.isUseProxy();

        client.set(new LoadRunnerCloudClient(LOADRUNNER_CLOUD_BASE_URL, logger, useProxy, eventContext.getProxyPort()));

        client.get().initApiKey(user, password, tenantId);

        if (eventContext.isLoadRunnerUseTracingHeader()) {
            sendTracingHeader(projectId, loadTestId);
        }
        else {
            logger.info("send tracing header is disabled");
        }

        RunReply myRunId = client.get().startRun(projectId, loadTestId);

        this.runId = myRunId.getRunId();

        EventMessage message = EventMessage.builder()
            .pluginName(pluginName())
            .variable(PERFANA_LRC_PREFIX + "tenantId", tenantId)
            .variable(PERFANA_LRC_PREFIX + "projectId", projectId)
            .variable(PERFANA_LRC_PREFIX + "runId", String.valueOf(this.runId))
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
                    List<TestRunActive> testRunActives = client.get().testRunsActive(projectId);

                    Optional<TestRunActive> testRunActive = testRunActives.stream()
                        .filter(t -> t.getRunId() == this.runId)
                        .findFirst();

                    if (testRunActive.isPresent()) {
                        TestRunActive testRun = testRunActive.get();
                        logger.info(String.format("Status for test id %s (%s) is now: %s", testRun.getTestId(), testRun.getTestName(), testRun.getStatus()));
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
                    Thread.currentThread().interrupt();
                }

                if (System.currentTimeMillis() > maxPollingTimestamp) {
                    logger.warn("Max polling period reached (" + eventContext.getPollingMaxDuration() + " seconds), will stop polling now.");
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

        Executor executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "LrcPollForTestRunning"));
        executor.execute(pollForTestRunning);

        logger.info(String.format("started run with projectId: %s loadTestId: %s at %s with runId: %s. Waiting for status RUNNING.",
            projectId, loadTestId, Instant.now(), myRunId.getRunId()));
    }

    private void sendTracingHeader(String projectId, String loadTestId) {
        String testRunId = testContext.getTestRunId();
        logger.info("send tracing header '" + TRACING_HEADER_NAME + ": " + testRunId + "'");

        RuntimeAdditionalAttribute attribute = RuntimeAdditionalAttribute.builder()
            .name(TRACING_HEADER_NAME)
            .value(testRunId)
            .description("Use in web_add_header(\"perfana-test-run-id\", lr_get_attrib_string(\"" + TRACING_HEADER_NAME + "\"))").build();

        List<RuntimeAdditionalAttribute> attributes = Collections.singletonList(attribute);
        if (client.get() != null) {
            client.get().addAdditionalRuntimeSettingsAttributesForAllScriptsOfTest(projectId, loadTestId, attributes);
        } else {
            logger.warn("Cannot add additional runtime settings attributes for all scripts of test, LoadRunnerCloudClient is null");
        }
    }

    private String pluginName() {
        return PLUGIN_NAME + "-" + eventContext.getName();
    }

    @Override
    public void abortTest() {
        logger.info("abort test [" + testContext.getTestRunId() + "] with runId [" + this.runId + "]");
        if (client.get() != null) {
            client.get().stopRun(runId);
        } else {
            logger.warn("Cannot call stop run, LoadRunnerCloudClient is null");
        }
    }

}