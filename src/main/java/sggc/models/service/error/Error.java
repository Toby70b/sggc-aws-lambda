package sggc.models.service.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Error {
    private ErrorType type;
    private String message;
}
