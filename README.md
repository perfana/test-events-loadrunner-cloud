# test-events-loadrunner-cloud

Events to start and stop for LoadRunner Cloud tests.

This event plugin schedules a test one minute from now.

No other events than `beforeTest` event has been implemented yet.

Properties for LoadRunner Cloud:
* `loadRunnerUser` the user 
* `loadRunnerPassword` the password 
* `loadRunnerTenantId` the tenantId 
* `loadRunnerProjectId` the projectId (look up in UI for defined test)
* `loadRunnerLoadTestId` the LoadTestId (look up in UI for defined test) 
* `useProxy` on port 8888, for example to use with fiddler

## Use with events-*-maven-plugin

You can use the `test-events-loadrunner-cloud` as a plugin of the `events-*-maven-plugin`
by putting the `test-events-loadrunner-cloud` jar on the classpath of the plugin.

You can use the `dependencies` element inside the `plugin` element.

For example (from [example-pom.xml](src/test/resources/example-pom.xml)):

```xml
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
    </dependencies>
</plugin>
```

You can substitute `event-scheduler-maven-plugin` by `event-gatling-maven-plugin`, `event-jmeter-maven-plugin`
and others when available.

Try this by calling:

    mvn -f src/test/resources/example-pom.xml event-scheduler:test

##Todo

* create lookups for the project and test ids based on names

Works with the Stokpop event-scheduler framework: 
* https://github.com/stokpop/event-scheduler
* https://github.com/stokpop/events-gatling-maven-plugin
