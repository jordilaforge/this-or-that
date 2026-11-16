package com.baloise.open.thisorthat.dto;

import com.baloise.open.thisorthat.api.dto.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DtoRecordTest {

    @Test
    void testCreateSurveyRequestRecord() {
        CreateSurveyRequest req = new CreateSurveyRequest("test perspective");
        assertEquals("test perspective", req.perspective());
        assertEquals("test perspective", req.getPerspective());

        CreateSurveyRequest other = new CreateSurveyRequest("test perspective");
        assertEquals(req, other);
        assertEquals(req.hashCode(), other.hashCode());
        assertTrue(req.toString().contains("test perspective"));
    }

    @Test
    void testImageRequestRecord() {
        ImageRequest req = new ImageRequest("data:image/png;base64,abc");
        assertEquals("data:image/png;base64,abc", req.file());
        assertEquals("data:image/png;base64,abc", req.getFile());

        ImageRequest other = new ImageRequest("data:image/png;base64,abc");
        assertEquals(req, other);
        assertEquals(req.hashCode(), other.hashCode());
    }

    @Test
    void testImageResponseRecord() {
        ImageResponse res = new ImageResponse("img-1");
        assertEquals("img-1", res.id());
        assertEquals("img-1", res.getId());

        ImageResponse other = new ImageResponse("img-1");
        assertEquals(res, other);
        assertEquals(res.hashCode(), other.hashCode());
    }

    @Test
    void testScoreRecord() {
        Score score = new Score("img-1", 100);
        assertEquals("img-1", score.imageId());
        assertEquals("img-1", score.getImageId());
        assertEquals(100, score.score());
        assertEquals(100, score.getScore());

        Score other = new Score("img-1", 100);
        assertEquals(score, other);
        assertEquals(score.hashCode(), other.hashCode());
    }

    @Test
    void testScoreResponseRecord() {
        List<Score> scores = List.of(new Score("img-1", 10));
        ScoreResponse res = new ScoreResponse(scores, true, "persp", 5, 3);

        assertEquals(scores, res.scores());
        assertEquals(scores, res.getScores());
        assertTrue(res.getSurveyIsRunning());
        assertTrue(res.surveyIsRunning());
        assertEquals("persp", res.getPerspective());
        assertEquals("persp", res.perspective());
        assertEquals(5, res.getNumberOfVotes());
        assertEquals(5, res.numberOfVotes());
        assertEquals(3, res.getNumberOfUsers());
        assertEquals(3, res.numberOfUsers());
    }

    @Test
    void testSurveyResponseRecord() {
        SurveyResponse res = new SurveyResponse("code-123");
        assertEquals("code-123", res.code());
        assertEquals("code-123", res.getCode());

        SurveyResponse other = new SurveyResponse("code-123");
        assertEquals(res, other);
    }

    @Test
    void testVoteRequestRecord() {
        VoteRequest req = new VoteRequest("w1", "l1");
        assertEquals("w1", req.winner());
        assertEquals("w1", req.getWinner());
        assertEquals("l1", req.loser());
        assertEquals("l1", req.getLoser());

        VoteRequest other = new VoteRequest("w1", "l1");
        assertEquals(req, other);
    }

    @Test
    void testVoteResponseRecord() {
        VoteResponse res = new VoteResponse("1", "2", true, "p");
        assertEquals("1", res.getId1());
        assertEquals("1", res.id1());
        assertEquals("2", res.getId2());
        assertEquals("2", res.id2());
        assertTrue(res.getSurveyIsRunning());
        assertTrue(res.surveyIsRunning());
        assertEquals("p", res.getPerspective());
        assertEquals("p", res.perspective());
    }

    @Test
    void testPairRecord() {
        Pair<String> pair = new Pair<>("a", "b");
        assertEquals("a", pair.t1());
        assertEquals("a", pair.getT1());
        assertEquals("b", pair.t2());
        assertEquals("b", pair.getT2());

        Pair<String> other = new Pair<>("a", "b");
        assertEquals(pair, other);
        assertEquals(pair.hashCode(), other.hashCode());
    }

    @Test
    void testScoreItemRecord() {
        ScoreItem item = new ScoreItem("img-1", 42);
        assertEquals("img-1", item.imageId());
        assertEquals("img-1", item.getImageId());
        assertEquals(42, item.score());
        assertEquals(42, item.getScore());

        ScoreItem other = new ScoreItem("img-1", 42);
        assertEquals(item, other);
        assertEquals(item.hashCode(), other.hashCode());
    }

    @Test
    void testVoteRecord() {
        Vote vote = new Vote("w", "l", "u1");
        assertEquals("w", vote.winner());
        assertEquals("w", vote.getWinner());
        assertEquals("l", vote.loser());
        assertEquals("l", vote.getLoser());
        assertEquals("u1", vote.userId());
        assertEquals("u1", vote.getUserId());

        Vote other = new Vote("w", "l", "u1");
        assertEquals(vote, other);
        assertEquals(vote.hashCode(), other.hashCode());
    }

    @Test
    void testVoteItemRecord() {
        Image img1 = new Image("1", "f1");
        Image img2 = new Image("2", "f2");
        VoteItem item = new VoteItem(img1, img2);
        assertEquals(img1, item.file1());
        assertEquals(img1, item.getFile1());
        assertEquals(img2, item.file2());
        assertEquals(img2, item.getFile2());

        VoteItem other = new VoteItem(img1, img2);
        assertEquals(item, other);
        assertEquals(item.hashCode(), other.hashCode());
    }

    @Test
    void testImageRecord() {
        Image img = new Image("1", "abc");
        assertEquals("1", img.id());
        assertEquals("1", img.getId());
        assertEquals("abc", img.file());
        assertEquals("abc", img.getFile());

        Image same = new Image("1", "abc");
        assertEquals(img, same);
        assertEquals(img.hashCode(), same.hashCode());
        assertTrue(img.toString().contains("1"));
    }

    @Test
    void testSurveyClass() {
        Date now = new Date();
        Survey survey = new Survey(now, "srv-1", new ArrayList<>(), false, new ArrayList<>(), new ArrayList<>(), "p");

        assertEquals("srv-1", survey.getId());
        assertEquals(now, survey.getCreationDate());
        assertEquals("p", survey.getPerspective());
        assertFalse(survey.getStarted());

        survey.setStarted(true);
        assertTrue(survey.getStarted());

        survey.setId("srv-2");
        assertEquals("srv-2", survey.getId());

        survey.setPerspective("new-p");
        assertEquals("new-p", survey.getPerspective());

        survey.setImages(Collections.singletonList(new Image("1", "f1")));
        assertEquals(1, survey.getImages().size());

        survey.setScores(Collections.singletonList(new ScoreItem("1", 10)));
        assertEquals(1, survey.getScores().size());

        survey.setVotes(Collections.singletonList(new Vote("1", "2", "u")));
        assertEquals(1, survey.getVotes().size());

        Survey quickSurvey = new Survey("quick-1", "quick-p");
        assertEquals("quick-1", quickSurvey.getId());
        assertEquals("quick-p", quickSurvey.getPerspective());
        assertNotNull(quickSurvey.getCreationDate());
        assertFalse(quickSurvey.getStarted());
        assertTrue(quickSurvey.getImages().isEmpty());
    }
}
