package sggc.models.service;

import sggc.models.service.error.Error;

import java.util.List;

/**
 * Represents a failed response to a request to a service method. Contains details on the nature of the failure.
 */
public class ErrorResult<T> extends Result<T> {

    public ErrorResult(List<Error> errorList) {
        super(false, null, errorList);
    }
}
