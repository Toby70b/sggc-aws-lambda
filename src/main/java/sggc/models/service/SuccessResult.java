package sggc.models.service;
/**
 * Represents a successful response to a request to a service method.
 */
public class SuccessResult<T> extends Result<T> {

    public SuccessResult(T data) {
        super(true, data, null);
    }
}
