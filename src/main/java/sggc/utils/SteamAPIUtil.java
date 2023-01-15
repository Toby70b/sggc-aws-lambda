package sggc.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import sggc.exceptions.SecretRetrievalException;
import sggc.factory.AWSSecretsManagerClientFactory;
import sggc.infrasturcture.AwsSecretRetriever;
import sggc.infrasturcture.SecretRetriever;
import sggc.models.GetAppListResponse;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.IOException;

/**
 * Utility class for interacting with the Steam API
 */
@RequiredArgsConstructor
public class SteamAPIUtil {
    private static final String URI = "https://api.steampowered.com/ISteamApps/GetAppList/v2/?key=";
    private static final String STEAM_API_KEY_NAME = "SteamAPIKey";

    /**
     * Sends a HTTP request to the Steam API to retrieve a list of all games currently on Steam
     * @return an object containing a list of all games, parsed from the response from the Steam API
     * @throws IOException if an exception occurs either sending the request or parsing the response into an object
     * @throws SecretRetrievalException if an exception occurs trying to retrieve the Steam API key from AWS secrets manager
     */
    //TODO replace with rest client used in SGGC_WS
    public static GetAppListResponse requestAllSteamAppsFromSteamApi() throws IOException,SecretRetrievalException {
        String getAppListEndpoint = URI + getSteamApiKey();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(getAppListEndpoint);
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, GetAppListResponse.class);
        } catch (IOException e) {
            throw new IOException("Exception occurred when attempting to retrieve and parse all games from the Steam API",e);
        }
    }

    /**
     * Retrieves the Steam API key from AWS secrets manager
     * @return the Steam API key stored within AWS secrets manager
     * @throws SecretRetrievalException if an exception occurs trying to retrieve the Steam API key from AWS secrets manager
     */
    private static String getSteamApiKey() throws SecretRetrievalException {
        try(SecretsManagerClient secretsManagerClient = new AWSSecretsManagerClientFactory().createClient()){
            SecretRetriever secretRetriever = new AwsSecretRetriever(secretsManagerClient);
            return secretRetriever.getSecretValue(STEAM_API_KEY_NAME);
        }
        catch (Exception e){
            throw new SecretRetrievalException("Exception occurred when attempting to retrieve Steam API Key from AWS secrets manager",e);
        }
    }
}

