package sggc.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sggc.exceptions.ApiException;
import sggc.exceptions.SecretRetrievalException;
import sggc.factories.AWSSecretsManagerClientFactory;
import sggc.infrastructure.AwsSecretRetriever;
import sggc.infrastructure.SteamRequestSender;
import sggc.models.Game;
import sggc.models.GameCategory;
import sggc.models.GameData;
import sggc.models.GetAppListResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Represents a service for containing business logic related to Game objects.
 */
@Log4j2
@RequiredArgsConstructor
public class GameService {

    private final SteamRequestSender steamRequestSender;

    /**
     * Determines whether the provided game is considered by Steam to be multiplayer.
     *
     * @param game the game to check
     * @return true if the game is multiplayer otherwise false. Returns null if the game's multiplayer status could
     * not be determined.
     */
    public Boolean isGameMultiplayer(Game game)  {
        log.debug("Attempting to determine whether game [{}] is multiplayer", game.getAppid());
        GameData parsedResponse = null;
        try {
            parsedResponse = steamRequestSender.requestAppDetails(game.getAppid());
        } catch (ApiException | IOException e) {
            log.error("Error encountered when trying to determine game's multiplayer status.");
            return null;
        }
        //Check for presence of multiplayer category
        for (GameCategory category : parsedResponse.getCategories()) {
            if (category.getId() == GameCategory.SteamGameCategory.MULTIPLAYER) {
                log.debug("Game [{}] is multiplayer", game.getAppid());
                return true;
            }
        }
        log.debug("Game [{}] is not multiplayer", game.getAppid());
        return false;
    }

    /**
     * Sends a request to the Steam API to retrieve a Set of all games currently stored on the platform
     *
     * @return a Set of all games currently stored by Steam's API. Returns null if games could not be retrieved
     */
    public Set<Game> requestAllGamesFromSteam() {
        log.info("Contacting the Steam API for a Set of games");
        try {
            GetAppListResponse getGamesResponse = steamRequestSender.requestAllSteamAppsFromSteamApi();
            return getGamesResponse.getApplist().getApps();
        } catch (IOException | ApiException ex) {
            log.error("Error occurred during the request to Steam API.", ex);
            return null;
        }
    }
}
