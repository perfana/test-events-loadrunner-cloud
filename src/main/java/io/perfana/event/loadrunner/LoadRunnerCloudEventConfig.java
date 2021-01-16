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

import nl.stokpop.eventscheduler.api.config.EventConfig;

public class LoadRunnerCloudEventConfig extends EventConfig {
    private String loadRunnerUser;
    private String loadRunnerPassword;
    private String loadRunnerTenantId;
    private String loadRunnerProjectId;
    private String loadRunnerLoadTestId;
    private int pollingPeriodInSeconds = 10;
    private int pollingMaxDurationInSeconds = 300;
    private boolean useProxy = false;
    private int proxyPort = 8888;

    @Override
    public String getEventFactory() {
        return LoadRunnerCloudEventFactory.class.getName();
    }

    @Override
    public boolean isReadyForStartParticipant() {
        return true;
    }

    public String getLoadRunnerUser() {
        return loadRunnerUser;
    }

    public void setLoadRunnerUser(String loadRunnerUser) {
        this.loadRunnerUser = loadRunnerUser;
    }

    public String getLoadRunnerPassword() {
        return loadRunnerPassword;
    }

    public void setLoadRunnerPassword(String loadRunnerPassword) {
        this.loadRunnerPassword = loadRunnerPassword;
    }

    public String getLoadRunnerTenantId() {
        return loadRunnerTenantId;
    }

    public void setLoadRunnerTenantId(String loadRunnerTenantId) {
        this.loadRunnerTenantId = loadRunnerTenantId;
    }

    public String getLoadRunnerProjectId() {
        return loadRunnerProjectId;
    }

    public void setLoadRunnerProjectId(String loadRunnerProjectId) {
        this.loadRunnerProjectId = loadRunnerProjectId;
    }

    public String getLoadRunnerLoadTestId() {
        return loadRunnerLoadTestId;
    }

    public void setLoadRunnerLoadTestId(String loadRunnerLoadTestId) {
        this.loadRunnerLoadTestId = loadRunnerLoadTestId;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public int getPollingPeriodInSeconds() {
        return pollingPeriodInSeconds;
    }

    public void setPollingPeriodInSeconds(int pollingPeriodInSeconds) {
        this.pollingPeriodInSeconds = pollingPeriodInSeconds;
    }

    public int getPollingMaxDurationInSeconds() {
        return pollingMaxDurationInSeconds;
    }

    public void setPollingMaxDurationInSeconds(int pollingMaxDurationInSeconds) {
        this.pollingMaxDurationInSeconds = pollingMaxDurationInSeconds;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
}
