package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.ChannelCategorizeRequest;
import com.example.yt_tv.dtos.ChannelCategorizeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service")
public interface AiCategorizeClient {
    @PostMapping(value = "/api/ai/categorize-channel")
    ChannelCategorizeResponse categorize(@RequestBody ChannelCategorizeRequest request);
}
