package models;

import lombok.Data;

@Data
public class MongoSettings {
    private String databaseName;
    private String collectionName;
    private String connectionString;
}