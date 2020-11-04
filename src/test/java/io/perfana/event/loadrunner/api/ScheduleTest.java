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
package io.perfana.event.loadrunner.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertTrue;

public class ScheduleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    public void getTimestamp() throws JsonProcessingException {

        String dateAsString = "2020-01-02T03:04:05.666Z";

        Schedule schedule = Schedule.builder().timestamp(ZonedDateTime.parse(dateAsString)).build();

        String json = objectMapper.writeValueAsString(schedule);

        assertTrue("should contain " + dateAsString, json.contains(dateAsString));
    }

    @Test
    public void getTimestampUtc() throws JsonProcessingException {

        Schedule schedule = Schedule.builder().timestamp(ZonedDateTime.now(ZoneOffset.UTC)).build();

        String json = objectMapper.writeValueAsString(schedule);

        // contains Z for UTC?
        assertTrue("should contain .[0-9]{3,6}Z", json.matches(".*\\.[0-9]{3,6}Z.*"));
    }
}