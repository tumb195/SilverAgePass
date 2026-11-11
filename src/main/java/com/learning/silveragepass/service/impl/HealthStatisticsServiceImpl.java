package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.health.*;
import com.learning.silveragepass.entity.*;
import com.learning.silveragepass.repository.*;
import com.learning.silveragepass.service.HealthStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HealthStatisticsServiceImpl implements HealthStatisticsService {
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final HealthDiaryEntryRepository diaryRepository;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public HealthStatisticsServiceImpl(EmotionAnalysisRepository emotionAnalysisRepository, HealthDiaryEntryRepository diaryRepository) {
        this.emotionAnalysisRepository = emotionAnalysisRepository;
        this.diaryRepository = diaryRepository;
    }

    @Override
    @Cacheable(value = "emotionTrends", key = "#userId + '_' + #days")
    @Transactional(readOnly = true)
    public EmotionTrendData getEmotionTrends(Long userId, int days) {
        validateUserId(userId); validateDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<HealthDiaryEntry> entries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        Map<String, EmotionAnalysis> analysisMap = getEmotionAnalysesForEntries(entries);
        List<EmotionTrendData.EmotionDataPoint> dataPoints = new ArrayList<>();
        double totalEmotionScore = 0.0; int count = 0;
        for (HealthDiaryEntry entry : entries) {
            EmotionAnalysis analysis = analysisMap.get(entry.getEntryId());
            EmotionTrendData.EmotionDataPoint dp = new EmotionTrendData.EmotionDataPoint();
            dp.setDate(entry.getEntryDate()); dp.setEmotionScore((double) entry.getEmotionScore());
            if (analysis != null) { dp.setPrimaryEmotion(analysis.getPrimaryEmotion().name()); dp.setIntensityScore(analysis.getIntensityScore()); }
            dataPoints.add(dp); totalEmotionScore += entry.getEmotionScore(); count++;
        }
        EmotionTrendData result = new EmotionTrendData();
        result.setUserId(userId); result.setDays(days); result.setDataPoints(dataPoints);
        result.setAverageEmotionScore(count > 0 ? totalEmotionScore / count : null);
        result.setTrendDirection(calculateTrendDirection(dataPoints));
        return result;
    }

    @Override
    @Cacheable(value = "emotionDistribution", key = "#userId + '_' + #days")
    @Transactional(readOnly = true)
    public EmotionDistribution getEmotionDistribution(Long userId, int days) {
        validateUserId(userId); validateDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<HealthDiaryEntry> entries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        Map<String, EmotionAnalysis> analysisMap = getEmotionAnalysesForEntries(entries);
        Map<String, Integer> counts = new HashMap<>();
        int positiveCount = 0, negativeCount = 0, totalAnalyses = 0;
        for (EmotionAnalysis analysis : analysisMap.values()) {
            String emotionType = analysis.getPrimaryEmotion().name();
            counts.put(emotionType, counts.getOrDefault(emotionType, 0) + 1);
            if (analysis.getIsPositive()) positiveCount++; else negativeCount++;
            totalAnalyses++;
        }
        Map<String, Double> distribution = new HashMap<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            distribution.put(e.getKey(), totalAnalyses > 0 ? (e.getValue() * 100.0) / totalAnalyses : 0.0);
        }
        EmotionDistribution result = new EmotionDistribution();
        result.setUserId(userId); result.setDays(days); result.setDistribution(distribution);
        result.setCounts(counts); result.setTotalAnalyses(totalAnalyses);
        result.setDominantEmotion(counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null));
        result.setPositivePercentage(totalAnalyses > 0 ? (positiveCount * 100.0) / totalAnalyses : null);
        result.setNegativePercentage(totalAnalyses > 0 ? (negativeCount * 100.0) / totalAnalyses : null);
        return result;
    }

    @Override
    @Cacheable(value = "healthSummary", key = "#userId + '_' + #days")
    @Transactional(readOnly = true)
    public HealthMetricsSummary getHealthSummary(Long userId, int days) {
        validateUserId(userId); validateDays(days);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<HealthDiaryEntry> entries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        Map<String, EmotionAnalysis> analysisMap = getEmotionAnalysesForEntries(entries);
        HealthMetricsSummary result = new HealthMetricsSummary();
        result.setUserId(userId); result.setDays(days);
        result.setAverageEmotionScore(calcAvg(entries, HealthDiaryEntry::getEmotionScore));
        result.setAverageSleepQuality(calcAvg(entries, HealthDiaryEntry::getSleepQuality));
        result.setAverageStressLevel(calcAvg(entries, HealthDiaryEntry::getStressLevel));
        result.setEmotionTrend(calcTrend(entries, HealthDiaryEntry::getEmotionScore, true));
        result.setSleepTrend(calcTrend(entries, HealthDiaryEntry::getSleepQuality, true));
        result.setStressTrend(calcTrend(entries, HealthDiaryEntry::getStressLevel, false));
        result.setTotalEntries(entries.size());
        result.setHighRiskDays((int) analysisMap.values().stream().filter(a -> a.getRiskLevel() >= 2).count());
        result.setLatestRiskLevel(analysisMap.values().stream().max(Comparator.comparing(EmotionAnalysis::getAnalyzedAt)).map(EmotionAnalysis::getRiskLevel).orElse(null));
        result.setOverallHealthScore(calcOverallScore(result.getAverageEmotionScore(), result.getAverageSleepQuality(), result.getAverageStressLevel(), result.getHighRiskDays(), entries.size()));
        return result;
    }

    @Override @Transactional(readOnly = true)
    public List<String> generatePersonalizedSuggestions(Long userId) {
        validateUserId(userId);
        List<String> suggestions = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        List<HealthDiaryEntry> entries = diaryRepository.findByUserIdAndDateRange(userId, endDate.minusDays(29), endDate);
        if (entries.size() < 7) { suggestions.add("继续记录您的健康日记"); return suggestions; }
        Double avgEmotion = calcAvg(entries, HealthDiaryEntry::getEmotionScore);
        if (avgEmotion != null && avgEmotion < 5.0) suggestions.add("您最近的情绪评分较低，建议多与家人朋友交流");
        else if (avgEmotion != null && avgEmotion >= 7.0) suggestions.add("您的情绪状态很好，继续保持");
        Double avgSleep = calcAvg(entries, HealthDiaryEntry::getSleepQuality);
        if (avgSleep != null && avgSleep < 3.0) suggestions.add("您的睡眠质量需要改善");
        Double avgStress = calcAvg(entries, HealthDiaryEntry::getStressLevel);
        if (avgStress != null && avgStress >= 4.0) suggestions.add("您的压力水平较高，建议放松");
        if (suggestions.isEmpty()) { suggestions.add("您的整体健康状况良好"); }
        return suggestions.size() > 5 ? suggestions.subList(0, 5) : suggestions;
    }

    @Override @CacheEvict(value = {"emotionTrends", "emotionDistribution", "healthSummary"}, allEntries = true)
    public void clearStatisticsCache(Long userId) { log.info("Clearing cache for user {}", userId); }

    private void validateUserId(Long userId) { if (userId == null) throw new IllegalArgumentException("User ID cannot be null"); }
    private void validateDays(int days) { if (days != 7 && days != 30 && days != 90) throw new IllegalArgumentException("Days must be 7, 30, or 90"); }
    
    private Map<String, EmotionAnalysis> getEmotionAnalysesForEntries(List<HealthDiaryEntry> entries) {
        Map<String, EmotionAnalysis> map = new HashMap<>();
        for (HealthDiaryEntry e : entries) emotionAnalysisRepository.findByDiaryEntryId(e.getEntryId()).ifPresent(a -> map.put(e.getEntryId(), a));
        return map;
    }
    
    private String calculateTrendDirection(List<EmotionTrendData.EmotionDataPoint> dp) {
        if (dp.size() < 3) return "stable";
        int ts = dp.size() / 3;
        double f = dp.subList(0, ts).stream().mapToDouble(EmotionTrendData.EmotionDataPoint::getEmotionScore).average().orElse(0);
        double l = dp.subList(dp.size() - ts, dp.size()).stream().mapToDouble(EmotionTrendData.EmotionDataPoint::getEmotionScore).average().orElse(0);
        return (l - f) > 1.0 ? "improving" : (l - f) < -1.0 ? "declining" : "stable";
    }
    
    private Double calcAvg(List<HealthDiaryEntry> e, java.util.function.ToIntFunction<HealthDiaryEntry> ex) { return e.isEmpty() ? null : e.stream().mapToInt(ex).average().orElse(0); }
    
    private String calcTrend(List<HealthDiaryEntry> e, java.util.function.ToIntFunction<HealthDiaryEntry> ex, boolean hib) {
        if (e.size() < 3) return "stable";
        List<HealthDiaryEntry> s = e.stream().sorted(Comparator.comparing(HealthDiaryEntry::getEntryDate)).collect(Collectors.toList());
        int ts = s.size() / 3;
        double f = s.subList(0, ts).stream().mapToInt(ex).average().orElse(0);
        double l = s.subList(s.size() - ts, s.size()).stream().mapToInt(ex).average().orElse(0);
        double d = l - f, t = hib ? 0.5 : -0.5;
        if (hib) return d > t ? "improving" : d < -t ? "declining" : "stable";
        return d < t ? "improving" : d > -t ? "declining" : "stable";
    }
    
    private Double calcOverallScore(Double e, Double sl, Double st, int hr, int te) {
        if (e == null || sl == null || st == null || te == 0) return null;
        double es = (e / 10.0) * 100, sls = (sl / 5.0) * 100, sts = ((5.0 - st) / 5.0) * 100, rp = (hr * 10.0) / te;
        return Math.max(0, Math.min(100, (es * 0.4) + (sls * 0.3) + (sts * 0.3) - rp));
    }
}