package lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.Game;
import models.GetAppListResponse;
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

    public static final String MONGO_SETTINGS_FILE_PATH = "/mongoSettings.json";

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
                    getGameMongoCollection(mongoClient, settings.getDatabaseName(), settings.getCollectionName());
            ArrayList<Game> storedGames = gamesCollection.find().into(new ArrayList<>());
            logger.log(String.format("%s stored games retrieved from db\n", storedGames.size()));
            GetAppListResponse allSteamApps = SteamRequestHandler.requestAllSteamAppsFromSteamApi();
            if (allSteamApps != null) {
                List<Game> allGames = allSteamApps.getApplist().getApps();
                allGames.removeAll(storedGames);
                if(allGames.isEmpty()){
                    logger.log("No new games to insert, collection is up to date\n");
                }
                else {
                    gamesCollection.insertMany(allSteamApps.getApplist().getApps());
                    logger.log(String.format("%s new games successfully saved\n", allGames.size()));
                }
            } else {
                throw new NoSuchElementException("Error, Steam request returned no games\n");
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

}