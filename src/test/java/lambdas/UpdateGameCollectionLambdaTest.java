package lambdas;

import models.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdateGameCollectionLambdaTest {
    UpdateGameCollectionLambda updateGameCollectionLambda;

    @BeforeEach
    public void setUp() {
        updateGameCollectionLambda = new UpdateGameCollectionLambda();
    }

    @Test
    @DisplayName("If the list of steamGames is null, throw NoSuchElementException")
    public void itShouldThrowAnExceptionIfSteamGamesIsNull() {
        assertThrows(NoSuchElementException.class, () -> updateGameCollectionLambda.filterExistingGamesOutOfList(null, null));
    }

    @Test
    @DisplayName("If the list of steamGames is empty, throw NoSuchElementException")
    public void itShouldThrowAnExceptionIfSteamGamesIsEmpty() {
        assertThrows(NoSuchElementException.class, () -> updateGameCollectionLambda.filterExistingGamesOutOfList(new ArrayList<>(), null));
    }

    @Test
    @DisplayName("It should remove every game from the new list if a game with a matching appId is within the existing list")
    public void itShouldRemoveExistingGamesFromNewGamesList() {
        Game exampleGame1 = new Game();
        Game exampleGame2 = new Game();
        Game exampleGame3 = new Game();

        exampleGame1.setAppid("01");
        exampleGame2.setAppid("02");
        exampleGame3.setAppid("03");

        List<Game> newList = new ArrayList<>(Arrays.asList(exampleGame1, exampleGame2,exampleGame3));
        List<Game> existingList = new ArrayList<>(Arrays.asList(exampleGame1, exampleGame2));

        List<Game> filteredList = updateGameCollectionLambda.filterExistingGamesOutOfList(newList,existingList);
        assertEquals(List.of(exampleGame3),filteredList);
    }

    @Test
    @DisplayName("If every game in the new list game is contained within the existing list it should return an empty list")
    public void itShouldReturnAnEmptyListIfBothListsContainTheSameGames() {
        Game exampleGame1 = new Game();
        Game exampleGame2 = new Game();
        Game exampleGame3 = new Game();

        exampleGame1.setAppid("01");
        exampleGame2.setAppid("02");
        exampleGame3.setAppid("03");

        List<Game> newList = new ArrayList<>(Arrays.asList(exampleGame1, exampleGame2,exampleGame3));
        List<Game> existingList = new ArrayList<>(Arrays.asList(exampleGame1, exampleGame2,exampleGame3));

        List<Game> filteredList = updateGameCollectionLambda.filterExistingGamesOutOfList(newList,existingList);
        assertEquals(new ArrayList<>(),filteredList);
    }

}