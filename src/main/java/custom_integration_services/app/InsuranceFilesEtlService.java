package custom_integration_services.app;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import custom_integration_services.components.AzureOpenAIUtils;
import custom_integration_services.constants.AIPromptConstants;


@Component
public class InsuranceFilesEtlService extends RouteBuilder {

    @Value("${insurance.life.raw.inbound.source.directory}")
    private String lifeInsuranceInboundDirectory;

    @Value("${insurance.life.processed.outbound.source.directory}")
    private String lifeInsuranceOutboundDirectory;

    @Override
    public void configure() throws Exception {
        from("file://" + lifeInsuranceInboundDirectory + "?noop=true")
            .convertBodyTo(String.class) // Read file content as String
            .process(exchange -> {
                String originalContent = exchange.getIn().getBody(String.class);
                ObjectMapper mapper = new ObjectMapper();
                String jsonContent = mapper.writeValueAsString(originalContent); 
                String jsonPayload = AIPromptConstants.createPolicyExtractTransformerAIRequestPayload(jsonContent);
                exchange.getIn().setBody(jsonPayload);
            })
        // Call Azure OpenAI API for intelligent data extraction and transformation
        .process(exchange -> AzureOpenAIUtils.configureAzureOpenAIHeaders(exchange))
        .toD(AzureOpenAIUtils.AZURE_OPENAI_ENDPOINT)
        .process(exchange -> {
            int code = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            String body = exchange.getIn().getBody(String.class);

            AzureOpenAIUtils.validateAzureOpenAIResponse(code, body);
            String extracted = AzureOpenAIUtils.extractAIResponseContent(body);
            exchange.getIn().setBody(extracted);
        })
        // Save the transformed JSON to output directory
        .to("file://" + lifeInsuranceOutboundDirectory + "?fileName=${file:name.noext}-ai.txt");
    }
    
}
