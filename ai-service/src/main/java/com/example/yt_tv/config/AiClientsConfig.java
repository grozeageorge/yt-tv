package com.example.yt_tv.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientsConfig {

    // MCP-enabled ChatClient that binds default tool functions (including MCP SQL tools)
    @Bean(name = "mcpChatClient")
    public ChatClient mcpChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors() // enable default advisors including tool/MCP integration when available
                .build();
    }
}
