package com.avadhoot.workforgeai.ai.service;

import com.avadhoot.workforgeai.ai.dto.ChatRequest;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.dto.PromptPreviewResponse;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.prompt.BuiltPrompt;
import com.avadhoot.workforgeai.ai.prompt.PromptTemplateService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final PromptTemplateService promptTemplateService;
    private final ConversationMemoryFacade conversationMemoryFacade;

    public AiChatService(
            ChatClient chatClient,
            PromptTemplateService promptTemplateService,
            ConversationMemoryFacade conversationMemoryFacade) {
        this.chatClient = chatClient;
        this.promptTemplateService = promptTemplateService;
        this.conversationMemoryFacade = conversationMemoryFacade;
    }

    public ChatResponse chat(ChatRequest request) {
        var prepared = conversationMemoryFacade.prepare(
                request.conversationId(),
                request.sessionId(),
                request.message());

        List<HistoryMessage> history = prepared.recentHistory();
        if ((history == null || history.isEmpty()) && request.history() != null && !request.history().isEmpty()) {
            history = request.history();
        }

        String userMessage = conversationMemoryFacade.augmentUserMessage(
                request.message().trim(),
                prepared.memoryBlock());

        BuiltPrompt built = promptTemplateService.build(
                userMessage,
                request.resolvedStrategy(),
                history);

        String content = chatClient.prompt()
                .system(built.systemPrompt())
                .messages(built.contextMessages())
                .user(built.userPrompt())
                .call()
                .content();

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("The model returned an empty response");
        }
        String answer = content.trim();
        conversationMemoryFacade.completeTurn(
                prepared.conversationId(),
                request.message().trim(),
                answer);
        return new ChatResponse(answer, built.strategy(), prepared.conversationIdString());
    }

    public PromptPreviewResponse preview(ChatRequest request) {
        BuiltPrompt built = promptTemplateService.build(
                request.message(),
                request.resolvedStrategy(),
                request.history());

        List<PromptPreviewResponse.PreviewMessage> messages = new ArrayList<>();
        for (Message message : built.contextMessages()) {
            messages.add(new PromptPreviewResponse.PreviewMessage(roleOf(message), message.getText()));
        }
        messages.add(new PromptPreviewResponse.PreviewMessage("user", built.userPrompt()));

        return new PromptPreviewResponse(
                built.strategy(),
                built.version(),
                built.systemPrompt(),
                List.copyOf(messages),
                built.userPrompt());
    }

    private static String roleOf(Message message) {
        if (message instanceof UserMessage || message.getMessageType() == MessageType.USER) {
            return "user";
        }
        if (message instanceof AssistantMessage || message.getMessageType() == MessageType.ASSISTANT) {
            return "assistant";
        }
        return message.getMessageType().getValue();
    }
}
