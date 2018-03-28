package com.simpleplus.telegram.bots.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            APIResponse response = objectMapper.readValue(result, APIResponse.class);

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

    /**
     * Calls the remote API and returns the response string.
     *
     * @param localDate the date which will be passed to the service. Please note that since it is a non-zoned date,
     *                  prior to passing it to the API, it will be converted to a zoned one with system-default time
     *                  zone (assuming start of day as time).
     */
    private String callRemoteService(Coordinates coordinates, LocalDate localDate) throws ServiceException {
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(String.format(Locale.ROOT, BASE_URL,
                    coordinates.getLatitude(),
                    coordinates.getLongitude(),
                    localDate.atStartOfDay(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
        private String status;
        private String message;
        private Map<String, LocalDateTime> results;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, LocalDateTime> getResults() {
            return results;
        }

        public void setResults(Map<String, LocalDateTime> results) {
            this.results = results;
        }
    }
}
