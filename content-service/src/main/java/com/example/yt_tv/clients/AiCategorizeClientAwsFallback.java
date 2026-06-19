package com.example.yt_tv.clients;

import com.example.yt_tv.dtos.ChannelCategorizeRequest;
import com.example.yt_tv.dtos.ChannelCategorizeResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation used when the 'aws' profile is active.
 * On AWS we don't call the ai-service, so return an empty/unknown category.
 */
@Component
@Profile("aws")
public class AiCategorizeClientAwsFallback implements AiCategorizeClient {
    @Override
    public ChannelCategorizeResponse categorize(ChannelCategorizeRequest request) {
        ChannelCategorizeResponse resp = new ChannelCategorizeResponse();
        resp.setCategory("");
        return resp;
    }
}

