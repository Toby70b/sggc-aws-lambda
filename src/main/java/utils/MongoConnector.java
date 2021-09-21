package utils;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoConnector {

    public static MongoClient connectToMongo(String connectionString) {
        ConnectionString mongoConnectionString = new ConnectionString(connectionString);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(mongoConnectionString)
                .retryWrites(true)
                .build();
        return MongoClients.create(mongoClientSettings);
    }
}
