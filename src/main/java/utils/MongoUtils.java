package utils;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import models.Game;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Data
public class MongoUtils {
    private final MongoClient client;
    private final String databaseName;

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> deserializingClass){
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase database = client.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
        return database.getCollection(collectionName, deserializingClass);
    }
}
