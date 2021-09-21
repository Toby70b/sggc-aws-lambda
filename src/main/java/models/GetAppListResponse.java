package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
public class GetAppListResponse {
    private Applist applist;

    @Data
    private static class Applist {
         List<Game> apps;
    }
    
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
            Game game = (Game) o;
            return appid.equalsIgnoreCase(((Game) o).appid);
        }

    }
}
