package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.dto.rag.ChunkingConfig;
import com.learning.silveragepass.dto.rag.DocumentChunk;
import com.learning.silveragepass.service.rag.DocumentChunker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown文档分块器实现
 * Markdown Document Chunker Implementation
 * 
 * 支持：
 * 1. 按Markdown标题层级分块
 * 2. 大块二次分割（滑动窗口）
 * 3. 小块合并
 * 4. 关键词提取
 */
@Slf4j
@Service
public class MarkdownDocumentChunker implements DocumentChunker {
    
    // Markdown标题正则
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    // 中文关键词提取（简单实现：提取名词短语）
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,8}");
    
    @Override
    public List<DocumentChunk> chunkByMarkdownStructure(
            String markdown, 
            String documentId, 
            String category,
            ChunkingConfig config) {
        
        if (markdown == null || markdown.isBlank()) {
            return Collections.emptyList();
        }
        
        log.debug("开始分块文档: {}, 配置: maxChunkSize={}", documentId, config.getMaxChunkSize());
        
        // 1. 解析Markdown结构
        List<MarkdownSection> sections = parseMarkdownSections(markdown, config.getMaxHeadingLevel());
        
        if (sections.isEmpty()) {
            // 无标题结构，使用滑动窗口分块
            log.debug("文档无标题结构，使用滑动窗口分块");
            return chunkBySlidingWindow(markdown, documentId, category, 
                    config.getMaxChunkSize(), config.getOverlapSize());
        }
        
        // 2. 转换为分块
        List<DocumentChunk> chunks = new ArrayList<>();
        Map<Integer, String> parentChunkIds = new HashMap<>(); // 层级 -> 最近的父分块ID
        
        int chunkIndex = 0;
        for (MarkdownSection section : sections) {
            // 构建章节路径
            String sectionPath = buildSectionPath(section, sections);
            
            // 检查内容大小
            int tokenCount = DocumentChunk.estimateTokenCount(section.content);
            
            if (tokenCount > config.getMaxChunkSize()) {
                // 大块需要二次分割
                List<DocumentChunk> subChunks = splitLargeSection(
                        section, documentId, category, sectionPath, 
                        chunkIndex, config, parentChunkIds);
                chunks.addAll(subChunks);
                chunkIndex += subChunks.size();
            } else if (tokenCount < config.getMinChunkSize() && !chunks.isEmpty()) {
                // 小块尝试合并到上一个分块
                DocumentChunk lastChunk = chunks.get(chunks.size() - 1);
                int combinedTokens = lastChunk.getTokenCount() + tokenCount;
                
                if (combinedTokens <= config.getMaxChunkSize()) {
                    // 合并
                    lastChunk.setContent(lastChunk.getContent() + "\n\n" + section.content);
                    lastChunk.setTokenCount(combinedTokens);
                    lastChunk.setEndOffset(section.endOffset);
                    if (config.isExtractKeywords()) {
                        List<String> mergedKeywords = new ArrayList<>(lastChunk.getKeywords());
                        mergedKeywords.addAll(extractKeywords(section.content, config.getMaxKeywords()));
                        lastChunk.setKeywords(mergedKeywords.stream().distinct().limit(config.getMaxKeywords()).toList());
                    }
                    continue;
                }
            }
            
            // 创建分块
            String chunkId = UUID.randomUUID().toString();
            String parentChunkId = findParentChunkId(section.level, parentChunkIds);
            
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(chunkId)
                    .documentId(documentId)
                    .content(section.content)
                    .chunkIndex(chunkIndex++)
                    .sectionPath(sectionPath)
                    .headingLevel(section.level)
                    .parentChunkId(parentChunkId)
                    .keywords(config.isExtractKeywords() ? 
                            extractKeywords(section.content, config.getMaxKeywords()) : null)
                    .startOffset(section.startOffset)
                    .endOffset(section.endOffset)
                    .tokenCount(tokenCount)
                    .category(category)
                    .build();
            
            chunks.add(chunk);
            parentChunkIds.put(section.level, chunkId);
        }
        
        log.info("文档 {} 分块完成，共 {} 个分块", documentId, chunks.size());
        return chunks;
    }
    
    @Override
    public List<DocumentChunk> chunkBySlidingWindow(
            String text, 
            String documentId,
            String category,
            int chunkSize, 
            int overlap) {
        
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        int chunkIndex = 0;
        int startOffset = 0;
        
        for (String paragraph : paragraphs) {
            int paragraphTokens = DocumentChunk.estimateTokenCount(paragraph);
            
            if (currentTokens + paragraphTokens > chunkSize && currentTokens > 0) {
                // 保存当前分块
                chunks.add(createWindowChunk(
                        currentChunk.toString(), documentId, category,
                        chunkIndex++, startOffset, currentTokens));
                
                // 重叠处理：保留部分内容
                String overlapContent = getOverlapContent(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapContent);
                currentTokens = DocumentChunk.estimateTokenCount(overlapContent);
                startOffset = startOffset + currentChunk.length() - overlapContent.length();
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            currentTokens += paragraphTokens;
        }
        
        // 保存最后一个分块
        if (currentChunk.length() > 0) {
            chunks.add(createWindowChunk(
                    currentChunk.toString(), documentId, category,
                    chunkIndex, startOffset, currentTokens));
        }
        
        return chunks;
    }
    
    /**
     * 解析Markdown章节结构
     */
    private List<MarkdownSection> parseMarkdownSections(String markdown, int maxLevel) {
        List<MarkdownSection> sections = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(markdown);
        
        int lastEnd = 0;
        String lastHeading = null;
        int lastLevel = 0;
        int lastStart = 0;
        
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String heading = matcher.group(2).trim();
            int start = matcher.start();
            
            // 保存上一个章节
            if (lastHeading != null && level <= maxLevel) {
                String content = markdown.substring(lastEnd, start).trim();
                if (!content.isEmpty()) {
                    sections.add(new MarkdownSection(
                            lastHeading, lastLevel, content, lastStart, start));
                }
            }
            
            if (level <= maxLevel) {
                lastHeading = heading;
                lastLevel = level;
                lastStart = start;
                lastEnd = matcher.end();
            }
        }
        
        // 保存最后一个章节
        if (lastHeading != null) {
            String content = markdown.substring(lastEnd).trim();
            if (!content.isEmpty()) {
                sections.add(new MarkdownSection(
                        lastHeading, lastLevel, content, lastStart, markdown.length()));
            }
        }
        
        // 如果没有找到任何标题，将整个文档作为一个章节
        if (sections.isEmpty() && !markdown.isBlank()) {
            sections.add(new MarkdownSection("", 0, markdown.trim(), 0, markdown.length()));
        }
        
        return sections;
    }
    
    /**
     * 构建章节路径
     */
    private String buildSectionPath(MarkdownSection section, List<MarkdownSection> allSections) {
        if (section.heading.isEmpty()) {
            return "";
        }
        
        List<String> pathParts = new ArrayList<>();
        int currentIndex = allSections.indexOf(section);
        int targetLevel = section.level;
        
        // 向前查找父级标题
        for (int i = currentIndex; i >= 0; i--) {
            MarkdownSection s = allSections.get(i);
            if (s.level < targetLevel || i == currentIndex) {
                pathParts.add(0, s.heading);
                targetLevel = s.level;
                if (s.level == 1) break;
            }
        }
        
        return String.join("/", pathParts);
    }
    
    /**
     * 分割大章节
     */
    private List<DocumentChunk> splitLargeSection(
            MarkdownSection section,
            String documentId,
            String category,
            String sectionPath,
            int startIndex,
            ChunkingConfig config,
            Map<Integer, String> parentChunkIds) {
        
        // 使用滑动窗口分割
        List<DocumentChunk> subChunks = chunkBySlidingWindow(
                section.content, documentId, category,
                config.getMaxChunkSize(), config.getOverlapSize());
        
        // 更新分块信息
        String parentId = findParentChunkId(section.level, parentChunkIds);
        for (int i = 0; i < subChunks.size(); i++) {
            DocumentChunk chunk = subChunks.get(i);
            chunk.setChunkIndex(startIndex + i);
            chunk.setSectionPath(sectionPath + (i > 0 ? " (续" + i + ")" : ""));
            chunk.setHeadingLevel(section.level);
            chunk.setParentChunkId(parentId);
            chunk.setStartOffset(section.startOffset + chunk.getStartOffset());
        }
        
        return subChunks;
    }
    
    /**
     * 查找父分块ID
     */
    private String findParentChunkId(int currentLevel, Map<Integer, String> parentChunkIds) {
        for (int level = currentLevel - 1; level >= 1; level--) {
            if (parentChunkIds.containsKey(level)) {
                return parentChunkIds.get(level);
            }
        }
        return null;
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content, int maxKeywords) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 简单实现：提取中文词组，按出现频率排序
        Map<String, Integer> wordFreq = new HashMap<>();
        Matcher matcher = KEYWORD_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String word = matcher.group();
            // 过滤常见停用词
            if (!isStopWord(word)) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }
        
        return wordFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(maxKeywords)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "的", "是", "在", "有", "和", "与", "或", "等", "了", "着",
                "这", "那", "个", "些", "可以", "应该", "需要", "进行", "通过"
        );
        return stopWords.contains(word);
    }
    
    /**
     * 获取重叠内容
     */
    private String getOverlapContent(String content, int overlapTokens) {
        String[] sentences = content.split("[。！？\n]");
        StringBuilder overlap = new StringBuilder();
        int tokens = 0;
        
        for (int i = sentences.length - 1; i >= 0 && tokens < overlapTokens; i--) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                overlap.insert(0, sentence + "。");
                tokens += DocumentChunk.estimateTokenCount(sentence);
            }
        }
        
        return overlap.toString();
    }
    
    /**
     * 创建滑动窗口分块
     */
    private DocumentChunk createWindowChunk(
            String content, String documentId, String category,
            int index, int startOffset, int tokenCount) {
        
        return DocumentChunk.builder()
                .id(UUID.randomUUID().toString())
                .documentId(documentId)
                .content(content)
                .chunkIndex(index)
                .startOffset(startOffset)
                .endOffset(startOffset + content.length())
                .tokenCount(tokenCount)
                .category(category)
                .keywords(extractKeywords(content, 10))
                .build();
    }
    
    /**
     * Markdown章节内部类
     */
    private static class MarkdownSection {
        final String heading;
        final int level;
        final String content;
        final int startOffset;
        final int endOffset;
        
        MarkdownSection(String heading, int level, String content, int startOffset, int endOffset) {
            this.heading = heading;
            this.level = level;
            this.content = content;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
