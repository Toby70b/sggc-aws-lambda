package lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.Game;
import models.MongoSettings;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import utils.MongoConnectionManager;
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
            settings = resourceFileUtils.deserializeJsonResourceFileIntoObject(MONGO_SETTINGS_FILE_PATH,MongoSettings.class);
        } catch (IOException exception) {
            logger.log("Error reading Mongo settings from file\n");
            throw exception;
        }
        logger.log("Successfully read from Mongo settings file:\n");
        logger.log(settings.toString());
        logger.log("Attempting connection to MongoDB Cluster\n");

        try (MongoClient mongoClient = MongoConnectionManager.connectToMongo(settings.getConnectionString())) {
            logger.log("Connection Established");
            MongoCollection<Game> gamesCollection =
                    getGameMongoCollection(mongoClient, settings.getDatabaseName(), settings.getGameCollectionName());
            ArrayList<Game> storedGames = gamesCollection.find().into(new ArrayList<>());
            logger.log(String.format("%s stored games retrieved from db\n", storedGames.size()));
            List<Game> allSteamGames = SteamRequestHandler.requestAllSteamAppsFromSteamApi().getApplist().getApps();
            List<Game> filteredGames = filterExistingGamesOutOfList(allSteamGames,storedGames);
            if(filteredGames.isEmpty()){
                logger.log("No new games to insert, collection is up to date\n");
            }
            else {
                gamesCollection.insertMany(filteredGames);
                logger.log(String.format("%s new games successfully saved\n", filteredGames.size()));
            }


        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
    }

    private static MongoCollection<Game> getGameMongoCollection(MongoClient mongoClient, String databaseName, String collectionName) {
        CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase database = mongoClient.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
        return database.getCollection(collectionName, Game.class);
    }

    public List<Game> filterExistingGamesOutOfList(List<Game> newGamesList, List<Game> existingGamesList) {
        if(newGamesList == null || newGamesList.isEmpty()){
            throw new NoSuchElementException("Error, Steam request returned no games");
        }
        else {
            newGamesList.removeAll(existingGamesList);
            return newGamesList;
        }
    }

}