package sggc.lambdas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sggc.exceptions.SecretRetrievalException;
import sggc.factory.AWSSecretsManagerClientFactory;
import sggc.factory.DynamoDbEnhancedClientFactory;
import sggc.infrasturcture.AwsSecretRetriever;
import sggc.infrasturcture.DynamoDbBatchWriter;
import sggc.infrasturcture.SteamRequestSender;
import sggc.models.Game;
import sggc.models.GetAppListResponse;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.io.IOException;
import java.net.URISyntaxException;
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
        logger.debug("Creating DynamoDB client");
        DynamoDbEnhancedClient enhancedClient = new DynamoDbEnhancedClientFactory().createEnhancedClient();
        logger.debug("DynamoDB client created");
        DynamoDbTable<Game> gameTable = enhancedClient.table(GAME_TABLE_NAME, TableSchema.fromBean(Game.class));
        logger.info("Retrieving all persisted games via scan");
        Set<Game> persistedGames = gameTable.scan().items().stream().collect(Collectors.toSet());
        logger.debug("Persisted games retrieved");
        Set<Game> allSteamGames = requestAllGamesFromSteam();

        if(allSteamGames == null){
            logger.error("Could not retrieve list of all Steam games, exiting");
            System.exit(1);
        }

        logger.debug("All games retrieved from Steam API games");
        Set<Game> newGames = getNonPersistedGames(persistedGames, allSteamGames);
        newGames.forEach(game -> game.setId(UUID.randomUUID() + "-" + new Date().toInstant().toEpochMilli()));
        logger.info("New games filtered, attempting to persist [{}] games", newGames.size());
        DynamoDbBatchWriter dynamoDbBatchWriter = new DynamoDbBatchWriter(enhancedClient);
        dynamoDbBatchWriter.batchWrite(Game.class, newGames, gameTable);
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
    private Set<Game> getNonPersistedGames(Set<Game> persistedGames, Set<Game> allGames) {
        new HashSet<>(new HashSet<>(allGames));
        return allGames.stream().filter(game -> !persistedGames.contains(game)).collect(Collectors.toSet());
    }

    /**
     * Sends a request to the Steam API to retrieve a Set of all games currently stored on the platform
     *
     * @return a Set of all games currently stored by Steam's API. Returns null if games cannot be retrieved
     */
    public Set<Game> requestAllGamesFromSteam()  {
        logger.info("Contacting the Steam API for a Set of games");
        AwsSecretRetriever secretRetriever = new AwsSecretRetriever(new AWSSecretsManagerClientFactory().createClient());
        SteamRequestSender steamRequestSender = new SteamRequestSender(secretRetriever);
        try {
            GetAppListResponse getGamesResponse = steamRequestSender.requestAllSteamAppsFromSteamApi();
            return getGamesResponse.getApplist().getApps();
        } catch (IOException e) {
            logger.error("Error occurred during the request to Steam API.", e);
            return null;
        } catch (SecretRetrievalException e) {
            logger.error("Error occurred retrieving secret from the external secret store.", e);
            return null;
        } catch (URISyntaxException e) {
            logger.error("Error occurred constructing request URI for the request to Steam API.", e);
            return null;
        }
    }
}
