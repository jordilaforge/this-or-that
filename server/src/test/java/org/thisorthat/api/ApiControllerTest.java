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
package org.thisorthat.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.thisorthat.api.dto.*;
import org.thisorthat.dto.Image;
import org.thisorthat.dto.Survey;
import org.thisorthat.exception.*;
import org.thisorthat.service.SurveyService;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiControllerTest {

    @Mock
    private SurveyService surveyService;

    @InjectMocks
    private ApiController apiController;

    @Test
    @DisplayName("createSurvey should delegate to service and return SurveyResponse")
    void testCreateSurvey() {
        CreateSurveyRequest req = new CreateSurveyRequest("Perspective A");
        Survey survey = new Survey("code-123", "Perspective A");
        when(surveyService.createSurvey("Perspective A")).thenReturn(survey);

        SurveyResponse res = apiController.createSurvey(req);

        assertNotNull(res);
        assertEquals("code-123", res.code());
    }

    @Test
    @DisplayName("deleteSurvey should delegate to service")
    void testDeleteSurvey() {
        apiController.deleteSurvey("code-123");
        verify(surveyService).deleteSurvey("code-123");
    }

    @Test
    @DisplayName("startSurvey should delegate to service")
    void testStartSurvey() {
        apiController.startSurvey("code-123");
        verify(surveyService).startSurvey("code-123");
    }

    @Test
    @DisplayName("stopSurvey should delegate to service")
    void testStopSurvey() {
        apiController.stopSurvey("code-123");
        verify(surveyService).stopSurvey("code-123");
    }

    @Test
    @DisplayName("getVote should delegate to service and return VoteResponse")
    void testGetVote() {
        VoteResponse expected = new VoteResponse("0", "1", true, "Persp");
        when(surveyService.getVote("code-123", "user-1")).thenReturn(expected);

        VoteResponse actual = apiController.getVote("user-1", "code-123");

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("setVote should delegate to service and return 200 OK")
    void testSetVote() {
        VoteRequest req = new VoteRequest("0", "1");
        ResponseEntity<VoteResponse> response = apiController.setVote("user-1", "code-123", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(surveyService).setVote("code-123", req, "user-1");
    }

    @Test
    @DisplayName("getScore should delegate to service and return ScoreResponse")
    void testGetScore() {
        ScoreResponse expected = new ScoreResponse(List.of(new Score("0", 10)), false, "P", 1, 1);
        when(surveyService.getScore("code-123")).thenReturn(expected);

        ScoreResponse actual = apiController.getScore("code-123");
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("addImageToSurvey should delegate to service and return ImageResponse")
    void testAddImageToSurvey() {
        ImageRequest req = new ImageRequest("data:image/jpeg;base64,abc");
        when(surveyService.addImageToSurvey(eq("code-123"), any(Image.class))).thenReturn("img-0");

        ImageResponse res = apiController.addImageToSurvey("code-123", req);
        assertEquals("img-0", res.id());
    }

    @Test
    @DisplayName("getImageFromSurvey should decode base64 file data correctly")
    void testGetImageFromSurvey() {
        byte[] originalBytes = new byte[]{1, 2, 3, 4};
        String base64Data = Base64.getEncoder().encodeToString(originalBytes);
        Image image = new Image("0", "data:image/jpeg;base64," + base64Data);
        when(surveyService.getImageFromSurvey("code-123", "0")).thenReturn(image);

        byte[] result = apiController.getImageFromSurvey("code-123", "0");
        assertArrayEquals(originalBytes, result);
    }

    @Test
    @DisplayName("buildError should map SurveyNotFoundException to 404 NOT_FOUND")
    void testBuildErrorSurveyNotFound() {
        doThrow(new SurveyNotFoundException("Survey not found")).when(surveyService).deleteSurvey("unknown");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.deleteSurvey("unknown"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("buildError should map ImageNotFoundException to 404 NOT_FOUND")
    void testBuildErrorImageNotFound() {
        doThrow(new ImageNotFoundException("Image not found")).when(surveyService).getImageFromSurvey("code", "img-99");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.getImageFromSurvey("code", "img-99"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("buildError should map SurveyStillRunningException to 400 BAD_REQUEST")
    void testBuildErrorSurveyStillRunning() {
        doThrow(new SurveyStillRunningException("Survey running")).when(surveyService).getScore("code");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.getScore("code"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("buildError should map SurveyStoppedException to 400 BAD_REQUEST")
    void testBuildErrorSurveyStopped() {
        doThrow(new SurveyStoppedException("Survey stopped")).when(surveyService).stopSurvey("code");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.stopSurvey("code"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("buildError should map SurveyAlreadyStartedException to 400 BAD_REQUEST")
    void testBuildErrorSurveyAlreadyStarted() {
        doThrow(new SurveyAlreadyStartedException("Already started")).when(surveyService).startSurvey("code");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.startSurvey("code"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("buildError should map generic Exception to 500 INTERNAL_SERVER_ERROR")
    void testBuildErrorGenericException() {
        doThrow(new RuntimeException("Unexpected error")).when(surveyService).startSurvey("code");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> apiController.startSurvey("code"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
}
