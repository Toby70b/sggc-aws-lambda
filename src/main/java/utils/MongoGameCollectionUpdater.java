package utils;

import com.mongodb.client.MongoCollection;
import lombok.Data;
import models.Game;
import models.GetAppListResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


@Data
public class MongoGameCollectionUpdater {

    public void refreshGamesCollection(MongoCollection<Game> gamesCollection, GetAppListResponse allSteamApps) throws NoSuchElementException {
        ArrayList<Game> storedGames = gamesCollection.find().into(new ArrayList<>());
        //logger.log(String.format("%s stored games retrieved from db\n", storedGames.size()));
        if (allSteamApps != null) {
            List<Game> allGames = allSteamApps.getApplist().getApps();
            allGames.removeAll(storedGames);
            if (allGames.isEmpty()) {
               // logger.log("No new games to insert, collection is up to date\n");
            } else {
                gamesCollection.insertMany(allSteamApps.getApplist().getApps());
               // logger.log(String.format("%s new games successfully saved\n", allGames.size()));
            }
        } else {
            throw new NoSuchElementException("Error, Steam request returned no games\n");
        }
    }
}
