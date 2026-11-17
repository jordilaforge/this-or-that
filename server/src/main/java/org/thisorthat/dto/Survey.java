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
package org.thisorthat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Survey {
    private Date creationDate;
    private String id;
    private List<Image> images = new ArrayList<>();
    private Boolean started = false;
    private List<ScoreItem> scores = new ArrayList<>();
    private List<Vote> votes = new ArrayList<>();
    private String perspective;

    public Survey() {
    }

    public Survey(String id, String perspective) {
        this(new Date(), id, new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), perspective);
    }

    public Survey(Date creationDate, String id, List<Image> images, Boolean started, List<ScoreItem> scores, List<Vote> votes, String perspective) {
        this.creationDate = creationDate;
        this.id = id;
        this.images = images != null ? images : new ArrayList<>();
        this.started = started != null ? started : false;
        this.scores = scores != null ? scores : new ArrayList<>();
        this.votes = votes != null ? votes : new ArrayList<>();
        this.perspective = perspective;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public Boolean getStarted() {
        return started;
    }

    public void setStarted(Boolean started) {
        this.started = started;
    }

    public List<ScoreItem> getScores() {
        return scores;
    }

    public void setScores(List<ScoreItem> scores) {
        this.scores = scores;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    public String getPerspective() {
        return perspective;
    }

    public void setPerspective(String perspective) {
        this.perspective = perspective;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Survey survey = (Survey) o;
        return Objects.equals(creationDate, survey.creationDate) &&
                Objects.equals(id, survey.id) &&
                Objects.equals(images, survey.images) &&
                Objects.equals(started, survey.started) &&
                Objects.equals(scores, survey.scores) &&
                Objects.equals(votes, survey.votes) &&
                Objects.equals(perspective, survey.perspective);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, id, images, started, scores, votes, perspective);
    }

    @Override
    public String toString() {
        return "Survey{" +
                "creationDate=" + creationDate +
                ", id='" + id + '\'' +
                ", images=" + images +
                ", started=" + started +
                ", scores=" + scores +
                ", votes=" + votes +
                ", perspective='" + perspective + '\'' +
                '}';
    }
}
