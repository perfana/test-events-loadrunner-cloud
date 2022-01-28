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

import io.perfana.eventscheduler.api.config.EventConfig;
import io.perfana.eventscheduler.api.config.EventContext;
import io.perfana.eventscheduler.api.config.TestContext;

import java.time.Duration;

public class LoadRunnerCloudEventConfig extends EventConfig {
    private String loadRunnerUser;
    private String loadRunnerPassword;
    private String loadRunnerTenantId;
    private String loadRunnerProjectId;
    private String loadRunnerLoadTestId;
    private boolean loadRunnerUseTracingHeader = false;
    private int pollingPeriodInSeconds = 10;
    private int pollingMaxDurationInSeconds = 300;
    private boolean useProxy = false;
    private int proxyPort = 8888;

    public void setLoadRunnerUser(String loadRunnerUser) {
        this.loadRunnerUser = loadRunnerUser;
    }

    public void setLoadRunnerPassword(String loadRunnerPassword) {
        this.loadRunnerPassword = loadRunnerPassword;
    }

    public void setLoadRunnerTenantId(String loadRunnerTenantId) {
        this.loadRunnerTenantId = loadRunnerTenantId;
    }

    public void setLoadRunnerProjectId(String loadRunnerProjectId) {
        this.loadRunnerProjectId = loadRunnerProjectId;
    }

    public void setLoadRunnerLoadTestId(String loadRunnerLoadTestId) {
        this.loadRunnerLoadTestId = loadRunnerLoadTestId;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public void setPollingPeriodInSeconds(int pollingPeriodInSeconds) {
        this.pollingPeriodInSeconds = pollingPeriodInSeconds;
    }

    public void setPollingMaxDurationInSeconds(int pollingMaxDurationInSeconds) {
        this.pollingMaxDurationInSeconds = pollingMaxDurationInSeconds;
    }

    public void setLoadRunnerUseTracingHeader(boolean loadRunnerUseTracingHeader) {
        this.loadRunnerUseTracingHeader = loadRunnerUseTracingHeader;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    private LoadRunnerCloudEventContext createLoadRunnerCloudEventContext(EventContext context) {
        Duration pollingPeriod = Duration.ofSeconds(this.pollingPeriodInSeconds);
        Duration pollingMaxDuration = Duration.ofSeconds(this.pollingMaxDurationInSeconds);
        return new LoadRunnerCloudEventContext(context,
            loadRunnerUser,
            loadRunnerPassword,
            loadRunnerTenantId,
            loadRunnerProjectId,
            loadRunnerLoadTestId,
            loadRunnerUseTracingHeader,
            pollingPeriod,
            pollingMaxDuration,
            useProxy,
            proxyPort);
    }

    @Override
    public LoadRunnerCloudEventContext toContext() {
        EventContext context = super.toContext();
        return createLoadRunnerCloudEventContext(context);
    }

    @Override
    public EventContext toContext(TestContext overrideTestContext) {
        return createLoadRunnerCloudEventContext(super.toContext(overrideTestContext));
    }
}
