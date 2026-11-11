package com.learning.silveragepass.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.service.ChatMemoryStore;
import com.learning.silveragepass.service.impl.InMemoryChatMemoryStore;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Spring AI Configuration for DeepSeek API Integration
 * 
 * This configuration class sets up the OpenAI service client for AI-powered health assistant
 * conversations using the DeepSeek API with deepseek-chat model.
 * 
 * Configuration includes:
 * - DeepSeek API credentials and base URL
 * - deepseek-chat model with optimized parameters for elderly user interactions
 * - HTTP client with appropriate timeouts for streaming responses
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    /**
     * Creates and configures the OpenAI service client for DeepSeek
     * 
     * The service is configured with:
     * - API key for authentication
     * - Custom base URL for DeepSeek API
     * - Extended timeout for streaming responses (60 seconds)
     * 
     * @return Configured OpenAiService instance
     */
    @Bean
    public OpenAiService openAiService() {
        // Ensure base URL ends with /
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        
        // Create custom OkHttpClient with timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();
        
        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        
        com.theokanning.openai.OpenAiApi api = retrofit.create(com.theokanning.openai.OpenAiApi.class);
        
        return new OpenAiService(api);
    }

    /**
     * Provides the configured model name for chat completions
     * 
     * @return DeepSeek-V3 model identifier
     */
    @Bean
    public String chatModel() {
        return "deepseek-ai/DeepSeek-V3";
    }

    /**
     * Provides the temperature setting for chat completions
     * 
     * Temperature of 0.7 provides a balance between:
     * - Creativity and variety in responses
     * - Consistency and reliability for elderly users
     * 
     * @return Temperature value (0.7)
     */
    @Bean
    public Double chatTemperature() {
        return 0.7;
    }

    /**
     * Provides the maximum tokens setting for chat completions
     * 
     * 2000 tokens allows for:
     * - Detailed, patient explanations suitable for elderly users
     * - Multiple conversation turns in context
     * - Comprehensive health guidance responses
     * 
     * @return Maximum tokens value (2000)
     */
    @Bean
    public Integer chatMaxTokens() {
        return 2000;
    }

    /**
     * Creates and configures the in-memory chat memory store for conversation context
     * 
     * The ChatMemoryStore manages conversation history with automatic context window
     * limitation. This is essential for the AI Health Assistant to maintain natural
     * conversations with elderly users while ensuring optimal AI performance.
     * 
     * Key Features:
     * - Stores up to 20 messages per session (context window limit)
     * - Thread-safe concurrent access for multiple users
     * - Automatic FIFO eviction when context window is exceeded
     * - In-memory storage for active sessions (30 min window)
     * 
     * Design Rationale:
     * - 20 messages provide sufficient context for natural conversation
     * - Prevents context overflow that could degrade AI response quality
     * - Balances memory usage with conversation continuity
     * - Optimized for elderly users who benefit from recent context
     * 
     * Requirements Validated:
     * - Requirement 6.7: Context window limitation (20 messages max)
     * - Property 25: Context Window Limitation
     * 
     * @return Configured InMemoryChatMemoryStore instance
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }
}
