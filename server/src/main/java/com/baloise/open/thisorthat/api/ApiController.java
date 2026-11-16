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
package com.baloise.open.thisorthat.api;

import com.baloise.open.thisorthat.api.dto.*;
import com.baloise.open.thisorthat.dto.Image;
import com.baloise.open.thisorthat.dto.Survey;
import com.baloise.open.thisorthat.exception.*;
import com.baloise.open.thisorthat.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Tag(name = "Surveys", description = "Endpoints for creating, managing, voting, and retrieving results for surveys")
public class ApiController {

    private final SurveyService surveyService;

    public ApiController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @PostMapping(path = "/create", consumes = "application/json")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Create a new survey", description = "Initializes a new survey with the provided perspective (question/topic) and returns the generated survey code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Survey successfully created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SurveyResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public SurveyResponse createSurvey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details for the survey to be created", required = true)
            @RequestBody CreateSurveyRequest createSurveyRequest) {
        try {
            Survey survey = surveyService.createSurvey(createSurveyRequest.getPerspective());
            return new SurveyResponse(survey.getId());
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @DeleteMapping("/{code}/delete")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Delete a survey", description = "Permanently deletes a survey and all associated votes, scores, and images.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Survey successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public void deleteSurvey(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode) {
        try {
            surveyService.deleteSurvey(surveyCode);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @PostMapping("/{code}/start")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Start a survey", description = "Opens the survey for voting and initializes score tracking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Survey successfully started"),
            @ApiResponse(responseCode = "400", description = "Survey already started or stopped"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public void startSurvey(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode) {
        try {
            surveyService.startSurvey(surveyCode);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @PostMapping("/{code}/stop")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Stop a survey", description = "Closes the survey for further voting and calculates the final scores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Survey successfully stopped"),
            @ApiResponse(responseCode = "400", description = "Survey already stopped"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public void stopSurvey(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode) {
        try {
            surveyService.stopSurvey(surveyCode);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @GetMapping(value = "/{code}/vote")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Get next pair of images to vote on", description = "Returns a pair of image IDs for comparison according to the selection algorithm.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Next image pair retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Survey is stopped"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public VoteResponse getVote(
            @Parameter(description = "Identifier of the voting user", required = true, example = "user-123")
            @RequestHeader("userId") String userId,
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode) {
        try {
            return surveyService.getVote(surveyCode, userId);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @PostMapping(value = "/{code}/vote", consumes = "application/json")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Submit a vote", description = "Records a user's selection between two images (winner vs loser).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vote successfully registered"),
            @ApiResponse(responseCode = "400", description = "Survey is stopped"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public ResponseEntity<VoteResponse> setVote(
            @Parameter(description = "Identifier of the voting user", required = true, example = "user-123")
            @RequestHeader("userId") String userId,
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Vote decision specifying winner and loser", required = true)
            @RequestBody VoteRequest voteRequest) {
        try {
            surveyService.setVote(surveyCode, voteRequest, userId);
            return ok().build();
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @GetMapping(value = "/{code}/score")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Get survey scores and statistics", description = "Retrieves ranking and scores of all images once the survey has ended.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scores retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScoreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Survey is still running"),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public ScoreResponse getScore(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode) {
        try {
            return surveyService.getScore(surveyCode);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @PostMapping(value = "/{code}/image", consumes = "application/json")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Add an image to a survey", description = "Uploads an image (as base64 string) to the survey and returns its assigned image ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image successfully added",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Survey not found")
    })
    public ImageResponse addImageToSurvey(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Image payload containing base64 data", required = true)
            @RequestBody ImageRequest imageRequest) {
        try {
            Image image = new Image(null, imageRequest.getFile());
            String id = surveyService.addImageToSurvey(surveyCode, image);

            return new ImageResponse(id);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    @GetMapping(value = "/{code}/image/{imageId}", produces = "image/jpeg")
    @CrossOrigin(origins = "*")
    @Operation(summary = "Download an image by ID", description = "Returns the binary JPEG image data for the specified image ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Binary JPEG image data",
                    content = @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "404", description = "Survey or image not found")
    })
    public byte[] getImageFromSurvey(
            @Parameter(description = "Unique survey code", required = true, example = "1pv1")
            @PathVariable("code") String surveyCode,
            @Parameter(description = "Identifier of the image", required = true, example = "0")
            @PathVariable("imageId") String imageId) {
        try {
            Image image = surveyService.getImageFromSurvey(surveyCode, imageId);
            String base64Image = image.getFile().split(",")[1];
            return Base64.getDecoder().decode(base64Image);
        } catch (Exception e) {
            throw buildError(e);
        }
    }

    private ResponseStatusException buildError(Exception exception) {
        if (exception instanceof SurveyNotFoundException) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        } else if (exception instanceof ImageNotFoundException) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        } else if (exception instanceof SurveyStillRunningException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } else if (exception instanceof SurveyStoppedException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } else if (exception instanceof SurveyAlreadyStartedException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occured: " + exception.getMessage(), exception);
        }
    }

}
