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

import io.perfana.event.loadrunner.api.ScheduleReply;
import nl.stokpop.eventscheduler.api.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LoadRunnerCloudEvent extends EventAdapter {

    private static final String LOADRUNNER_CLOUD_BASE_URL = "https://loadrunner-cloud.saas.microfocus.com/v1";

    public static final String PROP_LOAD_RUNNER_USER = "loadRunnerUser";
    public static final String PROP_LOAD_RUNNER_PASSWORD = "loadRunnerPassword";
    public static final String PROP_LOAD_RUNNER_TENANT_ID = "loadRunnerTenantId";
    public static final String PROP_LOAD_RUNNER_PROJECT_ID = "loadRunnerProjectId";
    public static final String PROP_LOAD_RUNNER_LOAD_TEST_ID = "loadRunnerLoadTestId";

    public static final String PROP_USE_PROXY = "useProxy";

    private static final Set<String> ALLOWED_PROPERTIES = setOf(PROP_LOAD_RUNNER_USER, PROP_LOAD_RUNNER_PASSWORD, PROP_USE_PROXY, PROP_LOAD_RUNNER_TENANT_ID, PROP_LOAD_RUNNER_PROJECT_ID, PROP_LOAD_RUNNER_LOAD_TEST_ID);
    private static final Set<String> ALLOWED_CUSTOM_EVENTS = Collections.emptySet();

    private LoadRunnerCloudClient client;

    public LoadRunnerCloudEvent(String eventName, TestContext testContext, EventProperties eventProperties, EventLogger logger) {
        super(eventName, testContext, eventProperties, logger);
    }

    @Override
    public void beforeTest() {
        logger.info("before test [" + testContext.getTestRunId() + "]");

        String user = findProperty(PROP_LOAD_RUNNER_USER);
        String password = findProperty(PROP_LOAD_RUNNER_PASSWORD);
        String tenantId = findProperty(PROP_LOAD_RUNNER_TENANT_ID);
        String projectId = findProperty(PROP_LOAD_RUNNER_PROJECT_ID);
        String loadTestId = findProperty(PROP_LOAD_RUNNER_LOAD_TEST_ID);

        boolean useProxy = Boolean.parseBoolean(eventProperties.getPropertyOrDefault(PROP_USE_PROXY, "false"));

        client = new LoadRunnerCloudClient(LOADRUNNER_CLOUD_BASE_URL, logger, useProxy);

        client.initApiKey(user, password, tenantId);

        ScheduleReply scheduleReply = client.startTest(projectId, loadTestId);

        logger.info(String.format("started projectId: %s loadTestId: %s at %s with scheduleId: %s",
            projectId, loadTestId, scheduleReply.getTimestamp(), scheduleReply.getScheduleId()));
    }

    private String findProperty(String propertyName) {
        String property = eventProperties.getProperty(propertyName);
        if (property == null) {
            throw new LoadRunnerCloudEventException(String.format("property %s is not set", propertyName));
        }
        return property;
    }

    @Override
    public void customEvent(CustomEvent scheduleEvent) {
        logger.debug("ignoring unknown event [" + eventName + "]");
    }

    @Override
    public Collection<String> allowedProperties() {
        return ALLOWED_PROPERTIES;
    }

    @Override
    public Collection<String> allowedCustomEvents() {
        return ALLOWED_CUSTOM_EVENTS;
    }
}