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
    public class Applist {
        private List<Game> apps;
    }
}
