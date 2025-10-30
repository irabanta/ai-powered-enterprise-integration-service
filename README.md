# AI-Powered Enterprise Integration Service

An intelligent data processing system that leverages Azure OpenAI for automated extraction and transformation of insurance policy data from various formats including unstructured text and IBM COBOL fixed-width files.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Data Processing Pipeline](#data-processing-pipeline)
- [API Endpoints](#api-endpoints)
- [Setup & Configuration](#setup--configuration)
- [Usage](#usage)
- [Data Samples](#data-samples)
- [AI Models & Prompts](#ai-models--prompts)
- [Performance Features](#performance-features)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

## Overview

This enterprise integration solution demonstrates the power of **Generative AI** for intelligent data extraction and transformation in insurance domain. The system automatically processes various data formats and converts them into standardized JSON structures using Azure OpenAI models.

### Key Features
- 🤖 **AI-Powered Data Extraction**: Automatically identifies and extracts relevant fields from unstructured insurance policy data
- � **Multi-Format Support**: Processes both unstructured text and IBM COBOL fixed-width format files
- �🔄 **Intelligent Transformation**: Converts diverse data formats to standardized JSON using natural language processing
- 📁 **File-Based Integration**: Monitors input directories and processes files automatically via Apache Camel
- 🌐 **Multi-Model AI Support**: Supports multiple Azure AI models (GPT-4.1 Nano, GPT-4.1 , GPT-5 Mini2, DeepSeek-R1)
- 🔗 **RESTful API**: Provides endpoints for real-time policy data retrieval by policy number
- 📂 **Dynamic File Processing**: Reads policy files from configurable directories with intelligent caching
- ⚡ **Real-time Processing**: Immediate processing of incoming files and API requests
- 📊 **Structured Output**: Generates consistent JSON output format with comprehensive policy details
- 🏗️ **Modular Architecture**: Clean separation of concerns with dedicated utility classes and constants
- 🧠 **Smart Prompting**: Specialized AI prompts optimized for different data types and processing scenarios
- 🚀 **Performance Optimizations**: Intelligent caching with TTL, timeout handling, and connection pooling

## REST API Architecture Diagram: The REST API fetch fixed-width policy data, ETL by AI, and returns JSON response with caching

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REST API (netty/http endpoints)             │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                        │
│              (IntegrationsRoute, DemoMainApplication)               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                              Policy Files                            │
│                (files/inbound/* , files/inbound/ibm/*)               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          ETL by AI (Azure OpenAI)                    │
│                 (AzureOpenAIUtils + AIPromptConstants)              │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   JSON Response & Caching (30-min TTL)              │
│               (API returns JSON and caches IBM responses)           │
└─────────────────────────────────────────────────────────────────────┘
```

## FILE ETL Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    File Monitor / ETL (Batch)                      │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  InsuranceFilesEtlService (Apache Camel)            │
│        - Watches `files/inbound/*` for new policy files (raw)       │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          ETL by AI (Azure OpenAI)                   │
│               (AzureOpenAIUtils + AIPromptConstants)               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                            Output Files                             │
│                       `files/outbound/*` (processed)                │
└─────────────────────────────────────────────────────────────────────┘
```

Note: This "FILE ETL Architecture Diagram" documents the batch/file-driven ETL flow. The REST API architecture above remains unchanged and is used for on-demand policy lookups and cached responses.


## Technologies Used

### Core Framework
- **Java 25** - Latest Java version with modern language features
- **Spring Boot 3.5.6** - Enterprise-grade application framework
- **Apache Camel 4.14.0** - Integration framework for data processing pipelines

### AI & Machine Learning
- **Azure OpenAI Service** - Multiple model support:
  - **GPT-4.1 ** (Primary) - Best performance for complex policy data
  - **GPT-4.1 Nano** - Optimized for speed and efficiency
  - **GPT-5 Mini2** - Advanced language understanding
  - **DeepSeek-R1** - Alternative AI model for specialized tasks
- **Azure AI Foundry** - Model deployment and management platform

### Integration & Communication
- **Netty HTTP** - High-performance HTTP server and client
- **Apache Camel HTTP** - HTTP component for external API calls
- **Jackson JSON** - JSON processing and serialization
- **REST API** - RESTful web services with OpenAPI documentation

### Development & Build Tools
- **Maven 3.9+** - Dependency management and build automation
- **JUnit 5** - Unit testing framework
- **Spring Boot Test** - Integration testing support

### Infrastructure
- **File System Integration** - Automated file monitoring and processing
- **Caching System** - In-memory caching with TTL for performance optimization
- **Connection Pooling** - Efficient resource management for HTTP connections

## Project Structure

```
ai-powered-enterprise-integration-service/
├── src/
│   ├── main/
│   │   ├── java/custom_integration_services/
│   │   │   ├── app/
│   │   │   │   ├── DemoMainApplication.java           # Spring Boot main application
│   │   │   │   ├── InsuranceFilesEtlService.java      # File-based ETL processing service
│   │   │   │   └── IntegrationsRoute.java             # REST API and routing configuration
│   │   │   ├── components/
│   │   │   │   └── AzureOpenAIUtils.java              # Azure OpenAI integration utilities
│   │   │   └── constants/
│   │   │       └── AIPromptConstants.java             # AI prompts and configurations
│   │   └── resources/
│   │       └── application.properties                 # Application configuration
│   └── test/
│       └── java/custom_integration_services/app/
│           └── InsuranceFilesEtlServiceTests.java     # Unit tests
├── files/                                             # Data processing directories
│   ├── inbound/                                       # Input files
│   │   ├── auto-insurance/raw/                        # Auto insurance raw data
│   │   └── life-insurance/
│   │       ├── raw/                                   # General life insurance data
│   │       │   ├── INS-2024-001.txt                   # Sample policy files
│   │       │   ├── INS-2024-002.txt
│   │       │   └── ...
│   │       └── ibm/                                   # IBM COBOL format data
│   │           ├── raw/
│   │           │   └── FIA653487.txt                  # Fixed-width format files
│   │           └── schemas/
│   │               └── fixed-width-policy-schema.txt  # Format documentation
│   └── outbound/                                      # Processed output files
│       ├── auto-insurance/processed/
│       └── life-insurance/processed/
│           ├── INS-2024-001-ai.txt                   # AI-processed JSON output
│           ├── INS-2024-002-ai.txt
│           └── ...
├── target/                                           # Maven build output
├── pom.xml                                           # Maven configuration
├── mvnw, mvnw.cmd                                    # Maven wrapper scripts
└── README.md                                         # Project documentation
```

## Core Components

### 1. DemoMainApplication
- **Purpose**: Spring Boot application entry point
- **Location**: `src/main/java/custom_integration_services/app/DemoMainApplication.java`
- **Functionality**: Bootstraps the entire application with Spring Boot auto-configuration

### 2. InsuranceFilesEtlService
- **Purpose**: File-based ETL processing for insurance data
- **Location**: `src/main/java/custom_integration_services/app/InsuranceFilesEtlService.java`
- **Features**:
  - Monitors `files/inbound/life-insurance/raw/` directory
  - Automatically processes new files using Apache Camel routes
  - Transforms unstructured policy data to JSON using Azure OpenAI
  - Saves processed results to `files/outbound/life-insurance/processed/`

### 3. IntegrationsRoute
- **Purpose**: REST API endpoints and advanced routing logic
- **Location**: `src/main/java/custom_integration_services/app/IntegrationsRoute.java`
- **API Endpoints**:
  - `GET /policy/{policyNumber}` - Retrieve general policy information
  - `GET /ibm/policy/{policyNumber}` - Retrieve IBM COBOL format policy data
- **Features**:
  - Intelligent caching system with 30-minute TTL
  - Performance optimizations (connection pooling, TCP no-delay)
  - Real-time file processing and API response generation

### 4. AzureOpenAIUtils
- **Purpose**: Centralized Azure OpenAI integration utilities
- **Location**: `src/main/java/custom_integration_services/components/AzureOpenAIUtils.java`
- **Features**:
  - Multi-model support with easy switching between AI models
  - HTTP header management and request configuration
  - Response parsing and error handling
  - Markdown code block cleaning for JSON extraction

### 5. AIPromptConstants
- **Purpose**: AI prompt templates and request payload generation
- **Location**: `src/main/java/custom_integration_services/constants/AIPromptConstants.java`
- **Features**:
  - Optimized prompts for different data formats (IBM COBOL, general policies)
  - JSON payload generation for Azure OpenAI API calls
  - Comprehensive transformation rules and field mappings

## Data Processing Pipeline

### File-Based Processing Flow
1. **File Monitoring**: Apache Camel monitors input directories for new files
2. **Content Reading**: Files are read and content is extracted as strings
3. **AI Payload Creation**: Content is wrapped in AI-specific request format
4. **Azure OpenAI Processing**: Data is sent to Azure OpenAI for intelligent extraction
5. **Response Processing**: AI response is parsed and cleaned
6. **Output Generation**: Structured JSON is saved to output directory

### API-Based Processing Flow
1. **REST Request**: Client sends GET request with policy number
2. **Cache Check**: System checks if response is cached and not expired
3. **File Discovery**: If not cached, system searches for matching policy files
4. **AI Processing**: Found files are processed using Azure OpenAI
5. **Response Caching**: Results are cached for future requests
6. **JSON Response**: Structured data is returned to client

## API Endpoints

### General Policy Endpoint
```
GET http://localhost:9081/policy/{policyNumber}
```
**Purpose**: Retrieve policy information from general life insurance files

**Example Request**:
```bash
curl -X GET "http://localhost:9081/policy/INS-2024-001"
```

**Sample Response**:
```json
{
  "policyNumber": "INS-2024-001",
  "firstName": "John",
  "lastName": "Smith",
  "gender": "M",
  "dob": "12/25/1985",
  "ssn": "123-45-6789"
}
```

### IBM Policy Endpoint
```
GET http://localhost:9081/ibm/policy/{policyNumber}
```
**Purpose**: Retrieve policy information from IBM COBOL fixed-width format files

**Example Request**:
```bash
curl -X GET "http://localhost:9081/ibm/policy/FIA653487"
```

**Sample Response**: Complex JSON structure with comprehensive policy details including beneficiaries, coverage amounts, and policy history.

## Setup & Configuration

### Prerequisites
- **Java 25** installed and configured
- **Maven 3.9+** for dependency management
- **Azure OpenAI API Key** with access to supported models
- **Git** for version control

### Environment Variables
Set the following environment variable: At present, only Azure Deployed Models are supported. Will add support for OpenAI hosted models in future releases.
```bash
export AZURE_OPENAI_API_KEY="your-azure-openai-api-key"
```

### Installation Steps

1. **Clone the repository**:
```bash
git clone https://github.com/irabanta/ai-powered-enterprise-integration-service.git
cd ai-powered-enterprise-integration-service
```

2. **Configure Azure OpenAI**:
   - Update model endpoints in `AzureOpenAIUtils.java` if needed
   - Ensure your Azure OpenAI service has the required model deployments

3. **Build the application**:
```bash
./mvnw clean compile
```

4. **Run the application**:
```bash
./mvnw spring-boot:run
```

5. **Verify installation**:
   - Application starts on port 9080 (main app) and 9081 (API endpoints)
   - Check logs for successful startup
   - Test API endpoints using provided examples

### Configuration Files

#### application.properties
```properties
spring.application.name=app
server.port=9080

# Insurance file processing configuration
insurance.life.raw.inbound.source.directory=files/inbound/life-insurance/raw
insurance.life.processed.outbound.source.directory=files/outbound/life-insurance/processed
insurance.auto.raw.inbound.source.directory=files/inbound/auto-insurance/raw
insurance.auto.processed.outbound.source.directory=files/outbound/auto-insurance/processed
insurance.life.ibm.raw.inbound.source.directory=files/inbound/life-insurance/ibm/raw
insurance.life.ibm.raw.inbound.schema.file=files/inbound/life-insurance/ibm/schemas/fixed-width-policy-schema.txt
insurance.life.ibm.processed.outbound.source.directory=files/outbound/life-insurance/ibm/processed
```

## Usage

### File Processing
1. **Add input files** to `files/inbound/life-insurance/raw/`
2. **Start the application** - files are automatically processed
3. **Check output** in `files/outbound/life-insurance/processed/`

### API Usage
1. **Start the application**
2. **Send HTTP requests** to the API endpoints
3. **Receive structured JSON responses**

### Supported File Formats
- **Unstructured Text**: Policy information in natural language format
- **IBM COBOL Fixed-Width**: Mainframe format with positional data fields

## Data Samples

### Input Sample (Unstructured Text)
```
Policy Holder Information:
Name: John Smith
Gender: Male
Date of Birth: December 25, 1985
SSN: 123-45-6789
Policy Number: INS-2024-001
Policy Type: Life Insurance
Premium: $250.00
Status: Active
```

### Output Sample (AI-Processed JSON)
```json
{
  "policyNumber": "INS-2024-001",
  "firstName": "John",
  "lastName": "Smith",
  "gender": "M",
  "dob": "12/25/1985",
  "ssn": "123-45-6789"
}
```

### IBM COBOL Format Sample
Complex fixed-width format with comprehensive policy details, beneficiary information, and financial data (see `files/inbound/life-insurance/ibm/raw/FIA653487.txt`).

## AI Models & Prompts

### Supported Models
1. **GPT-4.1 ** (Default) - Best overall performance
2. **GPT-4.1 Nano** - Speed-optimized for high-volume processing
3. **GPT-5 Mini2** - Advanced language understanding
4. **DeepSeek-R1** - Alternative specialized model

### Model Switching
Change the current model by updating `CURRENT_MODEL` in `AzureOpenAIUtils.java`:
```java
public static final String CURRENT_MODEL = GPT41MYAGENT; // Change this
```

### Prompt Optimization
- **Optimized IBM Prompt**: Streamlined for faster fixed-width data processing
- **General Policy Prompt**: Comprehensive extraction for unstructured data
- **Field-Specific Rules**: Date formatting, gender normalization, numeric parsing

## Performance Features

- **Intelligent Caching**: 30-minute TTL for API responses
- **Connection Pooling**: Optimized HTTP connections with keep-alive
- **Timeout Management**: Configurable request and connection timeouts
- **TCP Optimization**: No-delay and address reuse for better network performance
- **Parallel Processing**: Support for concurrent file processing
- **Memory Management**: Efficient JSON processing and response handling

## Development

### Running Tests
```bash
./mvnw test
```

### Code Structure
- **Clean Architecture**: Separation of concerns with dedicated packages
- **Utility Classes**: Reusable components for AI integration
- **Configuration Management**: Externalized settings in properties files
- **Error Handling**: Comprehensive exception management and logging

### Adding New AI Models
1. Add model configuration to `AZURE_AI_MODELS_DIC` in `AzureOpenAIUtils.java`
2. Update endpoint URIs and authentication headers
3. Test with sample data to ensure compatibility

### Extending File Formats
1. Create new prompt templates in `AIPromptConstants.java`
2. Add processing logic in service classes
3. Update configuration for new input/output directories

## Troubleshooting

### Common Issues

**Application fails to start**:
- Verify Java 25 is installed and JAVA_HOME is set
- Check if ports 9080 and 9081 are available
- Ensure all Maven dependencies are downloaded

**Azure OpenAI API errors**:
- Verify AZURE_OPENAI_API_KEY environment variable is set
- Check model deployment names match configuration
- Ensure sufficient API quota and rate limits

**File processing not working**:
- Verify input directory permissions
- Check file format matches expected structure
- Review application logs for processing errors

**API endpoints returning errors**:
- Ensure application is fully started (check logs)
- Verify policy files exist in expected directories
- Check network connectivity and firewall settings

### Logging
Enable debug logging by adding to `application.properties`:
```properties
logging.level.custom_integration_services=DEBUG
logging.level.org.apache.camel=INFO
```

### Performance Tuning
- Adjust cache TTL based on data freshness requirements
- Optimize AI model selection based on accuracy vs. speed needs
- Configure connection pool sizes for expected load

---

**Project Status**: Active Development  
**Version**: 0.0.1-SNAPSHOT  
**License**: Proprietary  
**Author**: Enterprise Integration Team

---

## Contributing
Feel free to contribute to this PoC by submitting issues, feature requests, or pull requests.

## License
This project is for demonstration purposes and internal use only.

---
*Generated on: December 2024*  
*Version: 2.0.0*  
*Author: Enterprise Integration Team*