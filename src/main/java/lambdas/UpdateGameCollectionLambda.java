package lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import models.Game;
import models.MongoSettings;
import utils.MongoConnector;
import utils.MongoUtils;
import utils.ResourceFileUtils;
import utils.SteamRequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class UpdateGameCollectionLambda implements RequestStreamHandler {

    public static final String MONGO_SETTINGS_FILE_PATH = "/MongoSettings.json";

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        MongoSettings settings;
        logger.log("Searching for Mongo settings file\n");
        ResourceFileUtils resourceFileUtils = new ResourceFileUtils();
        try {
            settings = resourceFileUtils.deserializeJsonResourceFileIntoObject(MONGO_SETTINGS_FILE_PATH, MongoSettings.class);
        } catch (IOException exception) {
            logger.log("Error reading Mongo settings from file\n");
            throw exception;
        }
        logger.log("Successfully read from Mongo settings file:\n");
        logger.log(settings.toString());
        logger.log("Attempting connection to MongoDB Cluster\n");

        try (MongoClient mongoClient = MongoConnector.connectToMongo(settings.getConnectionString())) {
            logger.log("Connection to MongoDB Established\n");
            MongoUtils mongoUtils = new MongoUtils(mongoClient, settings.getDatabaseName());
            MongoCollection<Game> gamesCollection = mongoUtils.getCollection(settings.getGameCollectionName(),Game.class);
            List<Game> storedGames = gamesCollection.find().into(new ArrayList<>());
            logger.log(String.format("%s stored games retrieved from db\n", storedGames.size()));
            List<Game> allSteamGames = requestAllGamesFromSteam(logger);
            List<Game> filteredGames = filterExistingGamesOutOfList(allSteamGames, storedGames);
            if (filteredGames.isEmpty()) {
                logger.log("No new games to insert, collection is up to date\n");
            } else {
                gamesCollection.insertMany(filteredGames);
                logger.log(String.format("%s new games successfully saved\n", filteredGames.size()));
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private List<Game> requestAllGamesFromSteam(LambdaLogger logger) throws IOException {
        List<Game> allSteamGames;
        try {
            allSteamGames = SteamRequestHandler.requestAllSteamAppsFromSteamApi().getApplist().getApps();
        } catch (IOException e) {
            logger.log("Exception occurred when requesting games from steam api");
            e.printStackTrace();
            throw e;
        }
        return allSteamGames;
    }


    public List<Game> filterExistingGamesOutOfList(List<Game> newGamesList, List<Game> existingGamesList) {
        if (newGamesList == null || newGamesList.isEmpty()) {
            throw new NoSuchElementException("Error, Steam request returned no games");
        } else {
            newGamesList.removeAll(existingGamesList);
            return newGamesList;
        }
    }

}