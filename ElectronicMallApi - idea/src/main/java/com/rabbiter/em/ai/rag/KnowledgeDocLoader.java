package com.rabbiter.em.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KnowledgeDocLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeDocLoader.class);

    private static final String KNOWLEDGE_BASE_PATH = "classpath:knowledge-base/**/*.md";

    public Map<String, List<KnowledgeBaseService.KnowledgeDoc>> loadAllDocs() {
        Map<String, List<KnowledgeBaseService.KnowledgeDoc>> docs = new LinkedHashMap<>();

        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(KNOWLEDGE_BASE_PATH);

            for (Resource resource : resources) {
                KnowledgeBaseService.KnowledgeDoc doc = parseDoc(resource);
                if (doc != null) {
                    docs.computeIfAbsent(doc.getCategory(), k -> new ArrayList<>()).add(doc);
                }
            }

            int totalDocs = docs.values().stream().mapToInt(List::size).sum();
            log.info("从文件系统加载了 {} 个分类，共 {} 篇知识文档", docs.size(), totalDocs);
        } catch (Exception e) {
            log.error("加载知识库文档失败", e);
        }

        return docs;
    }

    private KnowledgeBaseService.KnowledgeDoc parseDoc(Resource resource) {
        String filename = resource.getFilename();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String content = reader.lines().collect(Collectors.joining("\n"));

            if (!content.startsWith("---")) {
                log.warn("文档 {} 缺少 front matter，跳过", filename);
                return null;
            }

            int secondDelim = content.indexOf("---", 3);
            if (secondDelim == -1) {
                log.warn("文档 {} front matter 格式不完整", filename);
                return null;
            }

            String frontMatter = content.substring(3, secondDelim).trim();
            String body = content.substring(secondDelim + 3).trim();

            if (body.isEmpty()) {
                log.warn("文档 {} 内容为空，跳过", filename);
                return null;
            }

            String category = extractField(frontMatter, "category");
            String id = extractField(frontMatter, "id");
            String title = extractField(frontMatter, "title");

            if (category == null || id == null || title == null) {
                log.warn("文档 {} front matter 缺少必要字段 (category/id/title)", filename);
                return null;
            }

            return new KnowledgeBaseService.KnowledgeDoc(category, id, title, body);

        } catch (Exception e) {
            log.error("解析文档 {} 失败: {}", filename, e.getMessage());
            return null;
        }
    }

    private String extractField(String frontMatter, String field) {
        for (String line : frontMatter.split("\n")) {
            line = line.trim();
            if (line.startsWith(field + ":")) {
                String value = line.substring(field.length() + 1).trim();
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }
}