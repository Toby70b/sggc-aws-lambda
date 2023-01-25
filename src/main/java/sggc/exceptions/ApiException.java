package sggc.exceptions;

/**
 * Represents a generic exception to be thrown when unexpected behaviour is encountered from an external API.
 */
public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
