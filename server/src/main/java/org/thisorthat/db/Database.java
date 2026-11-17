package org.thisorthat.db;

import org.thisorthat.dto.Image;
import org.thisorthat.dto.ScoreItem;
import org.thisorthat.dto.Survey;
import org.thisorthat.dto.Vote;

import java.util.Date;
import java.util.List;

public interface Database {

    long surveyCount();

    void addSurvey(Survey survey);

    void removeSurvey(String code);

    Survey getSurvey(String code);

    String addImageToSurvey(String surveyCode, Image image);

    void startSurvey(String surveyCode);

    void stopSurvey(String surveyCode);

    Image getImageFromSurvey(String surveyCode, String imageId);

    List<Survey> getSurveysOlderThan(Date cutOffDate);

    void addScore(String surveyCode, ScoreItem scoreItem);

    void persistVote(String surveyCode, Vote vote);
}
