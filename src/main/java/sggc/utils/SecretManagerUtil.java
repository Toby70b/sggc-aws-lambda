package sggc.utils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 *
 * Utility class containing methods for interacting with AWS Secrets Manager
 */
public class SecretManagerUtil {

    /**
     * Creates a new instance of the AWS Secrets Manager client to perform actions on AWS secrets
     * @return a new instance of the AWS Secrets Manager client
     */
    public static SecretsManagerClient createSecretManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.EU_WEST_2)
                .build();
    }

    /**
     * Retrieves the String value of a secret stored in AWS Secrets Manager
     * @param secretsManagerClient the client with which to retrieve the secret
     * @param secretName the name of the secret to retrieve
     * @return a String value of the secret
     */
    public static String getSecretValue(SecretsManagerClient secretsManagerClient, String secretName) {
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
        return valueResponse.secretString();
    }
}
