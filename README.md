# test-events-loadrunner-cloud

Events to start and stop for LoadRunner Cloud tests.

This event plugin start a LoadRunner Cloud test for the given LoadTest id.

## events
The `beforeTest` and `abortTest` event are implemented.

The `beforeTest` starts the load test and polls the LoadRunner Cloud server to see if
the test changes from `INITIALIZING` state to `RUNNING` state. 

## messages
This is a `readyForStartParticipant`, so only when this event plugin sends a `Go!` message
on the `eventMessageBus` the event scheduler can progress to the `startTest` event. 
When the LoadRunner Cloud test reaches the `RUNNING` state, the `Go!` message is sent.

## properties for LoadRunner Cloud:
* `loadRunnerUser` the user 
* `loadRunnerPassword` the password 
* `loadRunnerTenantId` the tenantId 
* `loadRunnerProjectId` the projectId
* `loadRunnerLoadTestId` the loadTestId
* `loadRunnerUseTracingHeader` send tracing header via the run-time-settings (rts) (optional, default false)
* `pollingPeriodInSeconds` seconds between check if test is in RUNNING state (optional, default 10)
* `pollingMaxDurationInSeconds` max duration to check if test gets to RUNNING state (optional, default 300)
* `useProxy` activate proxy, for example to use with [mitmproxy](https://mitmproxy.org/) 
* `proxyPort` port to use for proxy, uses localhost (optional, default 8888) 

### notes
* tenantId: look up in browser url of LoadRunner Cloud: `TENANTID=X`
* projectId: look up in browser url of LoadRunner Cloud: `projectId=Y`
* loadTestId: look up in UI for defined test: LOAD TESTS > select test > look at `ID: Z` in Summary

## variables

The LoadRunner Cloud plugin sends the following variables with a message on the `EventMessageBus`:
* perfana-lrc-tenantId
* perfana-lrc-projectId
* perfana-lrc-runId

## tracing header

When `loadRunnerUseTracingHeader` is `true`, the tracing header is sent to the script run-time-settings.
The tracing header is the test run id. 
In the scripts the tracing header can be injected as web header via `web_add_header` and `lr_get_attrib_string`:

    web_add_header("perfana-test-run-id", lr_get_attrib_string("perfanaTestRunId"))

## use with events-*-maven-plugin

You can use the `test-events-loadrunner-cloud` as a plugin of the `events-*-maven-plugin`
by putting the `test-events-loadrunner-cloud` jar on the classpath of the plugin.

Use the `dependencies` element inside the `plugin` element as in the XML snippet below.

For example, with `perfana-test-client` plugin (from [example-pom.xml](src/test/resources/example-pom.xml)):

```xml
<plugins>
    <plugin>
        <groupId>nl.stokpop</groupId>
        <artifactId>event-scheduler-maven-plugin</artifactId>
        <configuration>
            <eventSchedulerConfig>
                <debugEnabled>true</debugEnabled>
                <schedulerEnabled>true</schedulerEnabled>
                <failOnError>true</failOnError>
                <continueOnEventCheckFailure>true</continueOnEventCheckFailure>
                <eventConfigs>
                    <eventConfig implementation="io.perfana.event.loadrunner.LoadRunnerCloudEventConfig">
                        <name>LoadRunnerCloudEvent1</name>
                        <loadRunnerUser>user</loadRunnerUser>
                        <loadRunnerPassword>password</loadRunnerPassword>
                        <loadRunnerTenantId>tenantId</loadRunnerTenantId>
                        <loadRunnerProjectId>1</loadRunnerProjectId>
                        <loadRunnerLoadTestId>2</loadRunnerLoadTestId>
                        <useProxy>true</useProxy>
                    </eventConfig>
                    <eventConfig implementation="io.perfana.event.PerfanaEventConfig">
                        <name>PerfanaEvent1</name>
                        <perfanaUrl>http://localhost:8888</perfanaUrl>
                        <assertResultsEnabled>false</assertResultsEnabled>
                        <variables>
                            <_var1>my_value</_var1>
                        </variables>
                        <testConfig>
                            <systemUnderTest>${systemUnderTest}</systemUnderTest>
                            <version>${version}</version>
                            <workload>${workload}</workload>
                            <testEnvironment>${testEnvironment}</testEnvironment>
                            <testRunId>${testRunId}</testRunId>
                            <buildResultsUrl>${buildResultsUrl}</buildResultsUrl>
                            <rampupTimeInSeconds>${rampupTimeInSeconds}</rampupTimeInSeconds>
                            <constantLoadTimeInSeconds>${constantLoadTimeInSeconds}</constantLoadTimeInSeconds>
                            <annotations>${annotations}</annotations>
                            <tags>${tags}</tags>
                        </testConfig>
                    </eventConfig>
                </eventConfigs>
            </eventSchedulerConfig>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>io.perfana</groupId>
                <artifactId>test-events-loadrunner-cloud</artifactId>
                <version>${test-events-loadrunner-cloud.version}</version>
            </dependency>
            <dependency>
                <groupId>io.perfana</groupId>
                <artifactId>perfana-java-client</artifactId>
                <version>${perfana-java-client.version}</version>
            </dependency>
        </dependencies>
    </plugin>
</plugins>
```

Try this by calling:

    mvn -f src/test/resources/example-pom.xml event-scheduler:test

##Todo

* create lookups for the project and test ids based on names

Works with the Stokpop event-scheduler framework: 
* https://github.com/stokpop/event-scheduler
* https://github.com/stokpop/event-Scheduler-maven-plugin
