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
package org.thisorthat.vote.image.selection;

import org.thisorthat.dto.ExhaustiveVoteState;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Pair;
import org.thisorthat.dto.Survey;

public interface ImageSelectionAlgorithm {

    Pair<Image> getNextImagePair(String surveyCode, String userId);

    default ExhaustiveVoteState getVoteState(String surveyCode, String userId) {
        Pair<Image> pair = getNextImagePair(surveyCode, userId);
        if (pair == null) {
            return new ExhaustiveVoteState(null, null, 0, 0, true);
        }
        return new ExhaustiveVoteState(pair.getT1(), pair.getT2(), 0, 0, false);
    }

    default ExhaustiveVoteState getVoteState(Survey survey, String userId) {
        return getVoteState(survey.getId(), userId);
    }
}
