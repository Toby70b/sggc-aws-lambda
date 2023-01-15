package sggc.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Represents a model of a GameData object returned by the Steam API.
 */
@Data
@RequiredArgsConstructor
public class GameData {
    private final Set<GameCategory> categories;

}
