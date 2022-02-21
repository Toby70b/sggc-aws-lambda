package sggc.exceptions;

/**
 * Exception to be thrown when an error occurs trying to retrieve a secret from the AWS Secrets manager
 */
public class SecretRetrievalException extends Exception {
    private static final String EXCEPTION_MESSAGE = "Exception occurred when attempting to retrieve a secret from AWS secrets manager";

    public SecretRetrievalException(Throwable cause) {
        super(EXCEPTION_MESSAGE,cause);
    }

    public SecretRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}

