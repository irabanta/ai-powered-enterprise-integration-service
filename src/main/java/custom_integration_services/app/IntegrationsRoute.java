package custom_integration_services.app;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import custom_integration_services.components.AzureOpenAIUtils;
import custom_integration_services.constants.AIPromptConstants;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class IntegrationsRoute extends RouteBuilder {
    @Value("${insurance.life.ibm.raw.inbound.source.directory}")
    private String lifeInsuranceInboundIbmDirectory;

    @Value("${insurance.life.ibm.raw.inbound.schema.file}")
    private String lifeInsuranceInboundIbmSchemaFile;

    @Value("${insurance.life.raw.inbound.source.directory}")
    private String lifeInsuranceInboundDirectory;

    @Value("${insurance.life.processed.outbound.source.directory}")
    private String lifeInsuranceOutboundDirectory;

    // Cache for policy responses with TTL
    private static class CacheEntry {
        final Object data;
        final LocalDateTime timestamp;
        
        CacheEntry(Object data) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }
        
        boolean isExpired(int ttlMinutes) {
            return ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) > ttlMinutes;
        }
    }
    
    private final Map<String, CacheEntry> policyCache = new ConcurrentHashMap<>();
    private final int CACHE_TTL_MINUTES = 30; // Cache for 30 minutes

    @Override
    public void configure() throws Exception {
        
        // Configure REST configuration with optimizations
        restConfiguration()
            .component("netty-http")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true")
            .host("localhost")
            .port(9081)
            // Performance optimizations
            .componentProperty("keepAlive", "true")
            .componentProperty("tcpNoDelay", "true")
            .componentProperty("reuseAddress", "true")
            .componentProperty("connectTimeout", "5000")
            .componentProperty("requestTimeout", "15000");
        
        // Default policy endpoint (uses 'welcome' fileSource)
        // example: http://localhost:9081/policy/INS-2024-001
        rest("/policy")
            .description("Policy Management API")
            .get("/{policyNumber}")
                .description("Get policy details by policy number")
                .param()
                    .name("policyNumber")
                    .type(org.apache.camel.model.rest.RestParamType.path)
                    .description("The policy number to lookup")
                    .dataType("string")
                .endParam()
                .responseMessage()
                    .code(200)
                    .message("Policy details found")
                .endResponseMessage()
                .responseMessage()
                    .code(404)
                    .message("Policy not found")
                .endResponseMessage()
                .to("direct:getPolicyDetailsByPolicyNumberFromFiles");
        
        // IBM policy endpoint (uses 'ibm' fileSource)
        // example: http://localhost:9081/ibm/policy/INS-2024-001
        rest("/ibm/policy")
            .description("IBM Policy Management API")
            .get("/{policyNumber}")
                .description("Get IBM policy details by policy number")
                .param()
                    .name("policyNumber")
                    .type(org.apache.camel.model.rest.RestParamType.path)
                    .description("The policy number to lookup")
                    .dataType("string")
                .endParam()
                .responseMessage()
                    .code(200)
                    .message("Policy details found")
                .endResponseMessage()
                .responseMessage()
                    .code(404)
                    .message("Policy not found")
                .endResponseMessage()
                .to("direct:getPolicyDetailsByPolicyNumberFromIbmFiles");
        
        // Route for default policies from any raw files
        from("direct:getPolicyDetailsByPolicyNumberFromFiles")
            .routeId("getPolicyDetailsByPolicyNumberFromFilesRoute")
            .log("Received request for policy number: ${header.policyNumber}")
            .process(exchange -> retrievePolicyFileAndPrepareForAiPrompting(exchange, 
                lifeInsuranceInboundDirectory, 
                "", 
                "unstructured"))
            .choice()
                .when(header("HTTP_RESPONSE_CODE").isEqualTo(404))
                    .log("Policy not found, returning 404 error")
                    .stop()
                .otherwise()
                    // Call Azure OpenAI API for intelligent data extraction and transformation
                    .process(exchange -> AzureOpenAIUtils.cleanupCamelHttpHeaders(exchange))
                    .process(exchange -> AzureOpenAIUtils.configureAzureOpenAIHeaders(exchange))
                    .toD(AzureOpenAIUtils.AZURE_OPENAI_ENDPOINT)
                    // Get the response and extract the transformed JSON and respond to the API caller
                    .process(exchange -> processAzureOpenAIResponse(exchange))
                    .log("Returning policy details: ${body}");
        
        // Route for IBM policies with performance optimizations
        from("direct:getPolicyDetailsByPolicyNumberFromIbmFiles")
            .routeId("getPolicyDetailsByPolicyNumberFromIbmFilesRoute")
            .log("Received request for policy number: ${header.policyNumber} (ibm)")
            // Check cache first to optimize performance
            .process(exchange -> {
                String policyNumber = exchange.getIn().getHeader("policyNumber", String.class);
                String cacheKey = "ibm_" + policyNumber;
                CacheEntry cachedEntry = policyCache.get(cacheKey);
                
                // TODO: Uncomment caching to prevent redundant processing and loadings
                // if (cachedEntry != null && !cachedEntry.isExpired(CACHE_TTL_MINUTES)) {
                //     exchange.getIn().setHeader("HTTP_RESPONSE_CODE", 200);
                //     exchange.getIn().setBody(cachedEntry.data);
                //     exchange.getIn().setHeader("CACHE_HIT", true);
                // } else {
                    exchange.getIn().setHeader("CACHE_HIT", false);
                //}
            })
            .choice()
                .when(header("CACHE_HIT").isEqualTo(true))
                    .log("Cache hit for policy: ${header.policyNumber}")
                    .stop()
                .otherwise()
                    .log("Cache miss, processing policy: ${header.policyNumber}")
                    .process(exchange -> retrievePolicyFileAndPrepareForAiPrompting(exchange, 
                        lifeInsuranceInboundIbmDirectory, 
                        lifeInsuranceInboundIbmSchemaFile,  
                        "structured"))
                    .choice()
                        .when(header("HTTP_RESPONSE_CODE").isEqualTo(404))
                            .log("Policy not found, returning 404 error")
                            .stop()
                        .otherwise()
                            // Call Azure OpenAI API with optimizations
                            .process(exchange -> AzureOpenAIUtils.cleanupCamelHttpHeaders(exchange))
                            .process(exchange -> {
                                AzureOpenAIUtils.configureAzureOpenAIHeaders(exchange);
                                // Add timeout headers for large IBM files
                                exchange.getIn().setHeader("CamelHttpConnectTimeout", 10000);
                                exchange.getIn().setHeader("CamelHttpSocketTimeout", 30000);
                            })
                            .toD(AzureOpenAIUtils.AZURE_OPENAI_ENDPOINT)
                            // Process the AI response and cache the transformed JSON and respond to the caller
                            .process(exchange -> {
                                processAzureOpenAIResponse(exchange);
                                
                                // Cache successful responses
                                if (exchange.getIn().getHeader("HTTP_RESPONSE_CODE", Integer.class) == 200) {
                                    String policyNumber = exchange.getIn().getHeader("policyNumber", String.class);
                                    String cacheKey = "ibm_" + policyNumber;
                                    Object responseBody = exchange.getIn().getBody();
                                    policyCache.put(cacheKey, new CacheEntry(responseBody));
                                    log.info("Cached response for policy: " + policyNumber);
                                }
                            })
                            .log("Returning policy details: ${body}");
    }
    
    /**
     * Reads policy file content from the specified fileSource
     * Files are expected to be in samples/policies/{fileSource}/{policyNumber}.txt
     */
    private String getMockPolicyDetails(String policyNumber, String fileSource) {
        try {
            // Construct file path
            Path filePath = Paths.get(fileSource, policyNumber + ".txt");
            
            // Check if file exists
            if (!Files.exists(filePath)) {
                System.out.println("Policy file not found: " + filePath);
                return null;
            }
            
            // Read file content
            String fileContent = Files.readString(filePath);
            System.out.println("Read policy file content for: " + policyNumber + " in folder: " + fileSource);
            return fileContent;
            
        } catch (IOException e) {
            System.err.println("Error reading policy file: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error processing policy: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Prepares Azure OpenAI API request by reading policy file and creating JSON payload
     * @param exchange Camel Exchange
     * @param fileSource   fileSource to look for policy files ("unstructure", "structured", etc.)
     */
    private void retrievePolicyFileAndPrepareForAiPrompting(Exchange exchange, String fileSource, String sourceFileSchemaPath, String sourceFileType) throws Exception {
        String policyNumber = exchange.getIn().getHeader("policyNumber", String.class);
        
        // Read policy file content
        String fileContent = getMockPolicyDetails(policyNumber, fileSource);
        
        if (fileContent == null || fileContent.trim().isEmpty()) {
            exchange.getIn().setHeader("HTTP_RESPONSE_CODE", 404);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Policy not found");
            errorResponse.put("policyNumber", policyNumber);
            exchange.getIn().setBody(errorResponse);
        } else {
            // Prepare content for Azure OpenAI API call. Assign file content to chat message
            ObjectMapper mapper = new ObjectMapper();
            String jsonContent = mapper.writeValueAsString(fileContent);
            
            // Use different AI prompts based on the fileType type
            String jsonPayload;
            if ("structured".equals(sourceFileType)) {
                // Use optimized prompt for faster processing
                jsonPayload = AIPromptConstants.createIbmPolicyExtractTransformerAIRequestPayload(jsonContent, sourceFileSchemaPath);
            } else {
                jsonPayload = AIPromptConstants.createPolicyExtractTransformerAIRequestPayload(jsonContent);
            }
            
            exchange.getIn().setBody(jsonPayload);
        }
    }
    
    /**
     * Processes Azure OpenAI API response and extracts policy details
     */
    private void processAzureOpenAIResponse(Exchange exchange) throws Exception {
        int code = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = exchange.getIn().getBody(String.class);

        if (code != 200) {
            exchange.getIn().setHeader("HTTP_RESPONSE_CODE", 404);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "API call failed with code " + code);
            errorResponse.put("details", body);
            exchange.getIn().setBody(errorResponse);
        } else {
            // Extract choices[0].message.content using utility method
            String extracted = AzureOpenAIUtils.extractAIResponseContent(body);
            
            // Parse the extracted JSON and set as response
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(extracted);
                
                // Handle both array and object responses
                Object policyDetails;
                if (jsonNode.isArray()) {
                    // For IBM policy responses that return arrays
                    policyDetails = mapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>(){});
                } else {
                    // For simple policy responses that return objects
                    policyDetails = mapper.convertValue(jsonNode, Map.class);
                }
                
                exchange.getIn().setHeader("HTTP_RESPONSE_CODE", 200);
                exchange.getIn().setBody(policyDetails);
            } catch (Exception e) {
                exchange.getIn().setHeader("HTTP_RESPONSE_CODE", 500);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to parse AI response");
                errorResponse.put("parseError", e.getMessage());
                errorResponse.put("rawAIResponse", extracted);
                errorResponse.put("fullAzureResponse", body);
                exchange.getIn().setBody(errorResponse);
            }
        }
    }
    
    /**
     * Cleans up expired cache entries periodically
     */
    private void cleanupExpiredCacheEntries() {
        policyCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(CACHE_TTL_MINUTES));
    }
    
    /**
     * Gets cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", policyCache.size());
        stats.put("cacheTTLMinutes", CACHE_TTL_MINUTES);
        
        long expiredCount = policyCache.values().stream()
            .mapToLong(entry -> entry.isExpired(CACHE_TTL_MINUTES) ? 1 : 0)
            .sum();
        stats.put("expiredEntries", expiredCount);
        
        return stats;
    }
}
