package sggc.models.steam;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a model of a Steam Game's category e.g. multiplayer, coop, workshop support etc.
 */
@RequiredArgsConstructor
@Data
public class GameCategory {

    private final SteamGameCategory id;

    public static enum SteamGameCategory {
        @SerializedName("1")
        MULTIPLAYER(1),
        @SerializedName("2")
        SINGLE_PLAYER(2);

        private final int id;

        SteamGameCategory(int id) {
            this.id = id;
        }
    }
}
