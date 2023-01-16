package sggc.infrastructure;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import sggc.exceptions.ApiException;
import sggc.exceptions.SecretRetrievalException;
import sggc.models.GameCategory;
import sggc.models.GameData;
import sggc.models.GetAppListResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Represents an interface for communicating with the Steam API
 */
@Slf4j
public class SteamRequestSender {
    public static final String STEAM_API_KEY_MASK = "*************";

    public static final String STEAM_KEY_QUERY_PARAM_KEY = "key";
    public static final String STEAM_APP_IDS_QUERY_PARAM_KEY = "appids";

    public static final String STEAM_API_KEY_SECRET_ID = "SteamAPIKey";

    public static final String GET_APP_DETAILS_ENDPOINT = "/api/appdetails/";
    public static final String GET_APP_LIST_ENDPOINT = "/ISteamApps/GetAppList/v2/";

    private final SecretRetriever secretRetriever;

    private final String steamApiAddress;
    private final String steamStoreAddress;

    private final CloseableHttpClient httpClient;

    public SteamRequestSender(SecretRetriever secretRetriever) {
        this.secretRetriever = secretRetriever;

        steamApiAddress = System.getenv("STEAM_API_ADDRESS");
        steamStoreAddress = System.getenv("STEAM_STORE_ADDRESS");

        httpClient = HttpClients.createDefault();
    }

    /**
     * Retrieves a list of all games currently on Steam.
     *
     * @return an object containing a list of all games, parsed from the response from the Steam API.
     * @throws IOException  if an exception occurs when parsing the response into from the Steam API.
     * @throws ApiException if an unexpected event is encountered when requesting the games from the Steam API
     */
    public GetAppListResponse getListOfAllSteamGames() throws IOException, ApiException {
        URI requestUri;
        try {
            requestUri = steamApiRequest(GET_APP_LIST_ENDPOINT)
                    .build();
        } catch (URISyntaxException | SecretRetrievalException e) {
            throw new ApiException("Exception encountered when constructing request URI.", e);
        }

        HttpGet request = new HttpGet(requestUri);
        String jsonResponse;
        log.debug("Contacting [{}] to get list of all games on Steam.", sanitizeRequestUri(requestUri));

        try(CloseableHttpResponse response = httpClient.execute(request)){
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (response.getEntity() != null && response.getEntity().getContent() != null) {
                    jsonResponse = EntityUtils.toString(response.getEntity());
                } else {
                    throw new ApiException("Get App List response contained no response body.");
                }
            } else {
                throw new ApiException("Get App List request responded with a non-200 status code.");
            }
        }
        Gson gson = new Gson();
        return gson.fromJson(jsonResponse, GetAppListResponse.class);
    }

    /**
     * Retrieves the details of a specific game's details via the Steam Store's API
     *
     * @param appId the appid of the game whose details are being requested
     * @return a GameData object parsed from the response from the Steam API containing the details of the specified app
     * @throws ApiException if an unexpected event is encountered when requesting the details from the Steam API
     * @throws IOException  if an exception occurs when parsing the response from the Steam API
     */
    public GameData getAppDetails(String appId) throws ApiException, IOException {

        URI requestUri;
        try {
            requestUri = steamStoreRequest(GET_APP_DETAILS_ENDPOINT)
                    .addParameter(STEAM_APP_IDS_QUERY_PARAM_KEY, appId)
                    .build();
        } catch (URISyntaxException e) {
            throw new ApiException("Exception encountered when constructing request URI.", e);
        }

        HttpGet request = new HttpGet(requestUri);
        String jsonResponse;
        log.debug("Contacting [{}] to get details of game [{}].", requestUri, appId);

        try(CloseableHttpResponse response = httpClient.execute(request)){
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (response.getEntity() != null && response.getEntity().getContent() != null) {
                    jsonResponse = EntityUtils.toString(response.getEntity());
                } else {
                    throw new ApiException("Get App Details response contained no response body.");
                }
            } else {
                throw new ApiException("Get App Details response contained non-200 status code.");
            }
        } catch (IOException e) {
            throw new ApiException("Exception encountered when executing HTTP request.");
        }

        return parseGameDetailsList(jsonResponse);
    }

    /**
     * Parses the game details list from Steam GetAppDetails endpoint into a model object
     *
     * @param stringToParse the string to parse
     * @return a {@link GameData} object serialized from the response from the Steam API
     * @throws IOException if an error occurs while serializing the string into JSON
     */
    private GameData parseGameDetailsList(String stringToParse) throws IOException {
        JsonObject rootObject = getAppDetailsResponseRootObject(stringToParse);
        boolean responseSuccess = isAppDetailsResponseSuccessful(rootObject);
        /*
        Sometimes steam no longer has info on the Game Id e.g. 33910 ARMA II, this is probably because the devs of the games
        in question may have created a new steam product for the exact same game (demo perhaps?), so to avoid crashing if the game no longer
        has any details, we'll pass it through as a multiplayer game. Which is better than excluding games that could be multiplayer
        */
        if (!responseSuccess) {
            log.debug("Could not determine whether game was multiplayer. Will be treated as multiplayer.");
            return new GameData(Collections.singleton(new GameCategory(GameCategory.SteamGameCategory.MULTIPLAYER)));
        } else  {
            if (doesGameHaveCategories(rootObject)){
                log.debug("Could not determine whether game was multiplayer. Will be treated as multiplayer.");
                return new GameData(Collections.singleton(new GameCategory(GameCategory.SteamGameCategory.MULTIPLAYER)));
            }
        }
        String dataField = "data";
        rootObject = rootObject.getAsJsonObject(dataField);
        Gson gson = new Gson();
        return gson.fromJson(rootObject.toString(), GameData.class);
    }

    /**
     * Returns the 'root' object from the Steam store's Get App Details endpoint.
     * @param jsonString string representing the response from the endpoint.
     *
     * @return a Json object containing the details the Steam store contains on a game.
     * @throws IOException if an error occurs while serializing the string into JSON
     */
    public JsonObject getAppDetailsResponseRootObject(String jsonString) throws IOException {
        JsonElement jsonTree = parseResponseStringToJson(jsonString);
        JsonObject obj = jsonTree.getAsJsonObject();
        // The root of the response is an id of the game thus get the responses root value
        String gameId = obj.keySet().iterator().next();
        return obj.getAsJsonObject(gameId);
    }

    /**
     * Determines whether the response from the Steam store's Get App Details endpoint was successful.
     *
     * @param obj the response from the HTTP request deserialized into a {@link JsonObject} for easier parsing.
     * @return a Json representation of the categories field in the response.
     */
    private boolean isAppDetailsResponseSuccessful(JsonObject obj) {
        String successFieldKey = "success";
        return Boolean.parseBoolean(obj.get(successFieldKey).toString());
    }

    /**
     * Determines whether the Steam store's details for a game includes any categories.
     *
     * @param obj the response from the HTTP request deserialized into a {@link JsonObject} for easier parsing.
     * @return a Json representation of the categories field in the response.
     */
    private boolean doesGameHaveCategories(JsonObject obj) {
        String dataFieldKey = "data";
        String gameCategoriesFieldKey = "categories";
        JsonElement gameCategories = obj.get(dataFieldKey).getAsJsonObject().get(gameCategoriesFieldKey);
        return gameCategories == null || gameCategories.toString().trim().isEmpty();
    }

    /**
     * Parses a response string to JSON
     *
     * @param stringToParse the string to parse
     * @return JSON representation of the string
     * @throws IOException if an error occurs while serializing the string into JSON
     */
    private JsonElement parseResponseStringToJson(String stringToParse) throws IOException {
        try {
            return JsonParser.parseString(stringToParse);

        } catch (JsonSyntaxException e) {
            throw new IOException("Error when parsing response string into JSON object", e);
        }
    }

    /**
     * Retrieves a Steam API key from AWS secrets manager
     *
     * @return a Steam API key stored within AWS secrets manager
     * @throws SecretRetrievalException if an exception occurs trying to retrieve the Steam API key from the external secrets store.
     */

    private String getSteamApiKey() throws SecretRetrievalException {
        return secretRetriever.getSecretValue(STEAM_API_KEY_SECRET_ID);
    }


    /**
     * Masks the Steam API key within the query params of a  request URI, used to prevent the key being logged.
     *
     * @param requestUri the request URI whose Steam API key should be masked
     * @return the request URI, now containing a masked Steam API key. If the steam id query param cannot be found
     * within the request URI, then the request URI is returned, unmodified
     */
    private String maskSteamApiKey(String requestUri) {
        String steamApiKey;
        int steamKeyIndex = requestUri.indexOf(STEAM_KEY_QUERY_PARAM_KEY);
        if (steamKeyIndex != -1) {
            final String steamApiKeyQueryParam = STEAM_KEY_QUERY_PARAM_KEY + "=";
            if(requestUri.contains("&")) {
                steamApiKey = requestUri.substring(steamKeyIndex).substring(steamApiKeyQueryParam.length(),
                        requestUri.substring(steamKeyIndex).indexOf("&"));
            }
            else {
                steamApiKey = requestUri.substring(steamKeyIndex).substring(steamApiKeyQueryParam.length());
            }
            return requestUri.replaceAll(steamApiKey, STEAM_API_KEY_MASK);
        } else {
            return requestUri;
        }
    }

    /**
     * Sanitizes the URI to the Steam API to prevent sensitive data from being logged
     *
     * @param requestUri the request URI to sanitize
     * @return a sanitized request URI that is safe to log
     */
    private String sanitizeRequestUri(URI requestUri) {
        String requestUriString = requestUri.toString();
        if (requestUriString.contains(STEAM_KEY_QUERY_PARAM_KEY)) {
            requestUriString = maskSteamApiKey(requestUriString);
        }
        return requestUriString;
    }

    /**
     * Entrypoint for constructing a request to the Steam API. Sets all properties for a successful request to the Steam API
     *
     * @param endpoint the desired Steam API endpoint for the request to be built with
     * @return a builder object which can be chained to provide more properties that the request will be constructed with
     * @throws URISyntaxException       if an exception occurs constructing the request URI
     * @throws SecretRetrievalException if an exception occurs trying to retrieve the Steam API key from the external secrets store.
     */
    private URIBuilder steamApiRequest(String endpoint) throws URISyntaxException, SecretRetrievalException {
        return new URIBuilder(steamApiAddress + endpoint)
                .addParameter(STEAM_KEY_QUERY_PARAM_KEY, getSteamApiKey());
    }

    /**
     * Entrypoint for constructing a request to the Steam Store.
     *
     * @param endpoint the desired Steam API endpoint for the request to be built with
     * @return a builder object which can be chained to provide more properties that the request will be constructed with
     * @throws URISyntaxException if an exception occurs constructing the request URI
     */
    private URIBuilder steamStoreRequest(String endpoint) throws URISyntaxException {
        return new URIBuilder(steamStoreAddress + endpoint);
    }

}

