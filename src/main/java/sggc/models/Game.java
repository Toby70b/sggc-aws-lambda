package sggc.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Objects;

/**
 * Represents an application on Steam.
 */
@RequiredArgsConstructor
@DynamoDbBean
@Data
public class Game {
    private String id;
    private String appid;
    private String name;
    private Boolean multiplayer;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(appid, game.appid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appid);
    }
}
