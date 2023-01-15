package sggc.factories;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Represents a factory for creating enhanced clients to interact with an Amazon DynamoDB instance.
 */
public class DynamoDbEnhancedClientFactory {

    /**
     * Initializes a new {@link DynamoDbEnhancedClient} object; A client for interacting with an Amazon DynamoDB instance.
     * The region of the client is determined by the 'REGION' environment variable
     *
     * @return a new client for interacting with a local Amazon DynamoDB instance.
     */
    public DynamoDbEnhancedClient createEnhancedClient() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .build();
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
