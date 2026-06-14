package com.rabbiter.em.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhq.BgeSmallZhQuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 知识库服务 - RAG 的核心组件，负责向量检索
 * 
 * ===== 什么是向量检索？ =====
 * 传统搜索：关键词匹配（如 "退货" 匹配包含 "退货" 的文档）
 * 向量搜索：语义匹配（如 "怎么退钱" 也能匹配到 "退货流程" 文档）
 * 
 * ===== 工作原理 =====
 * 1. 应用启动时：
 *    - 加载 Markdown 文档
 *    - 把文档分割成小块（每块 800 字符）
 *    - 用 BGE 模型把每个文本块转为 512 维向量
 *    - 存储到向量数据库（内存/Chroma/Pinecone）
 * 
 * 2. 用户提问时：
 *    - 把用户问题也转为向量
 *    - 在向量数据库中查找最相似的向量
 *    - 返回对应的文本块
 * 
 * ===== 前端类比 =====
 * 就像前端的全文搜索，但不是用 indexOf 匹配字符串，
 * 而是用数学方法计算两个文本的"语义距离"。
 * 距离越近，说明语义越相关。
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    // BGE 模型要求的查询前缀（告诉模型这是用来检索的查询，不是普通文本）
    private static final String BGE_QUERY_PREFIX = "为这个句子生成表示以用于检索相关文章：";

    // 默认返回 5 个结果，相似度最低 0.5（满分 1.0）
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    /**
     * 向量存储 - 存储所有文本块的向量和原文
     * 
     * 类比：就像一个数据库表，但存储的是向量而不是普通字段
     * 表结构类似：
     * | 向量 (512维)        | 文本内容      | 元数据 (category, title) |
     * | [0.12, -0.34, ...] | "七天无理由..." | {category: "return_policy"} |
     */
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /** 文档加载器 - 从文件系统读取 Markdown 文件 */
    @Resource
    private KnowledgeDocLoader docLoader;

    /** 文档分块处理器 - 把长文档切成小块 */
    @Resource
    private DocumentChunkProcessor chunkProcessor;

    /**
     * BGE 中文嵌入模型 - 把文本转为 512 维向量
     * 
     * 类比：就像一个哈希函数，但相似的内容会产生相似的向量
     * "怎么退货" → [0.12, -0.34, 0.56, ...]
     * "如何退款" → [0.11, -0.33, 0.55, ...]  (向量很接近！)
     * "今天天气" → [0.89, 0.45, -0.12, ...]  (向量很远)
     */
    private final EmbeddingModel embeddingModel = new BgeSmallZhQuantizedEmbeddingModel();

    /** 内存中的知识文档映射（按分类存储） */
    private final Map<String, List<KnowledgeDoc>> knowledgeDocs = new LinkedHashMap<>();

    /** 索引片段计数器 */
    private final AtomicInteger segmentCount = new AtomicInteger(0);

    /**
     * 应用启动时执行 - 初始化知识库
     * 
     * 流程：
     * 1. 加载 Markdown 文件 → List<KnowledgeDoc>
     * 2. 分块 → List<TextSegment>
     * 3. 向量化 → List<Embedding>
     * 4. 存储到向量数据库
     */
    @PostConstruct
    public void init() {
        // 步骤1：从文件系统加载所有 Markdown 文档
        Map<String, List<KnowledgeDoc>> loadedDocs = docLoader.loadAllDocs();
        knowledgeDocs.putAll(loadedDocs);
        
        // 步骤2-4：分块、向量化、存储
        int indexed = indexDocuments();
        
        log.info("RAG 知识库初始化完成，共 {} 个文档分组，{} 篇源文档，{} 个索引片段（{}字符/块+{}重叠），存储后端: {}",
                knowledgeDocs.size(),
                knowledgeDocs.values().stream().mapToInt(List::size).sum(),
                indexed,
                800, 150,
                embeddingStore.getClass().getSimpleName());
    }

    /**
     * 索引文档 - 把文本转为向量并存储
     * 
     * 这个过程只在应用启动时执行一次
     */
    private int indexDocuments() {
        List<TextSegment> allSegments = new ArrayList<>();
        AtomicInteger chunkSeq = new AtomicInteger(0);

        // 遍历所有文档
        for (List<KnowledgeDoc> docs : knowledgeDocs.values()) {
            for (KnowledgeDoc doc : docs) {
                // 把文档分割成多个小块（每块 800 字符，重叠 150 字符）
                List<TextSegment> chunks = chunkProcessor.chunk(
                        doc.getContent(),
                        doc.getId(),
                        doc.getCategory(),
                        doc.getTitle()
                );

                // 给每个块添加元数据（类似给数据库记录加标签）
                for (TextSegment chunk : chunks) {
                    chunk.metadata().put("chunkSeq", String.valueOf(chunkSeq.incrementAndGet()));
                    chunk.metadata().put("sourceId", doc.getId());
                }

                allSegments.addAll(chunks);
            }
        }

        // 批量向量化：所有文本块 → 所有向量
        // 类比：就像批量处理图片压缩，一次处理多个效率更高
        List<Embedding> embeddings = embeddingModel.embedAll(allSegments).content();
        
        // 存储到向量数据库（向量 + 原文 + 元数据）
        embeddingStore.addAll(embeddings, allSegments);
        
        segmentCount.set(allSegments.size());
        return allSegments.size();
    }

    /**
     * 向量搜索 - 根据用户问题查找最相关的文档
     * 
     * @param query 用户问题，如"怎么退货？"
     * @param maxResults 最多返回几个结果
     * @return 按相似度排序的文档列表
     */
    public List<KnowledgeDoc> search(String query, int maxResults) {
        // 步骤1：把用户问题转为向量
        String prefixedQuery = BGE_QUERY_PREFIX + query;  // 添加前缀
        Embedding queryEmbedding = embeddingModel.embed(prefixedQuery).content();

        int effectiveMax = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;

        // 步骤2：构建搜索请求
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)  // 查询向量
                .maxResults(effectiveMax)        // 最多返回几个
                .minScore(MIN_SCORE)             // 最低相似度阈值
                .build();

        // 步骤3：执行搜索（余弦相似度计算）
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

        // 步骤4：把匹配结果转换为 KnowledgeDoc 对象
        return matches.stream()
                .map(match -> {
                    TextSegment segment = match.embedded();  // 获取原始文本块
                    KnowledgeDoc doc = new KnowledgeDoc(
                            segment.metadata().getString("category"),  // 从元数据读取分类
                            segment.metadata().getString("id"),        // 从元数据读取 ID
                            segment.metadata().getString("title"),     // 从元数据读取标题
                            segment.text()                             // 文本块内容
                    );
                    doc.setScore(match.score());  // 相似度分数（0.0 ~ 1.0）
                    doc.setChunkSeq(segment.metadata().getString("chunkSeq"));
                    doc.setSourceId(segment.metadata().getString("sourceId"));
                    return doc;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))  // 按相似度降序
                .collect(Collectors.toList());
    }

    /**
     * 按分类搜索 - 只在指定分类内查找
     * 
     * @param category 分类名称，如 "return_policy"
     * @param query 用户问题
     * @param maxResults 最多返回几个
     * @return 该分类内的相关文档
     */
    public List<KnowledgeDoc> searchByCategory(String category, String query, int maxResults) {
        // 先搜索更多结果（maxResults * 3），然后过滤出指定分类
        List<KnowledgeDoc> allResults = search(query, maxResults * 3);
        return allResults.stream()
                .filter(doc -> doc.getCategory().equals(category))  // 过滤分类
                .limit(maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS)  // 限制数量
                .collect(Collectors.toList());
    }

    /** 获取指定分类的所有文档（不搜索，直接返回） */
    public List<KnowledgeDoc> getAllDocsByCategory(String category) {
        return knowledgeDocs.getOrDefault(category, Collections.emptyList());
    }

    /** 获取所有分类的文档 */
    public Map<String, List<KnowledgeDoc>> getAllDocs() {
        return Collections.unmodifiableMap(knowledgeDocs);
    }

    /** 获取索引片段总数 */
    public int getSegmentCount() {
        return segmentCount.get();
    }

    /**
     * 知识文档对象 - 存储单个文档的信息
     * 
     * 类比：就像前端的接口定义
     * interface KnowledgeDoc {
     *   category: string;    // 分类
     *   id: string;          // 唯一标识
     *   title: string;       // 标题
     *   content: string;     // 内容
     *   score: number;       // 相似度分数
     *   chunkSeq: string;    // 块序号
     *   sourceId: string;    // 源文档 ID
     * }
     */
    public static class KnowledgeDoc {
        private String category;
        private String id;
        private String title;
        private String content;
        private double score;
        private String chunkSeq;
        private String sourceId;

        public KnowledgeDoc() {}

        public KnowledgeDoc(String category, String id, String title, String content) {
            this.category = category;
            this.id = id;
            this.title = title;
            this.content = content;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getChunkSeq() { return chunkSeq; }
        public void setChunkSeq(String chunkSeq) { this.chunkSeq = chunkSeq; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    }
}