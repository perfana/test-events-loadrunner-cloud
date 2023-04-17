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

import io.perfana.eventscheduler.api.config.EventContext;

import java.time.Duration;

public class LoadRunnerCloudEventContext extends EventContext {
    private final String loadRunnerUser;
    private final String loadRunnerPassword;
    private final String loadRunnerTenantId;
    private final String loadRunnerProjectId;
    private final String loadRunnerLoadTestId;
    private final boolean loadRunnerUseTracingHeader;
    private final Duration pollingPeriod;
    private final Duration pollingMaxDuration;
    private final boolean useProxy;
    private final int proxyPort;

    LoadRunnerCloudEventContext(
        EventContext context,
        String loadRunnerUser,
        String loadRunnerPassword,
        String loadRunnerTenantId,
        String loadRunnerProjectId,
        String loadRunnerLoadTestId,
        boolean loadRunnerUseTracingHeader,
        Duration pollingPeriod,
        Duration pollingMaxDuration,
        boolean useProxy,
        int proxyPort) {
            super(context, LoadRunnerCloudEventFactory.class.getName());
            this.loadRunnerUser = loadRunnerUser;
            this.loadRunnerPassword = loadRunnerPassword;
            this.loadRunnerTenantId = loadRunnerTenantId;
            this.loadRunnerProjectId = loadRunnerProjectId;
            this.loadRunnerLoadTestId = loadRunnerLoadTestId;
            this.loadRunnerUseTracingHeader = loadRunnerUseTracingHeader;
            this.pollingPeriod = pollingPeriod;
            this.pollingMaxDuration = pollingMaxDuration;
            this.useProxy = useProxy;
            this.proxyPort = proxyPort;
    }

    public String getLoadRunnerUser() {
        return loadRunnerUser;
    }

    public String getLoadRunnerPassword() {
        return loadRunnerPassword;
    }

    public String getLoadRunnerTenantId() {
        return loadRunnerTenantId;
    }

    public String getLoadRunnerProjectId() {
        return loadRunnerProjectId;
    }

    public String getLoadRunnerLoadTestId() {
        return loadRunnerLoadTestId;
    }

    public Duration getPollingPeriod() {
        return pollingPeriod;
    }

    public Duration getPollingMaxDuration() {
        return pollingMaxDuration;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean isLoadRunnerUseTracingHeader() {
        return loadRunnerUseTracingHeader;
    }
}
