package lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.MongoSettings;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import utils.MongoConnectionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RemoveUserTableLambda implements RequestStreamHandler {

    public static final String MONGO_SETTINGS_FILE_PATH = "/mongoSettings.json";

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        MongoSettings settings;
        logger.log("Searching for Mongo settings file\n");
        try {
            settings = getMongoSettings();
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

    public MongoSettings getMongoSettings() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File mongoSettingsFile = getResourceFile();
        return mapper.readValue(mongoSettingsFile, MongoSettings.class);
    }

    private File getResourceFile() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(MONGO_SETTINGS_FILE_PATH);
        File tempFile = File.createTempFile("mongoSettingsTemp", ".json");
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
        tempFile.deleteOnExit();
        return tempFile;
    }
}