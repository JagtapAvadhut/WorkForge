package com.avadhoot.workforgeai.ai.rag;

import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Builds the grounded RAG user prompt from retrieved sources.
 */
@Component
public class RagPromptBuilder {

    private final StTemplateRenderer renderer = StTemplateRenderer.builder()
            .startDelimiterToken('<')
            .endDelimiterToken('>')
            .build();

    public String systemPrompt() {
        return RagPromptTemplates.SYSTEM.trim();
    }

    public String userPrompt(String question, List<RagSource> sources) {
        String context = formatContext(sources);
        return PromptTemplate.builder()
                .renderer(renderer)
                .template(RagPromptTemplates.USER_TEMPLATE)
                .build()
                .render(Map.of(
                        "context", context,
                        "question", question.trim()
                ))
                .trim();
    }

    public String formatContext(List<RagSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "(no context)";
        }
        return IntStream.range(0, sources.size())
                .mapToObj(i -> {
                    RagSource source = sources.get(i);
                    String meta = source.metadata() == null || source.metadata().isEmpty()
                            ? ""
                            : " metadata=" + source.metadata();
                    return "[%d] (similarity=%.4f%s)%n%s".formatted(
                            i + 1,
                            source.similarity(),
                            meta,
                            source.content());
                })
                .collect(Collectors.joining("\n\n"));
    }
}
