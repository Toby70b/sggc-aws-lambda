package sggc.infrastructure;

import sggc.exceptions.SecretRetrievalException;

/**
 * Interface for retrieving secrets from an external store.
 */
public interface SecretRetriever {

    /**
     * Retrieves a secret from an external store.
     *
     * @param secretKey the key of the secret to retrieve.
     * @return value of the secret with a matching key.
     * @throws SecretRetrievalException if an exception is encountered when retrieving the secret.
     */
    String getSecretValue(String secretKey) throws SecretRetrievalException;
}
