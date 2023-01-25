package sggc.factories;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Represents a factory for creating clients to interact with a local AWS Secrets Manager instance.
 */
public class AWSSecretsManagerClientFactory {

    /**
     * Initializes a new {@link SecretsManagerClient} object; A client for interacting with an AWS Secrets Manager instance.
     * The region of the client is determined by the 'REGION' environment variable.
     *
     * @return a new client for interacting with a local AWS Secrets Manager instance.
     */
    public SecretsManagerClient createClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .build();
    }
}
