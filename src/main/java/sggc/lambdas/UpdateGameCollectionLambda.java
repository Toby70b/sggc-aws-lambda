package sggc.lambdas;

import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sggc.exceptions.ApiException;
import sggc.exceptions.SecretRetrievalException;
import sggc.factories.AWSSecretsManagerClientFactory;
import sggc.factories.DynamoDbEnhancedClientFactory;
import sggc.infrastructure.AwsSecretRetriever;
import sggc.infrastructure.DynamoDbBatchWriter;
import sggc.infrastructure.SteamRequestSender;
import sggc.models.Game;
import sggc.models.GetAppListResponse;
import sggc.services.GameService;
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
@Log4j2
public class UpdateGameCollectionLambda {

    private static final String GAME_TABLE_NAME = "Game";

    public void handleRequest() {
        log.debug("Creating DynamoDB client");
        DynamoDbEnhancedClient enhancedClient = new DynamoDbEnhancedClientFactory().createEnhancedClient();
        log.debug("DynamoDB client created");
        DynamoDbTable<Game> gameTable = enhancedClient.table(GAME_TABLE_NAME, TableSchema.fromBean(Game.class));
        log.info("Retrieving all persisted games via scan");
        Set<Game> persistedGames = gameTable.scan().items().stream().collect(Collectors.toSet());
        log.debug("Persisted games retrieved");

        AwsSecretRetriever secretRetriever = new AwsSecretRetriever(new AWSSecretsManagerClientFactory().createClient());
        SteamRequestSender steamRequestSender = new SteamRequestSender(secretRetriever);
        GameService gameService = new GameService(steamRequestSender);
        Set<Game> allSteamGames = gameService.requestAllGamesFromSteam();

        if(allSteamGames == null){
            log.error("Could not retrieve list of all Steam games, exiting");
            System.exit(1);
        }

        log.debug("All games retrieved from Steam API games");
        Set<Game> newGames = getNonPersistedGames(persistedGames, allSteamGames);
        for (Game game : newGames) {
            game.setId(UUID.randomUUID() + "-" + new Date().toInstant().toEpochMilli());
            game.setMultiplayer(gameService.isGameMultiplayer(game));
        }
        log.info("New games filtered, attempting to persist [{}] games", newGames.size());
        DynamoDbBatchWriter dynamoDbBatchWriter = new DynamoDbBatchWriter(enhancedClient);
        dynamoDbBatchWriter.batchWrite(Game.class, newGames, gameTable);
        log.info("Save successful");
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

}
