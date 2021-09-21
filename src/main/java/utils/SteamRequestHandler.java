package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import models.GetAppListResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

@Data
public class SteamRequestHandler {
    private static final String URI = "https://api.steampowered.com/ISteamApps/GetAppList/v2/?key=";
    private static final String STEAM_API_KEY = "B88AF6D15A99EF5A4E01075EF63E5DF2";

    public static GetAppListResponse requestAllSteamAppsFromSteamApi() {
        String getAppListEndpoint = URI + STEAM_API_KEY;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(getAppListEndpoint);
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, GetAppListResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}

