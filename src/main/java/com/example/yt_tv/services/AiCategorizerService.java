package com.example.yt_tv.services;

import com.example.yt_tv.dtos.YoutubeVideoInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCategorizerService {
    private final ChatClient chatClient;

    public AiCategorizerService(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public String categorizeChannel(String channelName, List<YoutubeVideoInfo> sampleVideos, List<String> youtubeHints) {
        String videoTitles = sampleVideos.stream()
                .limit(5)
                .map(YoutubeVideoInfo::title)
                .collect(Collectors.joining("\n"));

        String categories = "K-Pop, Hip-Hop, Pop Music, Rock, Electronic, Music (General), Science, Tech, Gaming, Cooking, Vlog, News, Education, Sports, Movies, Comedy";

        String hints = String.join(", ", youtubeHints);

        String prompt = """
                Classify this YouTube Channel into ONE category.
                Channel: "%s"
                Hints: %s
                Videos: %s
                
                Allowed Categories: [%s]
                
                Rules:
                1. "Blackpink", "BTS", "NewJeans" -> K-Pop.
                2. "The Weeknd", "Taylor Swift" -> Pop Music.
                3. "Eminem" -> Hip-Hop.
                4. "Veritasium" -> Science.
                
                Return ONLY the category name.
                """.formatted(channelName, youtubeHints, videoTitles, categories);

        String response = chatClient.prompt(prompt).call().content();

        if (response != null) {
            return response.replace("\n", "").replace("\r", "").trim();
        }

        return "Uncategorized";
    }
}
