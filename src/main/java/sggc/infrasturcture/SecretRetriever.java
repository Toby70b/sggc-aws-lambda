package sggc.infrasturcture;

/**
 * Interface for retrieving secrets from an external store.
 */
public interface SecretRetriever {

    /**
     * Retrieves a secret from an external store.
     *
     * @param secretKey the key of the secret to retrieve.
     * @return the specified key, or null if none could be found.
     */
    String getSecretValue(String secretKey);
}
