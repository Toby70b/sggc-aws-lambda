package sggc.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Represents a list of Steam applications
 */
@Data
@RequiredArgsConstructor
public class Applist {
    private Set<sggc.models.Game> apps;
}
