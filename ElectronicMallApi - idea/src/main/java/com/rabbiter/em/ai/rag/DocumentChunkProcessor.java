package com.rabbiter.em.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DocumentChunkProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkProcessor.class);

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 150;

    private final DocumentByCharacterSplitter splitter;

    public DocumentChunkProcessor() {
        this.splitter = new DocumentByCharacterSplitter(CHUNK_SIZE, CHUNK_OVERLAP);
        log.info("文档分块处理器初始化: 每块{}字符, 重叠{}字符", CHUNK_SIZE, CHUNK_OVERLAP);
    }

    public List<TextSegment> chunk(String content, String segmentId, String category, String title) {
        Document document = Document.from(content);
        document.metadata().put("id", segmentId);
        document.metadata().put("category", category);
        document.metadata().put("title", title);

        List<TextSegment> segments = splitter.split(document);
        log.debug("文档 [{}] 被分割为 {} 个片段", title, segments.size());
        return segments;
    }

    public List<TextSegment> chunk(String content, Map<String, String> metadata) {
        Document document = Document.from(content);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            document.metadata().put(entry.getKey(), entry.getValue());
        }

        List<TextSegment> segments = splitter.split(document);
        return segments;
    }
}