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

Example configuration in maven pom.xml:

Under an events enabled maven plugin: 

```xml
<plugin>
    <groupId>nl.stokpop</groupId>
    <artifactId>event-scheduler-maven-plugin</artifactId>
    <!-- and more ... -->
```

define this sub-dependencies of this plugin:

```xml
    <dependencies>
        <dependency>
            <groupId>io.perfana</groupId>
            <artifactId>perfana-java-client</artifactId>
            <version>${perfana-java-client.version}</version>
        </dependency>
        <dependency>
            <groupId>io.perfana</groupId>
            <artifactId>test-events-loadrunner-cloud</artifactId>
            <version>${test-events-loadrunner-cloud.version}</version>
        </dependency>
    </dependencies>
```

and create an event for the LoadRunner Cloud:

```xml
    <events>
        <MyPerfanaEvent>
            <eventFactory>io.perfana.event.PerfanaEventFactory</eventFactory>
            <enabled>true</enabled>
            <perfanaUrl>${perfanaUrl}</perfanaUrl>
        </MyPerfanaEvent>
        <!-- here you can define LoadRunner Cloud events, with own properties per event,
             so you can form instance create two events with different load tests for example -->
        <MyLoadRunnerCloudEvent>
            <eventFactory>io.perfana.event.loadrunner.LoadRunnerCloudEventFactory</eventFactory>
            <enabled>true</enabled>
            <loadRunnerUser>${loadRunnerUser}</loadRunnerUser>
            <loadRunnerPassword>${loadRunnerPassword}</loadRunnerPassword>
            <loadRunnerTenantId>${loadRunnerTenantId}</loadRunnerTenantId>
            <loadRunnerProjectId>${loadRunnerProjectId}</loadRunnerProjectId>
            <loadRunnerLoadTestId>${loadRunnerLoadTestId}</loadRunnerLoadTestId>
        </MyLoadRunnerCloudEvent>
    </events>
```

##Todo

* create lookups for the project and test ids based on names

Works with the Stokpop event-scheduler framework: 
* https://github.com/stokpop/event-scheduler
* https://github.com/stokpop/events-gatling-maven-plugin
