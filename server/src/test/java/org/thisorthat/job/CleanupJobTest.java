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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thisorthat.db.InMemoryDatabase;
import org.thisorthat.dto.Survey;
import org.thisorthat.exception.DatabaseException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupJobTest {

    @Mock
    private InMemoryDatabase inMemoryDatabase;

    @InjectMocks
    private CleanupJob cleanupJob;

    @Test
    @DisplayName("CleanupJob should remove all surveys older than yesterday")
    void testCleanupOldSurveys() {
        Survey oldSurvey1 = new Survey("old-1", "P1");
        Survey oldSurvey2 = new Survey("old-2", "P2");
        when(inMemoryDatabase.getSurveysOlderThan(any(Date.class)))
                .thenReturn(List.of(oldSurvey1, oldSurvey2));

        cleanupJob.run();

        verify(inMemoryDatabase).removeSurvey("old-1");
        verify(inMemoryDatabase).removeSurvey("old-2");
    }

    @Test
    @DisplayName("CleanupJob should handle DatabaseException gracefully when survey removal fails")
    void testCleanupHandlesException() {
        Survey problematicSurvey = new Survey("problematic", "P");
        when(inMemoryDatabase.getSurveysOlderThan(any(Date.class)))
                .thenReturn(List.of(problematicSurvey));
        doThrow(new DatabaseException("Deletion failed"))
                .when(inMemoryDatabase).removeSurvey("problematic");

        assertDoesNotThrow(() -> cleanupJob.run());
        verify(inMemoryDatabase).removeSurvey("problematic");
    }

    @Test
    @DisplayName("CleanupJob does nothing when no surveys are older than threshold")
    void testCleanupNoSurveys() {
        when(inMemoryDatabase.getSurveysOlderThan(any(Date.class)))
                .thenReturn(Collections.emptyList());

        cleanupJob.run();

        verify(inMemoryDatabase, never()).removeSurvey(anyString());
    }
}
