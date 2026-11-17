/*
 * Copyright 2018 Baloise Group
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
package org.thisorthat.job;

import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Survey;
import org.thisorthat.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.List;

@Component
public class CleanupJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CleanupJob.class);

    @Autowired
    private InMemoryDatabase inMemoryDatabase;

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void run() {
        log.info("ENTRY CleanupJob RUN()");
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        List<Survey> oldSurveys = inMemoryDatabase.getSurveysOlderThan(yesterday.getTime());
        log.info("Removing {} surveys", oldSurveys.size());
        for (Survey survey : oldSurveys) {
            try {
                inMemoryDatabase.removeSurvey(survey.getId());
            } catch (DatabaseException e) {
                log.error("Error removing old survey with id: {}", survey.getId());
            }
        }
        log.info("EXIT CleanupJob RUN()");
    }

}
