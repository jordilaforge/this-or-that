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
package org.thisorthat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.thisorthat.api.dto.*;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIntegrationTest {

    private static final String SAMPLE_BASE64_JPEG =
            "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=";

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        this.restTemplate = new RestTemplate();
        // Allow assertions directly on error status codes (4xx, 5xx)
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("Application context loads successfully with embedded web server")
    void contextLoads() {
        assertTrue(port > 0);
    }

    @Test
    @DisplayName("OpenAPI spec and Actuator endpoints are accessible")
    void testOpenApiAndActuatorEndpoints() {
        ResponseEntity<String> apiDocs = restTemplate.getForEntity(url("/v3/api-docs"), String.class);
        assertEquals(HttpStatus.OK, apiDocs.getStatusCode());
        assertNotNull(apiDocs.getBody());
        assertTrue(apiDocs.getBody().contains("This-Or-That Voting API") || apiDocs.getBody().contains("openapi"));

        ResponseEntity<String> swaggerUi = restTemplate.getForEntity(url("/swagger-ui.html"), String.class);
        assertTrue(swaggerUi.getStatusCode().is2xxSuccessful() || swaggerUi.getStatusCode().is3xxRedirection());

        ResponseEntity<String> health = restTemplate.getForEntity(url("/actuator/health"), String.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertNotNull(health.getBody());
        assertTrue(health.getBody().contains("UP"));
    }

    @Test
    @DisplayName("Full survey lifecycle: create, add images, start, vote, stop, and retrieve scores")
    void testFullSurveyLifecycle() {
        // 1. Create survey with perspective
        CreateSurveyRequest createReq = new CreateSurveyRequest("Which design looks better?");
        ResponseEntity<SurveyResponse> createResp = restTemplate.postForEntity(
                url("/create"),
                createReq,
                SurveyResponse.class
        );
        assertEquals(HttpStatus.OK, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        String code = createResp.getBody().code();
        assertNotNull(code);
        assertFalse(code.isBlank());

        // 2. Add first image (ID = "0")
        ImageRequest imgReq1 = new ImageRequest(SAMPLE_BASE64_JPEG);
        ResponseEntity<ImageResponse> imgResp1 = restTemplate.postForEntity(
                url("/" + code + "/image"),
                imgReq1,
                ImageResponse.class
        );
        assertEquals(HttpStatus.OK, imgResp1.getStatusCode());
        assertNotNull(imgResp1.getBody());
        assertEquals("0", imgResp1.getBody().id());

        // 3. Add second image (ID = "1")
        ImageRequest imgReq2 = new ImageRequest(SAMPLE_BASE64_JPEG);
        ResponseEntity<ImageResponse> imgResp2 = restTemplate.postForEntity(
                url("/" + code + "/image"),
                imgReq2,
                ImageResponse.class
        );
        assertEquals(HttpStatus.OK, imgResp2.getStatusCode());
        assertNotNull(imgResp2.getBody());
        assertEquals("1", imgResp2.getBody().id());

        // 4. Retrieve uploaded image binary
        ResponseEntity<byte[]> getImageResp = restTemplate.getForEntity(
                url("/" + code + "/image/0"),
                byte[].class
        );
        assertEquals(HttpStatus.OK, getImageResp.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, getImageResp.getHeaders().getContentType());
        assertNotNull(getImageResp.getBody());
        assertTrue(getImageResp.getBody().length > 0);

        // Verify decoded bytes match expected base64 content
        byte[] expectedBytes = Base64.getDecoder().decode(SAMPLE_BASE64_JPEG.split(",")[1]);
        assertArrayEquals(expectedBytes, getImageResp.getBody());

        // 5. Start survey
        ResponseEntity<Void> startResp = restTemplate.postForEntity(url("/" + code + "/start"), null, Void.class);
        assertEquals(HttpStatus.OK, startResp.getStatusCode());

        // 6. Request next vote pair with userId header
        HttpHeaders headers = new HttpHeaders();
        headers.set("userId", "user-123");
        HttpEntity<Void> voteRequestEntity = new HttpEntity<>(headers);

        ResponseEntity<VoteResponse> getVoteResp = restTemplate.exchange(
                url("/" + code + "/vote"),
                HttpMethod.GET,
                voteRequestEntity,
                VoteResponse.class
        );
        assertEquals(HttpStatus.OK, getVoteResp.getStatusCode());
        assertNotNull(getVoteResp.getBody());
        assertTrue(getVoteResp.getBody().surveyIsRunning());
        assertEquals("Which design looks better?", getVoteResp.getBody().perspective());
        assertNotNull(getVoteResp.getBody().id1());
        assertNotNull(getVoteResp.getBody().id2());

        // 7. Submit vote
        VoteRequest voteReq = new VoteRequest("0", "1");
        HttpEntity<VoteRequest> setVoteEntity = new HttpEntity<>(voteReq, headers);
        ResponseEntity<Void> setVoteResp = restTemplate.exchange(
                url("/" + code + "/vote"),
                HttpMethod.POST,
                setVoteEntity,
                Void.class
        );
        assertEquals(HttpStatus.OK, setVoteResp.getStatusCode());

        // 8. Attempt score retrieval before survey stops -> returns 400 BAD_REQUEST
        ResponseEntity<String> runningScore = restTemplate.getForEntity(url("/" + code + "/score"), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, runningScore.getStatusCode());

        // 9. Stop survey
        ResponseEntity<Void> stopResp = restTemplate.postForEntity(url("/" + code + "/stop"), null, Void.class);
        assertEquals(HttpStatus.OK, stopResp.getStatusCode());

        // 10. Get final scores
        ResponseEntity<ScoreResponse> scoreResp = restTemplate.getForEntity(url("/" + code + "/score"), ScoreResponse.class);
        assertEquals(HttpStatus.OK, scoreResp.getStatusCode());
        assertNotNull(scoreResp.getBody());
        assertFalse(scoreResp.getBody().surveyIsRunning());
        assertEquals("Which design looks better?", scoreResp.getBody().perspective());
        assertEquals(1, scoreResp.getBody().numberOfVotes());
        assertEquals(1, scoreResp.getBody().numberOfUsers());
        assertFalse(scoreResp.getBody().scores().isEmpty());
        assertEquals(1516, scoreResp.getBody().scores().get(0).score());
        assertEquals(1484, scoreResp.getBody().scores().get(1).score());
        assertEquals(org.thisorthat.vote.image.selection.SelectionMode.EXHAUSTIVE, scoreResp.getBody().selectionMode());

        // 11. Attempting to delete a stopped survey throws SurveyStoppedException -> returns 400 BAD_REQUEST
        ResponseEntity<String> deleteStoppedResp = restTemplate.exchange(
                url("/" + code + "/delete"),
                HttpMethod.DELETE,
                null,
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, deleteStoppedResp.getStatusCode());
    }

    @Test
    @DisplayName("Delete survey while running successfully deletes it")
    void testDeleteRunningSurvey() {
        // 1. Create survey
        CreateSurveyRequest createReq = new CreateSurveyRequest("Temporary survey to delete");
        ResponseEntity<SurveyResponse> createResp = restTemplate.postForEntity(
                url("/create"),
                createReq,
                SurveyResponse.class
        );
        assertEquals(HttpStatus.OK, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        String code = createResp.getBody().code();

        // 2. Add two images and start
        restTemplate.postForEntity(url("/" + code + "/image"), new ImageRequest(SAMPLE_BASE64_JPEG), ImageResponse.class);
        restTemplate.postForEntity(url("/" + code + "/image"), new ImageRequest(SAMPLE_BASE64_JPEG), ImageResponse.class);
        ResponseEntity<Void> startResp = restTemplate.postForEntity(url("/" + code + "/start"), null, Void.class);
        assertEquals(HttpStatus.OK, startResp.getStatusCode());

        // 3. Delete while running
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                url("/" + code + "/delete"),
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.OK, deleteResp.getStatusCode());

        // 4. Confirm deleted survey is no longer found
        ResponseEntity<String> getAfterDelete = restTemplate.getForEntity(url("/" + code + "/score"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, getAfterDelete.getStatusCode());
    }

    @Test
    @DisplayName("Error scenarios for invalid input, state violations, and non-existent surveys")
    void testErrorScenarios() {
        // Non-existent survey returns 404
        ResponseEntity<String> notFoundScore = restTemplate.getForEntity(url("/non-existent-code/score"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, notFoundScore.getStatusCode());

        ResponseEntity<String> notFoundStart = restTemplate.postForEntity(url("/non-existent-code/start"), null, String.class);
        assertEquals(HttpStatus.NOT_FOUND, notFoundStart.getStatusCode());

        // Missing required userId header returns 400
        ResponseEntity<String> missingHeaderVote = restTemplate.getForEntity(url("/non-existent-code/vote"), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, missingHeaderVote.getStatusCode());

        // Starting a survey that was already started
        CreateSurveyRequest createReq = new CreateSurveyRequest("State test");
        ResponseEntity<SurveyResponse> createResp = restTemplate.postForEntity(url("/create"), createReq, SurveyResponse.class);
        String code = createResp.getBody().code();
        restTemplate.postForEntity(url("/" + code + "/image"), new ImageRequest(SAMPLE_BASE64_JPEG), ImageResponse.class);
        restTemplate.postForEntity(url("/" + code + "/image"), new ImageRequest(SAMPLE_BASE64_JPEG), ImageResponse.class);
        restTemplate.postForEntity(url("/" + code + "/start"), null, Void.class);

        ResponseEntity<String> doubleStart = restTemplate.postForEntity(url("/" + code + "/start"), null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, doubleStart.getStatusCode());

        // Stopping survey then attempting double stop
        restTemplate.postForEntity(url("/" + code + "/stop"), null, Void.class);
        ResponseEntity<String> doubleStop = restTemplate.postForEntity(url("/" + code + "/stop"), null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, doubleStop.getStatusCode());
    }
}
