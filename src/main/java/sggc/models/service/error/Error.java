package sggc.models.service.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents the details of an error being returned from a service method call.
 */
@Data
@AllArgsConstructor
public class Error {
    private ErrorType type;
    private String message;
}
