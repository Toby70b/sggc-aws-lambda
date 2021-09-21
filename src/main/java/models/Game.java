package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;

@Data
public class Game {
    @JsonIgnore
    private ObjectId id;
    private String appid;
    private String name;
    @JsonIgnore
    private Boolean multiplayer;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        return appid.equalsIgnoreCase(((Game) o).appid);
    }

}
