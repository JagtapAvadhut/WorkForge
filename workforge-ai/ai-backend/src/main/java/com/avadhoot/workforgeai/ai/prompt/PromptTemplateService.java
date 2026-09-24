package com.avadhoot.workforgeai.ai.prompt;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds strategy-specific prompts using Spring AI PromptTemplate rendering.
 */
@Service
public class PromptTemplateService {

    private final int maxHistoryMessages;
    private final StTemplateRenderer renderer = StTemplateRenderer.builder()
            .startDelimiterToken('<')
            .endDelimiterToken('>')
            .build();

    public PromptTemplateService(
            @Value("${workforge.ai.chat.max-history-messages:10}") int maxHistoryMessages) {
        this.maxHistoryMessages = Math.max(0, maxHistoryMessages);
    }

    public BuiltPrompt build(String message, PromptStrategy strategy, List<HistoryMessage> history) {
        PromptStrategy resolved = strategy == null ? PromptStrategy.GENERAL : strategy;
        PromptVersion version = PromptVersion.forStrategy(resolved);

        String systemPrompt = renderSystem(resolved);
        String userPrompt = renderUser(message, resolved);
        List<Message> context = new ArrayList<>();

        if (resolved == PromptStrategy.FEW_SHOT) {
            for (FewShotExample example : WorkforgePromptTemplates.WORKFORGE_FEW_SHOT) {
                context.add(new UserMessage(example.user()));
                context.add(new AssistantMessage(example.assistant()));
            }
        }

        context.addAll(toMessages(trimHistory(history)));

        return new BuiltPrompt(resolved, version, systemPrompt, List.copyOf(context), userPrompt);
    }

    public String renderTemplate(String template, Map<String, Object> variables) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .template(template)
                .build()
                .render(variables);
    }

    private String renderSystem(PromptStrategy strategy) {
        String template = switch (strategy) {
            case GENERAL -> WorkforgePromptTemplates.GENERAL_SYSTEM;
            case DOMAIN_EXPERT -> WorkforgePromptTemplates.DOMAIN_EXPERT_SYSTEM;
            case CONCISE -> WorkforgePromptTemplates.CONCISE_SYSTEM;
            case DETAILED -> WorkforgePromptTemplates.DETAILED_SYSTEM;
            case FEW_SHOT -> WorkforgePromptTemplates.FEW_SHOT_SYSTEM;
            case STRUCTURED -> WorkforgePromptTemplates.STRUCTURED_SYSTEM;
        };
        return PromptTemplate.builder()
                .renderer(renderer)
                .template(template)
                .build()
                .render(Map.of("constraints", WorkforgePromptTemplates.SHARED_CONSTRAINTS.trim()))
                .trim();
    }

    private String renderUser(String message, PromptStrategy strategy) {
        Map<String, Object> vars = Map.of(
                "role", roleFor(strategy),
                "topic", topicFrom(message),
                "audience", audienceFor(strategy),
                "style", styleFor(strategy),
                "question", message.trim()
        );
        return renderTemplate(WorkforgePromptTemplates.USER_TEMPLATE, vars).trim();
    }

    private static String roleFor(PromptStrategy strategy) {
        return switch (strategy) {
            case GENERAL -> "helpful WorkForge assistant";
            case DOMAIN_EXPERT -> "enterprise project-management domain expert";
            case CONCISE -> "brief WorkForge explainer";
            case DETAILED -> "thorough WorkForge instructor";
            case FEW_SHOT -> "WorkForge tutor using worked examples";
            case STRUCTURED -> "structured WorkForge concept summarizer";
        };
    }

    private static String audienceFor(PromptStrategy strategy) {
        return switch (strategy) {
            case DOMAIN_EXPERT, DETAILED -> "software delivery professionals";
            case CONCISE -> "busy practitioners";
            case STRUCTURED -> "API/integration consumers";
            default -> "WorkForge learners";
        };
    }

    private static String styleFor(PromptStrategy strategy) {
        return switch (strategy) {
            case CONCISE -> "concise";
            case DETAILED -> "detailed step-by-step";
            case STRUCTURED -> "structured JSON";
            case FEW_SHOT -> "example-driven";
            case DOMAIN_EXPERT -> "expert and precise";
            case GENERAL -> "clear and friendly";
        };
    }

    private static String topicFrom(String message) {
        String trimmed = message.trim();
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 117) + "...";
    }

    private List<HistoryMessage> trimHistory(List<HistoryMessage> history) {
        if (history == null || history.isEmpty() || maxHistoryMessages == 0) {
            return List.of();
        }
        int size = history.size();
        if (size <= maxHistoryMessages) {
            return history;
        }
        return history.subList(size - maxHistoryMessages, size);
    }

    private static List<Message> toMessages(List<HistoryMessage> history) {
        List<Message> messages = new ArrayList<>();
        for (HistoryMessage item : history) {
            if (item == null || item.content() == null || item.content().isBlank()) {
                continue;
            }
            String role = item.role() == null ? "" : item.role().trim().toLowerCase(Locale.ROOT);
            String content = item.content().trim();
            if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            } else if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else {
                throw new IllegalArgumentException(
                        "Invalid history role '" + item.role() + "'. Allowed: user, assistant");
            }
        }
        return messages;
    }
}
