package sggc.lambdas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sggc.exceptions.SecretRetrievalException;
import sggc.factory.DynamoDbEnhancedClientFactory;
import sggc.infrasturcture.DynamoDbBatchWriter;
import sggc.models.Game;
import sggc.models.GetAppListResponse;
import sggc.utils.SteamAPIUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class representing a lambda function scheduled to run on AWS Lambda via  CRON timer every day at midnight 
 * to update the SGGC's 'Game' DynamoDB Table with new Games added to Steam over the previous day.
 */
public class UpdateGameCollectionLambda {

    private static final String GAME_TABLE_NAME = "Game";
    private static final Logger logger = LoggerFactory.getLogger(UpdateGameCollectionLambda.class);

    public void handleRequest() {

        String region = System.getenv("REGION");

        logger.debug("Creating DynamoDB client");
        DynamoDbEnhancedClient enhancedClient =
                new DynamoDbEnhancedClientFactory().createDynamoDbEnhancedClient(Region.of(region));
        logger.debug("DynamoDB client created");
        DynamoDbTable<Game> gameTable = enhancedClient.table(GAME_TABLE_NAME, TableSchema.fromBean(Game.class));
        logger.info("Retrieving all persisted games via scan");
        Set<Game> persistedGames = gameTable.scan().items().stream().collect(Collectors.toSet());
        logger.debug("Persisted games retrieved");
        Set<Game> allSteamGames = new HashSet<>();
        logger.info("Contacting the Steam API for a Set of games");
        try {
            allSteamGames = requestAllGamesFromSteam();
        } catch (Exception e) {
            logger.error("Exception occurred while contacting the Steam API [" + e + "]");
            System.exit(1);
        }
        logger.debug("All games retrieved from Steam API games");
        Set<Game> newGames = getNonPersistedGames(persistedGames, allSteamGames);
        newGames.forEach(game -> game.setId(UUID.randomUUID() + "-" + new Date().toInstant().toEpochMilli()));
        logger.info("New games filtered, attempting to persists [" + newGames.size() + "] games");
        DynamoDbBatchWriter dynamoDbBatchWriter = new DynamoDbBatchWriter();
        dynamoDbBatchWriter.batchWrite(Game.class, newGames, enhancedClient, gameTable);
        logger.info("Save successful");
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
