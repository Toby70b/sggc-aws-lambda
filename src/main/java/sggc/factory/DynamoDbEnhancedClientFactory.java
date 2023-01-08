package sggc.factory;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Represents a factory for creating enhanced clients to interact with an Amazon DynamoDB instance.
 */
public class DynamoDbEnhancedClientFactory {

    /**
     * Initializes a new {@link DynamoDbEnhancedClient} object; A client for interacting with an Amazon DynamoDB instance.
     *
     * @param region the region that the Amazon DynamoDB instance resides in.
     * @return a new client for interacting with a local Amazon DynamoDB instance.
     */
    public DynamoDbEnhancedClient createDynamoDbEnhancedClient(Region region) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(region)
                .build();
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
