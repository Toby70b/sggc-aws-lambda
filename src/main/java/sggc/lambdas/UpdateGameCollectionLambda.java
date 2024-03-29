package sggc.lambdas;

import lombok.extern.slf4j.Slf4j;
import sggc.factories.AWSSecretsManagerClientFactory;
import sggc.factories.DynamoDbEnhancedClientFactory;
import sggc.infrastructure.AwsSecretRetriever;
import sggc.infrastructure.DynamoDbBatchWriter;
import sggc.infrastructure.SteamRequestSender;
import sggc.models.Game;
import sggc.models.service.Result;
import sggc.services.GameService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class representing a lambda function to update the SGGC's DynamoDB Table with new Games added to Steam.
 */
@Slf4j
public class UpdateGameCollectionLambda {

    private static final String GAME_TABLE_NAME = "Game";

    /**
     * Entrypoint for the lambda function.
     */
    public void handleRequest() {
        log.debug("Creating DynamoDB client.");
        DynamoDbEnhancedClient enhancedClient = new DynamoDbEnhancedClientFactory().createEnhancedClient();
        DynamoDbTable<Game> gameTable = enhancedClient.table(GAME_TABLE_NAME, TableSchema.fromBean(Game.class));
        log.info("Retrieving all persisted games via scan.");
        Set<Game> persistedGames = gameTable.scan().items().stream().collect(Collectors.toSet());
        log.debug("Persisted games retrieved.");

        AwsSecretRetriever secretRetriever = new AwsSecretRetriever(new AWSSecretsManagerClientFactory().createClient());
        SteamRequestSender steamRequestSender = new SteamRequestSender(secretRetriever);
        GameService gameService = new GameService(steamRequestSender);

        log.info("Retrieving all from Steam API.");
        Result<Set<Game>> allSteamGamesResult = gameService.requestAllGamesFromSteam();
        Set<Game> allSteamGames = null;

        if (allSteamGamesResult.isSuccess() && allSteamGamesResult.getData() != null) {
            allSteamGames = allSteamGamesResult.getData();
        } else {
            log.error("Could not retrieve list of all Steam games, exiting.");
            System.exit(1);
        }

        log.debug("All games retrieved from Steam API games.");
        log.info("Filtering persisted games.");
        Set<Game> newGames = getNonPersistedGames(persistedGames, allSteamGames);
        for (Game game : newGames) {
            game.setId(UUID.randomUUID() + "-" + new Date().toInstant().toEpochMilli());

            log.debug("Attempting to determine multiplayer status of game [{}]", game.getAppid());
            Result<Boolean> multiplayerStatusResult = gameService.isGameMultiplayer(game);
            if (multiplayerStatusResult.isSuccess() && multiplayerStatusResult.getData() != null) {
                game.setMultiplayer(multiplayerStatusResult.getData());
            }
        }
        log.info("New games filtered, attempting to persist [{}] games.", newGames.size());
        DynamoDbBatchWriter dynamoDbBatchWriter = new DynamoDbBatchWriter(enhancedClient);
        dynamoDbBatchWriter.batchWrite(Game.class, newGames, gameTable);
        log.info("Save successful.");
    }

    /**
     * Given two Sets, a Set of all games and a Set of games determined to be already persisted, returns a Set of any
     * non-persisted games.
     *
     * @param persistedGames a Set of games determined to already by persisted.
     * @param allGames       a Set of all games currently on Steam.
     * @return a Set of non-persisted games.
     */
    private Set<Game> getNonPersistedGames(Set<Game> persistedGames, Set<Game> allGames) {
        new HashSet<>(new HashSet<>(allGames));
        return allGames.stream().filter(game -> !persistedGames.contains(game)).collect(Collectors.toSet());
    }

}
