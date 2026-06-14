package com.rabbiter.em.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreFactory.class);

    @Value("${ai.rag.vector-store:memory}")
    private String vectorStoreType;

    @Value("${ai.rag.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${ai.rag.pinecone.api-key:}")
    private String pineconeApiKey;

    @Value("${ai.rag.pinecone.environment:}")
    private String pineconeEnvironment;

    @Value("${ai.rag.pinecone.index-name:}")
    private String pineconeIndexName;

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> embeddingStore() {
        switch (vectorStoreType.toLowerCase()) {
            case "chroma":
                return createChromaStore();
            case "pinecone":
                return createPineconeStore();
            case "memory":
            default:
                return createMemoryStore();
        }
    }

    private EmbeddingStore<TextSegment> createMemoryStore() {
        log.info("使用 InMemoryEmbeddingStore（内存向量存储）");
        return new InMemoryEmbeddingStore<>();
    }

    private EmbeddingStore<TextSegment> createChromaStore() {
        try {
            log.info("使用 Chroma 向量数据库: {}", chromaUrl);
            Class<?> clazz = Class.forName("dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore");
            Object builder = clazz.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("baseUrl", String.class).invoke(builder, chromaUrl);
            builder = builder.getClass().getMethod("collectionName", String.class).invoke(builder, "mall_knowledge");
            builder = builder.getClass().getMethod("dimension", Integer.class).invoke(builder, 512);
            @SuppressWarnings("unchecked")
            EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) builder.getClass().getMethod("build").invoke(builder);
            return store;
        } catch (Exception e) {
            log.warn("Chroma 初始化失败 ({}), 降级到内存存储", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }

    private EmbeddingStore<TextSegment> createPineconeStore() {
        if (isEmpty(pineconeApiKey) || isEmpty(pineconeEnvironment) || isEmpty(pineconeIndexName)) {
            log.warn("Pinecone 配置不完整, 降级到内存存储");
            return new InMemoryEmbeddingStore<>();
        }
        try {
            log.info("使用 Pinecone 向量数据库: {}/{}", pineconeEnvironment, pineconeIndexName);
            Class<?> clazz = Class.forName("dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore");
            Object builder = clazz.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("apiKey", String.class).invoke(builder, pineconeApiKey);
            builder = builder.getClass().getMethod("environment", String.class).invoke(builder, pineconeEnvironment);
            builder = builder.getClass().getMethod("index", String.class).invoke(builder, pineconeIndexName);
            builder = builder.getClass().getMethod("dimension", Integer.class).invoke(builder, 512);
            @SuppressWarnings("unchecked")
            EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) builder.getClass().getMethod("build").invoke(builder);
            return store;
        } catch (Exception e) {
            log.warn("Pinecone 初始化失败 ({}), 降级到内存存储", e.getMessage());
            return new InMemoryEmbeddingStore<>();
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}