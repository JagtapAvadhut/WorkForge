package com.avadhoot.workforgeai.ai;

import com.avadhoot.workforgeai.ai.dto.ChatRequest;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.dto.PromptPreviewResponse;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;
import com.avadhoot.workforgeai.ai.prompt.PromptTemplateService;
import com.avadhoot.workforgeai.ai.prompt.PromptVersion;
import com.avadhoot.workforgeai.ai.service.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private PromptTemplateService promptTemplateService;
    private AiChatService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        promptTemplateService = new PromptTemplateService(10);
        ConversationMemoryFacade memoryFacade = mock(ConversationMemoryFacade.class);
        when(memoryFacade.prepare(any(), any(), anyString())).thenReturn(
                new ConversationMemoryFacade.PreparedContext(
                        java.util.UUID.randomUUID(), "default", List.of(), List.of(), ""));
        when(memoryFacade.augmentUserMessage(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doNothing().when(memoryFacade).completeTurn(any(), anyString(), anyString());
        service = new AiChatService(chatClient, promptTemplateService, memoryFacade);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void chat_returnsModelContent_withDefaultStrategy() {
        when(callSpec.content()).thenReturn("  An issue is a tracked work item.  ");

        ChatResponse response = service.chat(new ChatRequest("What is an issue?"));

        assertThat(response.response()).isEqualTo("An issue is a tracked work item.");
        assertThat(response.strategy()).isEqualTo(PromptStrategy.GENERAL);
        verify(requestSpec).system(anyString());
        verify(requestSpec).user(anyString());
    }

    @ParameterizedTest
    @EnumSource(PromptStrategy.class)
    void chat_acceptsEachStrategy(PromptStrategy strategy) {
        when(callSpec.content()).thenReturn("ok");

        ChatResponse response = service.chat(new ChatRequest("Explain sprint", strategy, List.of()));

        assertThat(response.strategy()).isEqualTo(strategy);
        assertThat(response.response()).isEqualTo("ok");
    }

    @Test
    void chat_rejectsEmptyModelOutput() {
        when(callSpec.content()).thenReturn("   ");

        assertThatThrownBy(() -> service.chat(new ChatRequest("Hello")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void promptPreview_showsRenderedMessagesWithoutCallingModel() {
        PromptPreviewResponse preview = service.preview(
                new ChatRequest("Explain sprint", PromptStrategy.DOMAIN_EXPERT, List.of()));

        assertThat(preview.strategy()).isEqualTo(PromptStrategy.DOMAIN_EXPERT);
        assertThat(preview.version()).isEqualTo(PromptVersion.DOMAIN_EXPERT_V1);
        assertThat(preview.systemPrompt()).contains("enterprise project-management");
        assertThat(preview.systemPrompt()).contains("Do not invent live WorkForge data");
        assertThat(preview.userPrompt()).contains("Explain sprint");
        assertThat(preview.messages()).isNotEmpty();
        assertThat(preview.messages().getLast().role()).isEqualTo("user");
    }

    @Test
    void fewShot_includesExamplePairsBeforeUserQuestion() {
        PromptPreviewResponse preview = service.preview(
                new ChatRequest("What is a board?", PromptStrategy.FEW_SHOT, List.of()));

        assertThat(preview.messages()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(preview.messages().get(0).content()).contains("sprint");
        assertThat(preview.messages().get(1).role()).isEqualTo("assistant");
        assertThat(preview.messages().get(2).content()).contains("backlog");
    }

    @Test
    void history_isIncludedAndTrimmed() {
        PromptTemplateService limited = new PromptTemplateService(2);
        List<HistoryMessage> history = List.of(
                new HistoryMessage("user", "first"),
                new HistoryMessage("assistant", "second"),
                new HistoryMessage("user", "third"),
                new HistoryMessage("assistant", "fourth")
        );

        var built = limited.build("latest", PromptStrategy.GENERAL, history);

        assertThat(built.contextMessages()).hasSize(2);
        assertThat(built.contextMessages().get(0).getText()).isEqualTo("third");
        assertThat(built.contextMessages().get(1).getText()).isEqualTo("fourth");
    }

    @Test
    void history_rejectsInvalidRole() {
        assertThatThrownBy(() -> promptTemplateService.build(
                "hi",
                PromptStrategy.GENERAL,
                List.of(new HistoryMessage("system", "nope"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid history role");
    }

    @Test
    void invalidStrategy_isRejected() {
        assertThatThrownBy(() -> PromptStrategy.from("NOT_A_STRATEGY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid strategy");
    }

    @Test
    void templateVariables_areRendered() {
        String rendered = promptTemplateService.renderTemplate(
                "You are a <role>. Explain <topic> for a <audience>. Use <style> style.",
                Map.of(
                        "role", "domain expert",
                        "topic", "sprint",
                        "audience", "engineers",
                        "style", "concise"
                ));

        assertThat(rendered).contains("domain expert");
        assertThat(rendered).contains("sprint");
        assertThat(rendered).contains("engineers");
        assertThat(rendered).contains("concise");
        assertThat(rendered).doesNotContain("<role>");
    }

    @Test
    void structured_systemPrompt_requestsJsonShape() {
        var built = promptTemplateService.build("Explain epic", PromptStrategy.STRUCTURED, List.of());

        assertThat(built.systemPrompt()).contains("\"summary\"");
        assertThat(built.systemPrompt()).contains("\"keyPoints\"");
        assertThat(built.version()).isEqualTo(PromptVersion.STRUCTURED_V1);
    }

    @Test
    void chat_passesHistoryMessagesToClient() {
        when(callSpec.content()).thenReturn("follow-up answer");
        List<HistoryMessage> history = List.of(
                new HistoryMessage("user", "What is a sprint?"),
                new HistoryMessage("assistant", "A time-boxed iteration.")
        );

        service.chat(new ChatRequest("How does it relate to a board?", PromptStrategy.GENERAL, history));

        verify(requestSpec).messages(any(List.class));
    }
}
