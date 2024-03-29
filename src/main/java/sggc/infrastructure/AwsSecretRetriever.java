package sggc.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sggc.exceptions.SecretRetrievalException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Represents an interface for retrieving secrets required by the application from AWS Secret Manager.
 */
@Slf4j
@RequiredArgsConstructor
public class AwsSecretRetriever implements SecretRetriever {

    private final SecretsManagerClient client;

    /**
     * Retrieves a key from AWS secrets manager.
     *
     * @param secretKey the key of the secret to retrieve.
     * @return the specified key stored within AWS secrets manager, or null if none could be found.
     * @throws SecretRetrievalException if an exception is thrown when trying to retrieve a secret.
     */
    @Override
    public String getSecretValue(String secretKey) throws SecretRetrievalException {
        try {
            log.debug("Attempting to retrieve secret [{}] from AWS Secrets Manager.", secretKey);
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretKey)
                    .build();
            GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
            return valueResponse.secretString();
        } catch (Exception e) {
            log.error("Error occurred when retrieving secret [{}] from AWS Secrets Manager.", secretKey);
            throw new SecretRetrievalException(secretKey, e);
        }
    }
}
