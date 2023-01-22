package sggc.models.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import sggc.models.service.error.Error;

import java.util.List;

@Data
public abstract class Result<T> {
    private boolean success;
    private T data;
    private List<Error> errorList;

    public Result(boolean success, T data, List<Error> errorList) {
        this.success = success;
        this.data = data;
        this.errorList = errorList;
    }
}
