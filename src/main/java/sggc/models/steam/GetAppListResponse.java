package sggc.models.steam;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a response from the Steam API's GetAppList endpoint.
 */
@Data
@RequiredArgsConstructor
public class GetAppListResponse {
    private Applist applist;
}
