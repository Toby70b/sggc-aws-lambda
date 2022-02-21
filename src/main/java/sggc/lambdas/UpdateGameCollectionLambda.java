package sggc.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import sggc.exceptions.SecretRetrievalException;
import sggc.models.Game;
import sggc.models.GetAppListResponse;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import sggc.utils.DynamoDbUtil;
import sggc.utils.SteamAPIUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a lambda function to be run by AWS Lambda
 */
public class UpdateGameCollectionLambda implements RequestStreamHandler {

    private static final String GAME_TABLE_NAME = "Game";

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Creating DynamoDB client");
        DynamoDbEnhancedClient enhancedClient = DynamoDbUtil.createDynamoDbEnhancedClient();
        logger.log("DynamoDB client created");
        DynamoDbTable<Game> gameTable = enhancedClient.table(GAME_TABLE_NAME, TableSchema.fromBean(Game.class));
        logger.log("Retrieving all persisted games via scan");
        Set<Game> persistedGames = gameTable.scan().items().stream().collect(Collectors.toSet());
        logger.log("Persisted games retrieved");
        Set<Game> allSteamGames = new HashSet<>();
        logger.log("Contacting the Steam API for a Set of games");
        try {
            allSteamGames = requestAllGamesFromSteam();
        } catch (Exception e) {
            logger.log("Exception occurred while contacting the Steam API [" + e + "]");
            System.exit(1);
        }
        logger.log("All games retrieved from Steam API games");
        Set<Game> newGames = getNonPersistedGames(persistedGames, allSteamGames);
        newGames.forEach(game -> game.setId(UUID.randomUUID() + "-" + new Date().toInstant().toEpochMilli()));
        logger.log("New games filtered, attempting to persists [" + newGames.size() + "] games");
        DynamoDbUtil.batchWrite(Game.class, newGames, enhancedClient, gameTable);
        logger.log("Save successful");
    }

    /**
     * Given two Sets, a Set of all games and a Set of games determined to be already persisted,
     * returns a Set of any non-persisted
     *
     * @param persistedGames a Set of games determined to already by persisted
     * @param allGames       a Set of all games currently on Steam
     * @return a Set of non-persisted games
     */
    private Set<Game> getNonPersistedGames(Set<Game> persistedGames, Set<Game> allGames) {new HashSet<>(new HashSet<>(allGames));
        return allGames.stream().filter(game -> !persistedGames.contains(game)).collect(Collectors.toSet());
    }

    /**
     * Sends a request to the Steam API to retrieve a Set of all games currently stored on the platform
     * @return a Set of all games currently stored by Steam's API
     * @throws SecretRetrievalException if an error occurs trying to retrieve the Steam API Key from AWS secrets manager
     * @throws IOException if an error occurs sending or receiving the request from the Steam API
     * @throws IllegalArgumentException if the parsed response from the Steam API isn't as expected
     */
    public Set<Game>  requestAllGamesFromSteam() throws SecretRetrievalException, IOException, IllegalArgumentException {
        GetAppListResponse getGamesResponse = SteamAPIUtil.requestAllSteamAppsFromSteamApi();
        if (getGamesResponse != null) {
            return getGamesResponse.getApplist().getApps();
        } else {
            throw new IllegalArgumentException("Parsed response from Steam API was null");
        }
    }
}