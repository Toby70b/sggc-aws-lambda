package sggc.models.service;

import sggc.models.service.error.Error;

import java.util.List;

public class ErrorResult<T> extends Result<T> {

    public ErrorResult(List<Error> errorList) {
        super(false, null, errorList);
    }
}
