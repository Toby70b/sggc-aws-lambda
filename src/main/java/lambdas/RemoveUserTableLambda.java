package lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.MongoSettings;
import org.bson.Document;
import utils.MongoConnectionManager;
import utils.ResourceFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoveUserTableLambda implements RequestStreamHandler {

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
            MongoDatabase database = mongoClient.getDatabase(settings.getDatabaseName());
            MongoCollection<Document> user = database.getCollection(settings.getUserCollectionName());
            user.drop();
            logger.log("User table successfully dropped\n");
        } catch (Exception e) {
            logger.log("Error while dropping user table\n");
            e.printStackTrace();
        }
    }

}