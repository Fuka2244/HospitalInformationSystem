package com.hospitalinfo.hospitalinformationsystem.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI配置类
 * 通义千问兼容OpenAI协议，使用OpenAiChatModel/OpenAiEmbeddingModel调用
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ai.qwen")
public class AiConfig {

    private String apiKey;
    private String modelName;
    private String baseUrl;

    // Embedding配置
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /** 标记向量索引是否已从缓存加载（无需重建） */
    private final AtomicBoolean embeddingStoreLoadedFromCache = new AtomicBoolean(false);

    public static class EmbeddingConfig {
        private String apiKey;
        private String modelName;
        private String baseUrl;
        private String persistPath = "medicine-embedding-store.json";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getPersistPath() { return persistPath; }
        public void setPersistPath(String persistPath) { this.persistPath = persistPath; }
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxTokens(2048)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(embedding.getApiKey())
                .modelName(embedding.getModelName())
                .baseUrl(embedding.getBaseUrl())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 创建药品向量存储，优先从磁盘缓存文件加载
     */
    @Bean
    public EmbeddingStore<TextSegment> medicineEmbeddingStore() {
        Path cacheFile = Paths.get(embedding.getPersistPath());
        if (Files.exists(cacheFile)) {
            try {
                long fileSize = Files.size(cacheFile);
                log.info("正在加载药品向量缓存文件 (大小: {}MB, 路径: {})，请稍候...",
                        String.format("%.1f", fileSize / 1024.0 / 1024.0), cacheFile.toAbsolutePath());
                long start = System.currentTimeMillis();
                InMemoryEmbeddingStore<TextSegment> loaded =
                        InMemoryEmbeddingStore.fromFile(cacheFile.toString());
                long elapsed = System.currentTimeMillis() - start;
                embeddingStoreLoadedFromCache.set(true);
                log.info("药品向量缓存加载完成，耗时{}ms", elapsed);
                return loaded;
            } catch (Exception e) {
                log.warn("加载向量缓存失败，将使用空store重新构建: {}", e.getMessage());
            }
        }
        log.info("未找到向量缓存文件，将重新构建索引");
        return new InMemoryEmbeddingStore<>();
    }

    public boolean isEmbeddingStoreLoadedFromCache() {
        return embeddingStoreLoadedFromCache.get();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public EmbeddingConfig getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingConfig embedding) { this.embedding = embedding; }
}
