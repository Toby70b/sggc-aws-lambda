package sggc.infrasturcture;

import sggc.exceptions.SecretRetrievalException;

/**
 * Interface for retrieving secrets from an external store.
 */
public interface SecretRetriever {

    /**
     * Retrieves a secret from an external store.
     *
     * @param secretKey the key of the secret to retrieve.
     * @return the specified key. Will return null if no secret with a matching key could be found, or an error occurs
     * while attempting to retrieve the secret.
     */
    String getSecretValue(String secretKey) throws SecretRetrievalException;
}
