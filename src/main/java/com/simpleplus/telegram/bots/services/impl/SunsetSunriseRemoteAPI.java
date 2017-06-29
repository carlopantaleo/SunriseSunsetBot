package com.simpleplus.telegram.bots.services.impl;

import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.helpers.Coordinates;
import com.simpleplus.telegram.bots.helpers.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SunsetSunriseRemoteAPI implements SunsetSunriseService {
    private String baseUrl = "https://api.sunrise-sunset.org/json?lat=%f&lng=%f";

    public SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException {
        String result = callRemoteService(coordinates);
        System.out.println(result); // Debug
        return parseResult(result);
    }

    private SunsetSunriseTimes parseResult(String result) throws ServiceException {
        JSONObject obj = new JSONObject(result);
        String status, civilTwilightEnd, civilTwilightBegin;

        try {
            status = obj.getString("status");
            civilTwilightEnd = obj.getJSONObject("results").getString("civil_twilight_end");
            civilTwilightBegin = obj.getJSONObject("results").getString("civil_twilight_begin");
        } catch (JSONException e) {
            throw new ServiceException("Internal service error (JSONException)");
        }

        if (!status.equals("OK")) {
            throw new ServiceException("Remote service error (" + status + ")");
        }

        LocalTime sunset;
        LocalTime sunrise;
        try {
            sunset = LocalTime.parse(civilTwilightEnd, DateTimeFormatter.ofPattern("h:m:s a"));
            sunrise = LocalTime.parse(civilTwilightBegin, DateTimeFormatter.ofPattern("h:m:s a"));
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            throw new ServiceException("Internal service error (DateTimeParseException)");
        }

        return new SunsetSunriseTimes(sunset, sunrise);
    }

    private String callRemoteService(Coordinates coordinates) throws ServiceException {
        String result = "";
        
        try {
            URL url = new URL(String.format(baseUrl, coordinates.getLatitude(), coordinates.getLongitude()));
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new ServiceException("HTTP Error "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {
                result = result.concat(output);
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new ServiceException("IO Error");
        }
        
        return result;
    }
}
