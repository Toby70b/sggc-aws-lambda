package sggc.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sggc.exceptions.ApiException;
import sggc.infrastructure.SteamRequestSender;
import sggc.models.Game;
import sggc.models.service.ErrorResult;
import sggc.models.service.Result;
import sggc.models.service.SuccessResult;
import sggc.models.service.error.Error;
import sggc.models.service.error.ErrorType;
import sggc.models.steam.GameCategory;
import sggc.models.steam.GameData;
import sggc.models.steam.GetAppListResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Represents a service for containing business logic related to Game objects.
 */
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final SteamRequestSender steamRequestSender;

    /**
     * Determines whether the provided game is considered by Steam to be multiplayer.
     *
     * @param game the game to check
     * @return A {@link SuccessResult} object containing the game's multiplayer status. If instead an error was
     * encountered an {@link ErrorResult} object containing details on the error.
     */
    public Result<Boolean> isGameMultiplayer(Game game) {
        log.debug("Attempting to determine whether game [{}] is multiplayer.", game.getAppid());
        GameData parsedResponse;
        try {
            parsedResponse = steamRequestSender.getAppDetails(game.getAppid());
        } catch (ApiException | IOException e) {
            String errorMessage = "Error encountered when trying to determine game's multiplayer status.";
            log.error(errorMessage);
            return new ErrorResult<>(List.of(new Error(ErrorType.EXCEPTION_ENCOUNTERED,
                    errorMessage)));
        }
        //Check for presence of multiplayer category
        for (GameCategory category : parsedResponse.getCategories()) {
            if (category.getId() == GameCategory.SteamGameCategory.MULTIPLAYER) {
                log.debug("Game [{}] is multiplayer.", game.getAppid());
                return new SuccessResult<>(true);
            }
        }
        log.debug("Game [{}] is not multiplayer.", game.getAppid());
        return new SuccessResult<>(false);
    }

    /**
     * Sends a request to the Steam API to retrieve a Set of all games currently stored on the platform.
     *
     * @return A {@link SuccessResult} object containing all games currently stored on Steam. If instead an error was
     * encountered an {@link ErrorResult} object containing details on the error.
     */
    public Result<Set<Game>> requestAllGamesFromSteam() {
        log.info("Contacting the Steam API for a Set of games.");
        try {
            GetAppListResponse getGamesResponse = steamRequestSender.getListOfAllSteamGames();
            return new SuccessResult<>(getGamesResponse.getApplist().getApps());
        } catch (IOException | ApiException ex) {
            String logMessage = "Error occurred during the request to Steam API.";
            log.error(logMessage, ex);
            return new ErrorResult<>(List.of(new Error(ErrorType.EXCEPTION_ENCOUNTERED,
                    logMessage)));
        }
    }
}
