package sggc.models.service;

public class SuccessResult<T> extends Result<T> {

    public SuccessResult(T data) {
        super(true, data, null);
    }
}
