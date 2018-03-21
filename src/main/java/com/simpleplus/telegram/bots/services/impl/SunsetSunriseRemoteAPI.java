package com.simpleplus.telegram.bots.services.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

public class SunsetSunriseRemoteAPI implements SunsetSunriseService, BotBean {
    private static final Logger LOG = LogManager.getLogger(SunsetSunriseRemoteAPI.class);
    private static final String BASE_URL = "http://127.0.0.1:8500/json/sun/%f/%f/%s";

    @Override
    public SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates, LocalDate localDate)
            throws ServiceException {
        String result = callRemoteService(coordinates, localDate);
        LOG.debug(result);
        return parseResult(result);
    }

    @Override
    public SunsetSunriseTimes getSunsetSunriseTimes(Coordinates coordinates) throws ServiceException {
        return getSunsetSunriseTimes(coordinates, LocalDate.now());
    }

    private SunsetSunriseTimes parseResult(String result) throws ServiceException {
        SunsetSunriseTimes times = new SunsetSunriseTimes();

        try {
            APIResponse response = new ObjectMapper().readValue(result, APIResponse.class);

            if (response.status == null || !"OK".equals(response.status)) {
                throw new ServiceException("Remote service error: " + response.message);
            }

            for (Map.Entry<String, LocalDateTime> entry : response.results.entrySet()) {
                times.putTime(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new ServiceException("Internal service error (" + e.getMessage() + ")", e);
        }

        return times;
    }

    private String callRemoteService(Coordinates coordinates, LocalDate localDate) throws ServiceException {
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(String
                    .format(Locale.ROOT, BASE_URL, coordinates.getLatitude(), coordinates.getLongitude(),
                            localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new ServiceException("HTTP Error " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String output;
            while ((output = br.readLine()) != null) {
                result = result.append(output);
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            LOG.error("MalformedURLException", e);
        } catch (IOException e) {
            throw new ServiceException("IO Error");
        }

        return result.toString();
    }

    public static class APIResponse {
        String status;
        String message;
        Map<String, LocalDateTime> results;
    }
}
