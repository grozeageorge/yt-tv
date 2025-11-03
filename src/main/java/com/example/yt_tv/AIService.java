package com.example.yt_tv;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
@Service
public class AIService {
    private final ChatClient chatClient;

    public AIService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String recommendVideos(String userInterests, java.util.List<String> pickedChannels) {
        String prompt = """
            You are a YouTube curator. The user picked these channels: %s.
            Based on these and the user's interests - "%s" - do the following:
            1. Suggest 5 specific, currently relevant video ideas or actual video titles to watch next.
            2. Suggest 3 additional channels closely related to the picks.
            Respond in compact JSON with keys: videos [string[]], channels [string[]].
            """.formatted(String.join(", ", pickedChannels), userInterests);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}