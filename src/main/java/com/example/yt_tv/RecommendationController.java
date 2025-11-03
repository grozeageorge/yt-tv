
package com.example.yt_tv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final AIService aiService;

    public RecommendationController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(path = "/recommend", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String recommend(@RequestBody RecommendationRequest req) {
        return aiService.recommendVideos(req.interests(), req.channels());
    }

    public record RecommendationRequest(
            @NotEmpty List<String> channels,
            @NotBlank String interests
    ) {}
}