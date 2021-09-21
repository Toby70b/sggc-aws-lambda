package models;

import lombok.Data;

@Data
public class MongoSettings {
    private String databaseName;
    private String gameCollectionName;
    private String userCollectionName;
    private String connectionString;
}