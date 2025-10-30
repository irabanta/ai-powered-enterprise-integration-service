package custom_integration_services.constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import custom_integration_services.components.AzureOpenAIUtils;

import java.io.IOException;

/**
 * Constants class containing reusable AI prompts and instructions for the insurance data processing system.
 * This centralizes AI prompt management and ensures consistency across all integration routes.
 */
public class AIPromptConstants {

    /**
     * Optimized system prompt for faster IBM COBOL fixed-width policy data extraction.
     * This is a streamlined version that focuses on essential fields for better performance.
     */
    public static final String OPTIMIZED_IBM_POLICY_TRANSFORMER = 
        "Extract key insurance policy data from IBM fixed-width format. " +
        "Focus on: policy holder name, policy number, policy type, status, dates, premium amounts. " +
        "Return concise JSON with essential fields only. " +
        "Convert dates to MM/dd/yyyy format. Parse numeric values properly. " +
        "Response format: {\"policies\":[{\"policyNumber\":\"...\",\"firstName\":\"...\",\"lastName\":\"...\",\"policyType\":\"...\",\"status\":\"...\",\"premium\":...,\"effectiveDate\":\"...\"}]}";

    /**
     * System prompt for insurance policy holder data transformation.
     * This prompt instructs the AI to extract specific fields from unstructured data
     * and transform them into a standardized JSON format.
     * 
     * Fields extracted: FirstName, LastName, Gender, DateOfBirth, SSN, PolicyNumber
     * 
     * Transformation Rules:
     * - DateOfBirth: Convert to MM/dd/yyyy format
     * - Gender: Normalize to single character 'M' for Male, 'F' for Female
     * - Output: Structured JSON with consistent field names
     */
    public static final String INSURANCE_DATA_TRANSFORMER_PROMPT = 
        "You are an insurance policy holder data transformer that extracts any incoming data, " +
        "identify the fields listed below and transform the output into expected output format given below. " +
        "Fields to be identified: Firstname, Lastname, Gender, DateOfBirth, SSN, PolicyNumber. " +
        "Transformation Rules: (1) For any given input format of DateOfBirth, the transformed dob should be MM/dd/yyyy format. " +
        "(2) For any Gender input, the output should be just 1 character 'M' for Male and 'F' for Female. " +
        "Expected output JSON format: {policyNumber: {{PolicyNumber}}, firstName: {{FirstName}}, lastName: {{LastName}}, gender: {{Gender}}, dob: {{DateOfBirth}}, ssn: {{SSN}}}";

    /**
     * Creates optimized JSON payload for faster IBM policy processing using current model configuration.
     * This method uses the optimized prompt and merges with current model settings.
     */
    public static String createOptimizedIbmPolicyRequestPayload(String userContent) {
        try {
            String basePayload = String.format(
                "{"
                + "\"messages\": ["
                + "{"
                + "\"role\": \"system\","
                + "\"content\": %s"
                + "},"
                + "{"
                + "\"role\": \"user\","
                + "\"content\": %s"
                + "}"
                + "]"
                + "}",
                escapeForJson(OPTIMIZED_IBM_POLICY_TRANSFORMER),
                userContent
            );
            
            // Merge with current model configuration
            return AzureOpenAIUtils.mergeWithCurrentModelPostBody(basePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create optimized IBM AI request payload", e);
        }
    }

    /**
     * Example JSON output format for life insurance policy data transformation.
     * This constant provides the exact structure expected from the AI transformation
     * of IBM COBOL fixed-width format policy data.
     */
    public static final String LIFE_INSURANCE_POLICY_JSON_OUTPUT_EXAMPLE = 
        "Expected output JSON format:  " +
        "[{\"name\":\"Alice Johnson\",\"policy\":[{\"id\":2,\"isPaid\":true,\"policyType\":\"Life Policy\",\"policyNumber\":\"TL6534868\",\"productType\":\"Term Life\",\"estimatedAnnuitizationDate\":null,\"policyStatus\":\"In-Force\",\"anniversaryDate\":\"03/05/2019\",\"sumAssured\":null,\"premium\":2650,\"dividend\":500,\"deathBenefit\":500000,\"outstandingLoan\":1800,\"netCashValue\":22650,\"accountValue\":29150,\"surrenderValue\":26650,\"surrenderFee\":2500,\"document\":\"MyPolicyDocument\",\"faceAmount\":500000,\"availableLoanValue\":3000,\"modeOfPayment\":\"Annual\",\"withdrawalTaken\":1000,\"beneficiary\":{\"primaryBeneficiary\":[{\"firstName\":\"Tom\",\"lastName\":\"Smith\",\"relationship\":\"Father\",\"birthDate\":\"12/25/1988\",\"percentage\":\"80\",\"ssn\":\"6631\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"tom.smith@umbrella.com\"},{\"firstName\":\"Jake\",\"lastName\":\"Smith\",\"relationship\":\"Brother\",\"birthDate\":\"11/25/2015\",\"percentage\":\"20\",\"ssn\":\"6632\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"jake.smith@umbrella.com\"}],\"contingentBeneficiary\":[{\"firstName\":\"Liam\",\"lastName\":\"Smith\",\"relationship\":\"Uncle\",\"birthDate\":\"10/20/1998\",\"percentage\":\"100\",\"ssn\":\"6633\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"liam.smith@umbrella.com\"}]},\"premiumDueDate\":\"03/11/2031\",\"policyIssueDate\":\"03/05/2018\",\"policyIssueState\":\"Minnesota\",\"isChildRiderCoverage\":\"No\",\"childRiderCoverage\":null,\"isWaiverOfPremiumCoverage\":\"Yes\",\"waiverOfPremiumCoverage\":100000,\"isAccidentalDeathBenefitCoverage\":\"No\",\"isDisabilityIncomeBenefitCoverage\":\"No\",\"disabilityIncomeBenefitCoverage\":null,\"isColaCoverage\":\"Yes\",\"colaCoverage\":200000,\"underwritingClass\":\"Standard\",\"annualStatement\":\"AnnualStatement\",\"policyPrint\":\"PolicyPrint\",\"premiumReceipt\":\"PremiumReceipt\",\"policyLapseNotice\":\"PolicyLapseNotice\",\"freeLookNotice\":\"FreeLookNotice\",\"annualIncome\":135000,\"payor\":\"Tom Smith\",\"totalLoanBalance\":7000,\"availableLoanCash\":25000,\"cashBorrowedPercent\":\"3.25\",\"policyLoanIntrestRate\":\"3.50%\",\"premiumFrequency\":\"Semi-Annual\",\"renewalDate\":\"01/02/2025\",\"maturityDate\":\"03/05/2027\",\"annualPremium\":2650,\"premiumPaidTo\":\"03/05/2024\"}]},{\"name\":\"Bob Johnson\",\"policy\":[{\"id\":1,\"isPaid\":true,\"policyType\":\"Annuity Policy\",\"policyNumber\":\"VA6534866\",\"productType\":\"Variable Annuity\",\"estimatedAnnuitizationDate\":\"05/01/2029\",\"policyStatus\":\"In-Force\",\"anniversaryDate\":\"05/01/2020\",\"sumAssured\":null,\"premium\":2500,\"dividend\":800,\"deathBenefit\":null,\"outstandingLoan\":null,\"netCashValue\":800000,\"accountValue\":850000,\"surrenderValue\":788325,\"surrenderFee\":null,\"document\":\"MyPolicyDocument\",\"faceAmount\":650000,\"availableLoanValue\":3000,\"modeOfPayment\":\"Annual\",\"withdrawalTaken\":1000,\"beneficiary\":{\"primaryBeneficiary\":[{\"firstName\":\"Tom\",\"lastName\":\"Smith\",\"relationship\":\"Father\",\"birthDate\":\"12/25/1988\",\"percentage\":\"50\",\"ssn\":\"6631\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"tom.smith@umbrella.com\"},{\"firstName\":\"Jake\",\"lastName\":\"Smith\",\"relationship\":\"Brother\",\"birthDate\":\"11/25/2015\",\"percentage\":\"50\",\"ssn\":\"6632\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"jake.smith@umbrella.com\"}],\"contingentBeneficiary\":[{\"firstName\":\"Liam\",\"lastName\":\"Smith\",\"relationship\":\"Uncle\",\"birthDate\":\"10/20/1998\",\"percentage\":\"50\",\"ssn\":\"6633\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"liam.smith@umbrella.com\"},{\"firstName\":\"Emma\",\"lastName\":\"Smith\",\"relationship\":\"Aunt\",\"birthDate\":\"02/21/2004\",\"percentage\":\"50\",\"ssn\":\"6634\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"emma.smith@umbrella.com\"}]},\"premiumDueDate\":\"11/07/2024\",\"policyIssueDate\":\"05/01/2023\",\"policyIssueState\":\"Delaware\",\"isChildRiderCoverage\":\"No\",\"childRiderCoverage\":null,\"isWaiverOfPremiumCoverage\":\"Yes\",\"waiverOfPremiumCoverage\":100000,\"isAccidentalDeathBenefitCoverage\":\"No\",\"isDisabilityIncomeBenefitCoverage\":\"Yes\",\"disabilityIncomeBenefitCoverage\":100000,\"isColaCoverage\":\"No\",\"colaCoverage\":null,\"underwritingClass\":null,\"annualStatement\":\"AnnualStatement\",\"policyPrint\":\"PolicyPrint\",\"premiumReceipt\":\"PremiumReceipt\",\"policyLapseNotice\":\"PolicyLapseNotice\",\"freeLookNotice\":\"FreeLookNotice\",\"annualIncome\":120000,\"payor\":\"Tom Smith\",\"totalLoanBalance\":2000,\"availableLoanCash\":25000,\"cashBorrowedPercent\":\"2.50\",\"policyLoanIntrestRate\":\"3.50%\",\"premiumFrequency\":\"Quarterly\",\"renewalDate\":null,\"maturityDate\":null,\"annualPremium\":0,\"premiumPaidTo\":null},{\"id\":2,\"isPaid\":true,\"policyType\":\"Life Policy\",\"policyNumber\":\"TL6534867\",\"productType\":\"Term Life\",\"estimatedAnnuitizationDate\":null,\"policyStatus\":\"Surrendered\",\"anniversaryDate\":\"05/01/2021\",\"sumAssured\":null,\"premium\":6230,\"dividend\":500,\"deathBenefit\":650000,\"outstandingLoan\":0,\"netCashValue\":6250,\"accountValue\":15000,\"surrenderValue\":12250,\"surrenderFee\":2500,\"document\":\"MyPolicyDocument\",\"faceAmount\":650000,\"availableLoanValue\":3000,\"modeOfPayment\":\"Annual\",\"withdrawalTaken\":1000,\"beneficiary\":{\"primaryBeneficiary\":[{\"firstName\":\"Tom\",\"lastName\":\"Smith\",\"relationship\":\"Father\",\"birthDate\":\"12/25/1988\",\"percentage\":\"80\",\"ssn\":\"6631\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"tom.smith@umbrella.com\"},{\"firstName\":\"Jake\",\"lastName\":\"Smith\",\"relationship\":\"Brother\",\"birthDate\":\"11/25/2015\",\"percentage\":\"20\",\"ssn\":\"6632\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"jake.smith@umbrella.com\"}],\"contingentBeneficiary\":[{\"firstName\":\"Liam\",\"lastName\":\"Smith\",\"relationship\":\"Uncle\",\"birthDate\":\"10/20/1998\",\"percentage\":\"100\",\"ssn\":\"6633\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"liam.smith@umbrella.com\"}]},\"premiumDueDate\":\"11/07/2026\",\"policyIssueDate\":\"05/01/2023\",\"policyIssueState\":\"Florida\",\"isChildRiderCoverage\":\"No\",\"childRiderCoverage\":null,\"isWaiverOfPremiumCoverage\":\"Yes\",\"waiverOfPremiumCoverage\":100000,\"isAccidentalDeathBenefitCoverage\":\"No\",\"isDisabilityIncomeBenefitCoverage\":\"No\",\"disabilityIncomeBenefitCoverage\":null,\"isColaCoverage\":\"Yes\",\"colaCoverage\":200000,\"underwritingClass\":\"Preferred Plus\",\"annualStatement\":\"AnnualStatement\",\"policyPrint\":\"PolicyPrint\",\"premiumReceipt\":\"PremiumReceipt\",\"policyLapseNotice\":\"PolicyLapseNotice\",\"freeLookNotice\":\"FreeLookNotice\",\"annualIncome\":0,\"payor\":\"Tom Smith\",\"totalLoanBalance\":2000,\"availableLoanCash\":25000,\"cashBorrowedPercent\":\"2.50\",\"policyLoanIntrestRate\":\"3.50%\",\"premiumFrequency\":\"Semi-Annual\",\"renewalDate\":\"01/12/2024\",\"maturityDate\":\"05/01/2029\",\"annualPremium\":6230,\"premiumPaidTo\":\"05/01/2021\"},{\"id\":3,\"isPaid\":true,\"policyType\":\"Life Policy\",\"policyNumber\":\"UL6534868\",\"productType\":\"Universal Life\",\"estimatedAnnuitizationDate\":null,\"policyStatus\":\"Lapsed\",\"anniversaryDate\":\"03/01/2012\",\"sumAssured\":null,\"premium\":2000,\"dividend\":500,\"deathBenefit\":200000,\"outstandingLoan\":0,\"netCashValue\":9100,\"accountValue\":15600,\"surrenderValue\":13100,\"surrenderFee\":2500,\"document\":\"MyPolicyDocument\",\"faceAmount\":200000,\"availableLoanValue\":3000,\"modeOfPayment\":\"Annual\",\"withdrawalTaken\":1000,\"beneficiary\":{\"primaryBeneficiary\":[{\"firstName\":\"Tom\",\"lastName\":\"Smith\",\"relationship\":\"Father\",\"birthDate\":\"12/25/1983\",\"percentage\":\"50\",\"ssn\":\"6631\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"tom.smith@umbrella.com\"},{\"firstName\":\"Jake\",\"lastName\":\"Smith\",\"relationship\":\"Brother\",\"birthDate\":\"11/25/2010\",\"percentage\":\"50\",\"ssn\":\"6632\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"jake.smith@umbrella.com\"}],\"contingentBeneficiary\":[{\"firstName\":\"Liam\",\"lastName\":\"Smith\",\"relationship\":\"Uncle\",\"birthDate\":\"10/20/1993\",\"percentage\":\"50\",\"ssn\":\"6633\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"liam.smith@umbrella.com\"},{\"firstName\":\"Emma\",\"lastName\":\"Smith\",\"relationship\":\"Aunt\",\"birthDate\":\"02/21/1999\",\"percentage\":\"50\",\"ssn\":\"6634\",\"address\":\"2090 Marina AVE\",\"city\":\"Petaluma\",\"state\":\"Georgia\",\"zipCode\":\"95954\",\"phone\":\"971-201-2090\",\"email\":\"emma.smith@umbrella.com\"}]},\"premiumDueDate\":\"03/01/2034\",\"policyIssueDate\":\"03/01/2023\",\"policyIssueState\":\"Maryland\",\"isChildRiderCoverage\":\"No\",\"childRiderCoverage\":null,\"isWaiverOfPremiumCoverage\":\"Yes\",\"waiverOfPremiumCoverage\":100000,\"isAccidentalDeathBenefitCoverage\":\"Yes\",\"isDisabilityIncomeBenefitCoverage\":\"No\",\"disabilityIncomeBenefitCoverage\":null,\"isColaCoverage\":\"Yes\",\"colaCoverage\":200000,\"underwritingClass\":\"Substandard\",\"annualStatement\":\"AnnualStatement\",\"policyPrint\":\"PolicyPrint\",\"premiumReceipt\":\"PremiumReceipt\",\"policyLapseNotice\":\"PolicyLapseNotice\",\"freeLookNotice\":\"FreeLookNotice\",\"annualIncome\":0,\"payor\":\"Tom Smith\",\"totalLoanBalance\":1100,\"availableLoanCash\":25000,\"cashBorrowedPercent\":\"2.50\",\"policyLoanIntrestRate\":\"3.50%\",\"premiumFrequency\":\"Annual\",\"renewalDate\":\"01/20/2024\",\"maturityDate\":\"03/01/2028\",\"annualPremium\":2000,\"premiumPaidTo\":\"03/01/2021\"}]}]";

    /**
     * System prompt for IBM COBOL fixed-width life insurance policy data transformation.
     * This prompt instructs the AI to parse IBM mainframe fixed-width format files
     * and extract comprehensive policy details including beneficiary information.
     * 
     * Input: IBM COBOL fixed-width formatted insurance policy data
     * Output: Structured JSON array containing all insured policies with complete details
     * 
     * Transformation Rules:
     * - Parse fixed-width positional data according to field definitions
     * - Extract main policy holder information
     * - Extract all associated policies (Life, Annuity, etc.)
     * - Extract primary and contingent beneficiaries for each policy
     * - Convert financial amounts from zero-padded format to integers
     * - Parse boolean values (Yes/No, True/False) appropriately
     * - Handle null values for missing or empty fields
     * - Maintain data relationships between policies and beneficiaries
     */
    public static final String LIFE_INSURANCE_POLICY_DETAIL_DATA_TRANSFORMER_FROM_FIXED_FILE = 
        "You are an expert IBM mainframe data parser specialized in extracting life insurance policy data from COBOL fixed-width format files. " +
        "Parse the incoming fixed-width formatted data and extract all policy holder information, policy details, and beneficiary data. " +
        "The input contains multiple record types: main policy holder data, life policy records, annuity policy records, and beneficiary records. " +
        "Follow the field position definitions provided in the comments to accurately extract data. " +
        "Transform all extracted data into a comprehensive JSON array structure. " +
        "Transformation Rules: " +
        "(1) Parse fixed-width fields according to position definitions in comments. " +
        "(2) Convert zero-padded numeric fields to proper integer/decimal values. " +
        "(3) Handle text fields by trimming trailing spaces. " +
        "(4) Convert Yes/No and True/False values to appropriate boolean or string format as needed. " +
        "(5) Group policies by insured person name. " +
        "(6) Include all beneficiary details for each policy. " +
        "(7) Handle null/empty values appropriately. " +
        "(8) Ensure all dates are in MM/dd/yyyy format. " +
        "(9) Wherever 2 character USA state codes are there like state: GA, convert them to full state names like state: Georgia. " +
        "(10) Wherever any value is Yes or No, convert them into boolean true or false. " +
        "(11) IMPORTANT: Return only clean JSON without any comments, explanations, or additional text. Do not include // or /* */ style comments in the response. " +
        " " + LIFE_INSURANCE_POLICY_JSON_OUTPUT_EXAMPLE;

    public static final String IBM_POLICY_DATA_TRANSFORMER_PROMPT = 
        "You are a fixed-width ETL parser. Do not infer or guess. No free reasoning. " +
        "Direct substring extraction. Return JSON only whose schema is given in previous system message. " +
        "Extract substrings based on fixed positions only whose schema is given in previous system message. " +
        "The input contains multiple record types: main policy holder data, life policy records, annuity policy records, and beneficiary records. " +
        "Convert dates to MM/dd/yyyy format and parse numeric values properly. " +
        "Wherever 2 character USA state codes are there, convert to fullname. Examples are GA: Georgia, CA: California, NY: New York, TX: Texas, FL: Florida, MN: Minnesota, DE: Delaware, MD: Maryland. ";

    /**
     * Creates a standardized JSON payload for Azure OpenAI API calls using current model configuration.
     * This method builds the complete request structure with system and user messages,
     * then merges it with the current model's post body configuration.
     * 
     * @param userContent The content to be processed by the AI (policy file content)
     * @return Formatted JSON string ready for API submission with model-specific parameters
     */
    public static String createPolicyExtractTransformerAIRequestPayload(String userContent) {
        try {
            String basePayload = """
            {
                "messages": [
                    {
                        "role": "system",
                        "content": "%s"
                    },
                    {
                        "role": "user",
                        "content": %s
                    }
                ]
            }
            """.formatted(INSURANCE_DATA_TRANSFORMER_PROMPT, userContent);
            
            // Merge with current model configuration
            return AzureOpenAIUtils.mergeWithCurrentModelPostBody(basePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create AI request payload", e);
        }
    }

    /**
     * Reads the IBM fixed-width policy schema from the schema file.
     * 
     * @return The schema content as a string, or a default message if file cannot be read
     */
    private static String readIbmPolicySchema(String fileSchemaPath) {
        try {
            Path schemaPath = Paths.get(fileSchemaPath);
            return Files.readString(schemaPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read IBM policy schema", e);
        }
    }

    public static String createIbmPolicyExtractTransformerAIRequestPayload(String userContent, String fileSchemaPath) {
        try {
            // Read file content from folder "samples/schemas/ibm/fixed-width-policy-schema.txt"
            String fixed_width_file_schema = readIbmPolicySchema(fileSchemaPath);

            String basePayload = String.format(
                "{"
                + "\"messages\": ["
                + "{"
                + "\"role\": \"system\","
                + "\"content\": %s"
                + "},"
                + "{"
                + "\"role\": \"system\","
                + "\"content\": %s"
                + "},"
                + "{"
                + "\"role\": \"system\","
                + "\"content\": %s"
                + "},"
                + "{"
                + "\"role\": \"user\","
                + "\"content\": %s"
                + "}"
                + "]"
                + "}",
                escapeForJson(fixed_width_file_schema),
                escapeForJson(LIFE_INSURANCE_POLICY_JSON_OUTPUT_EXAMPLE),
                escapeForJson(IBM_POLICY_DATA_TRANSFORMER_PROMPT),
                userContent
            );
            
            // Merge with current model configuration
            return AzureOpenAIUtils.mergeWithCurrentModelPostBody(basePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create IBM AI request payload", e);
        }
    }    /**
     * Creates a JSON payload for Azure OpenAI API calls specifically for IBM COBOL fixed-width data parsing.
     * This method builds the complete request structure for comprehensive policy data extraction
     * using the current model configuration.
     * 
     * @param userContent The IBM COBOL fixed-width formatted content to be processed by the AI
     * @return Formatted JSON string ready for API submission for complex policy data extraction
     */
    public static String createLifeInsurancePolicyDetailTransformerAIRequestPayload(String userContent) {
        try {
            String basePayload = String.format(
                "{"
                + "\"messages\": ["
                + "{"
                + "\"role\": \"system\","
                + "\"content\": %s"
                + "},"
                + "{"
                + "\"role\": \"user\","
                + "\"content\": %s"
                + "}"
                + "]"
                + "}",
                escapeForJson(LIFE_INSURANCE_POLICY_DETAIL_DATA_TRANSFORMER_FROM_FIXED_FILE),
                userContent
            );
            
            // Merge with current model configuration
            return AzureOpenAIUtils.mergeWithCurrentModelPostBody(basePayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create life insurance AI request payload", e);
        }
    }

    /**
     * Escapes a string for safe inclusion in JSON.
     * Handles quotes, newlines, and other special characters.
     * 
     * @param text The text to escape
     * @return JSON-safe escaped string with surrounding quotes
     */
    private static String escapeForJson(String text) {
        if (text == null) {
            return "null";
        }
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            + "\"";
    }

    // Private constructor to prevent instantiation
    private AIPromptConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}