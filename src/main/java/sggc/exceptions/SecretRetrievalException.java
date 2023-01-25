package sggc.exceptions;

/**
 * Represents an exception to be thrown when an error occurs trying to retrieve a secret from an external secrets store.
 */
public class SecretRetrievalException extends Exception {

    public SecretRetrievalException(String secretKey, Throwable cause) {
        super(String.format("Exception occurred when attempting to retrieve secret [%s] from an external secrets store."
                , secretKey), cause);
    }
}
