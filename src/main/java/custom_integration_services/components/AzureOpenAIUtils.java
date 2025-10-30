package custom_integration_services.components;

import org.apache.camel.Exchange;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Utility class providing reusable methods for Azure OpenAI API integration.
 * This class centralizes common Azure OpenAI operations used across different routes.
 */
public class AzureOpenAIUtils {

    public static final String DEEPSEEKR1 = "DeepSeek-R1";
    public static final String GPT41NANO = "gpt-4.1-nano";
    public static final String GPT41MYAGENT = "gpt-4.1-myagent"; //best
    public static final String GPT5MINI2 = "gpt-5-mini-2";
    
    // Current model configuration - change this to switch models across the application
    public static final String CURRENT_MODEL = GPT41MYAGENT;
    
    /**
     * Dictionary containing Azure AI model configurations with their respective URIs, headers, and body attributes.
     * This centralizes model-specific configurations for different Azure AI services.
     */
    public static final Map<String, Map<String, Object>> AZURE_AI_MODELS_DIC = Collections.unmodifiableMap(
        new HashMap<String, Map<String, Object>>() {{
            // GPT-4.1 Nano Model Configuration
            put(GPT41NANO, new HashMap<String, Object>() {{
                put("uri", "https://ai-irabantafoundryhub416263690599.cognitiveservices.azure.com/openai/deployments/"+GPT41NANO+"/chat/completions?api-version=2025-01-01-preview");
                put("headers", new HashMap<String, String>() {{
                    put("Content-Type", "application/json");
                    put("Authorization", "Bearer " + System.getenv("AZURE_OPENAI_API_KEY"));
                }});
                put("postBody", new HashMap<String, Object>() {{
                    put("max_completion_tokens", 13107);
                    put("temperature", 1);
                    put("top_p", 1);
                    put("frequency_penalty", 0.0);
                    put("presence_penalty", 0.0);
                    put("model", GPT41NANO);
                }});
            }});
            
            // GPT-4.1 MyAgent Model Configuration
            put(GPT41MYAGENT, new HashMap<String, Object>() {{
                put("uri", "https://ai-irabantafoundryhub416263690599.cognitiveservices.azure.com/openai/deployments/gpt-4.1-myagent/chat/completions?api-version=2025-01-01-preview");
                put("headers", new HashMap<String, String>() {{
                    put("Content-Type", "application/json");
                    put("Authorization", "Bearer " + System.getenv("AZURE_OPENAI_API_KEY"));
                }});
                put("postBody", new HashMap<String, Object>() {{
                    put("max_completion_tokens", 13107);
                    put("temperature", 1);
                    put("top_p", 1);
                    put("frequency_penalty", 0.0);
                    put("presence_penalty", 0.0);
                    put("model", GPT41MYAGENT);
                }});
            }});

            // GPT-5 Mini2 Model Configuration
            put(GPT5MINI2, new HashMap<String, Object>() {{
                put("uri", "https://ai-irabantafoundryhub416263690599.cognitiveservices.azure.com/openai/deployments/"+GPT5MINI2+"/chat/completions?api-version=2025-01-01-preview");
                put("headers", new HashMap<String, String>() {{
                    put("Content-Type", "application/json");
                    put("Authorization", "Bearer " + System.getenv("AZURE_OPENAI_API_KEY"));
                }});
                put("postBody", new HashMap<String, Object>() {{
                    put("max_completion_tokens", 16384);
                    put("model", GPT5MINI2);
                }});
            }});
            
            // Azure AI DeepSeek-R1 Configuration
            put(DEEPSEEKR1, new HashMap<String, Object>() {{
                put("uri", "https://ai-irabantafoundryhub416263690599.services.ai.azure.com/models/chat/completions?api-version=2024-05-01-preview");
                put("headers", new HashMap<String, String>() {{
                    put("Content-Type", "application/json");
                    put("Authorization", "Bearer " + System.getenv("AZURE_OPENAI_API_KEY"));
                    put("azureml-model-deployment", "gpt-4");
                }});
                put("postBody", new HashMap<String, Object>() {{
                    put("max_tokens", 2048);
                    put("model", DEEPSEEKR1);
                }});
            }});
            
        }}
    );
    
    // Dynamic endpoint that uses the current model configuration (lazy initialization)
    public static final String AZURE_OPENAI_ENDPOINT = getCurrentModelUri();

    /**
     * Cleans up Camel HTTP headers that may interfere with Azure OpenAI API calls.
     * Removes standard Camel HTTP headers that are automatically set during REST processing.
     * 
     * @param exchange The Camel exchange to clean up headers for
     */
    public static void cleanupCamelHttpHeaders(Exchange exchange) {
        exchange.getIn().removeHeader("CamelHttpPath");
        exchange.getIn().removeHeader("CamelHttpQuery");
        exchange.getIn().removeHeader("CamelHttpUri");
        exchange.getIn().removeHeader("CamelHttpUrl");
    }

    /**
     * Configures HTTP headers required for Azure OpenAI API calls using current model configuration.
     * Sets the necessary headers including HTTP method, content type, and API key from the model dictionary.
     * 
     * @param exchange The Camel exchange to configure headers for
     */
    public static void configureAzureOpenAIHeaders(Exchange exchange) {
        configureModelHeaders(exchange, CURRENT_MODEL);
    }

    /**
     * Extracts AI response content from Azure OpenAI API response.
     * Parses the JSON response and extracts the content from choices[0].message.content.
     * Also handles markdown code blocks (```json...```) that may wrap the actual JSON content.
     * 
     * @param responseBody The JSON response body from Azure OpenAI API
     * @return The extracted AI response content as clean JSON string
     * @throws Exception If the response cannot be parsed or the expected structure is not found
     */
    public static String extractAIResponseContent(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);
        String rawContent = root.path("choices").get(0).path("message").path("content").asText();
        
        // Clean up markdown code blocks if present
        return cleanMarkdownCodeBlocks(rawContent);
    }

    /**
     * Removes markdown code block wrappers from AI response content.
     * Handles various markdown formats like ```json, ```JSON, or plain ```.
     * Also removes JSON comments that may cause parsing errors.
     * If no markdown blocks are found, returns the original content unchanged.
     * 
     * @param content The raw content that may contain markdown code blocks
     * @return Clean JSON string without markdown wrappers, or original content if no wrappers found
     */
    private static String cleanMarkdownCodeBlocks(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String cleaned = content.trim();
        
        // Check if content has markdown code blocks
        boolean hasMarkdownBlocks = cleaned.startsWith("```");
        
        if (hasMarkdownBlocks) {
            // Remove markdown code blocks with language specification (```json, ```JSON)
            if (cleaned.startsWith("```json") || cleaned.startsWith("```JSON")) {
                cleaned = cleaned.replaceFirst("^```[jJ][sS][oO][nN]\\s*", "");
            }
            // Remove generic markdown code blocks (```)
            else if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```\\s*", "");
            }
            
            // Remove trailing ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.replaceFirst("\\s*```$", "");
            }
            
            cleaned = cleaned.trim();
        }
        
        // Remove JSON comments that may cause parsing errors
        cleaned = removeJsonComments(cleaned);
        
        return cleaned;
    }

    /**
     * Removes JSON comments that cause parsing errors.
     * Handles both single-line and multi-line comment styles.
     * 
     * @param jsonContent The JSON content that may contain comments
     * @return Clean JSON without comments
     */
    private static String removeJsonComments(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return jsonContent;
        }
        
        // Remove single-line comments
        // This regex removes line comments but preserves them inside quoted strings
        String cleaned = jsonContent.replaceAll("(?<![\"\'])//.*?(?=\\n|\\r|$)", "");
        
        // Remove multi-line comments
        // This regex removes block comments but preserves them inside quoted strings
        cleaned = cleaned.replaceAll("/\\*[^\"]*?\\*/", "");
        
        // Clean up any remaining empty lines
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\\n");
        
        return cleaned.trim();
    }

    /**
     * Validates Azure OpenAI API response and throws exception if unsuccessful.
     * Checks the HTTP response code and throws a descriptive exception for non-200 responses.
     * 
     * @param responseCode The HTTP response code from the API call
     * @param responseBody The response body (used in error messages)
     * @throws RuntimeException If the response code indicates an error (not 200)
     */
    public static void validateAzureOpenAIResponse(int responseCode, String responseBody) {
        if (responseCode != 200) {
            throw new RuntimeException("Azure OpenAI API call failed with code " + responseCode + " and body: " + responseBody);
        }
    }

    /**
     * Gets the URI for a specific Azure AI model from the dictionary.
     * 
     * @param modelName The name of the model (e.g., "gpt-4.1-nano", "gpt-4.1-myagent")
     * @return The URI for the specified model, or null if model not found
     */
    public static String getModelUri(String modelName) {
        Map<String, Object> modelConfig = AZURE_AI_MODELS_DIC.get(modelName);
        return modelConfig != null ? (String) modelConfig.get("uri") : null;
    }

    /**
     * Gets the headers configuration for a specific Azure AI model.
     * 
     * @param modelName The name of the model
     * @return Map of headers for the specified model, or null if model not found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getModelHeaders(String modelName) {
        Map<String, Object> modelConfig = AZURE_AI_MODELS_DIC.get(modelName);
        return modelConfig != null ? (Map<String, String>) modelConfig.get("headers") : null;
    }

    /**
     * Gets the post body configuration for a specific Azure AI model.
     * 
     * @param modelName The name of the model
     * @return Map of post body attributes for the specified model, or null if model not found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getModelPostBody(String modelName) {
        Map<String, Object> modelConfig = AZURE_AI_MODELS_DIC.get(modelName);
        return modelConfig != null ? (Map<String, Object>) modelConfig.get("postBody") : null;
    }

    /**
     * Configures headers for a specific Azure AI model.
     * 
     * @param exchange The Camel exchange to configure headers for
     * @param modelName The name of the model to use for configuration
     */
    public static void configureModelHeaders(Exchange exchange, String modelName) {
        Map<String, String> headers = getModelHeaders(modelName);
        if (headers != null) {
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
            headers.forEach((key, value) -> exchange.getIn().setHeader(key, value));
        } else {
            // Fallback to basic configuration
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
            exchange.getIn().setHeader("Content-Type", "application/json");
            exchange.getIn().setHeader("Authorization", "Bearer " + System.getenv("AZURE_OPENAI_API_KEY"));
        }
    }
    
    /**
     * Gets the current model URI from the dictionary.
     * 
     * @return The URI for the current model
     */
    public static String getCurrentModelUri() {
        return getModelUri(CURRENT_MODEL);
    }
    
    /**
     * Gets the current model headers configuration.
     * 
     * @return Map of headers for the current model
     */
    public static Map<String, String> getCurrentModelHeaders() {
        return getModelHeaders(CURRENT_MODEL);
    }
    
    /**
     * Gets the current model post body configuration.
     * 
     * @return Map of post body attributes for the current model
     */
    public static Map<String, Object> getCurrentModelPostBody() {
        return getModelPostBody(CURRENT_MODEL);
    }
    
    /**
     * Merges model-specific post body attributes with the messages payload.
     * This combines the AI prompt messages with model configuration parameters.
     * 
     * @param messagesJson The JSON string containing the messages array
     * @return Complete JSON payload ready for API submission
     * @throws Exception If JSON parsing fails
     */
    public static String mergeWithCurrentModelPostBody(String messagesJson) throws Exception {
        return mergeWithModelPostBody(messagesJson, CURRENT_MODEL);
    }
    
    /**
     * Merges model-specific post body attributes with the messages payload for a specific model.
     * 
     * @param messagesJson The JSON string containing the messages array
     * @param modelName The name of the model to use for configuration
     * @return Complete JSON payload ready for API submission
     * @throws Exception If JSON parsing fails
     */
    @SuppressWarnings("unchecked")
    public static String mergeWithModelPostBody(String messagesJson, String modelName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Parse the incoming messages JSON
        Map<String, Object> messagePayload = mapper.readValue(messagesJson, Map.class);
        
        // Get model-specific post body configuration
        Map<String, Object> modelPostBody = getModelPostBody(modelName);
        
        if (modelPostBody != null) {
            // Merge model configuration with messages
            messagePayload.putAll(modelPostBody);
        }
        
        // Convert back to JSON string
        return mapper.writeValueAsString(messagePayload);
    }

    // Private constructor to prevent instantiation
    private AzureOpenAIUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}