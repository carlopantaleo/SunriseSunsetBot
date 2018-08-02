package com.simpleplus.telegram.bots.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.components.BotContext;
import com.simpleplus.telegram.bots.components.PropertiesManager;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.NamedLocationToCoordinatesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class GooglePlacesAPI implements NamedLocationToCoordinatesService, BotBean {
    private static final Logger LOG = LogManager.getLogger(GooglePlacesAPI.class);
    private static final String API_ROOT = "https://maps.googleapis.com/maps/api/place/";
    private static final String PLACE_ID = API_ROOT + "findplacefromtext/json?key=%s&inputtype=textquery&fields=geometry,formatted_address&input=%s";

    private PropertiesManager propertiesManager;

    @Override
    public void init() {
        propertiesManager = (PropertiesManager) BotContext.getDefaultContext().getBean(PropertiesManager.class);
    }

    @Override
    public @Nullable Coordinates findCoordinates(String namedLocation) throws ServiceException {
        String result = callRemoteService(namedLocation);

        if (!result.isEmpty()) {
            return parseResult(result);
        } else {
            return null;
        }
    }

    private Coordinates parseResult(String result) throws ServiceException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            PlaceSearchResult placeSearchResult = objectMapper.readValue(result, PlaceSearchResult.class);
            if (placeSearchResult.getCandidates().size() > 0) {
                PlaceCandidate firstCandidate = placeSearchResult.getCandidates().get(0);
                PlaceLocation location = firstCandidate.getGeometry().getLocation();
                Coordinates coordinates = new Coordinates(location.getLat(), location.getLng());
                coordinates.setDescription(firstCandidate.getFormattedAddress());
                return coordinates;
            } else {
                return null;
            }
        } catch (IOException e) {
            LOG.error("IOException during places API response deserialization.", e);
            throw new ServiceException("Could not deserialize response.");
        }
    }

    @VisibleForTesting // Public in order to be mocked
    public String callRemoteService(String namedLocation) throws ServiceException {
        String key = propertiesManager.getPropertyOrDefault("google-places-api-key", "");
        return callApi(key, namedLocation);
    }

    private String callApi(String... params) throws ServiceException {
        StringBuilder result = new StringBuilder();

        try {
            URL _url = new URL(String.format(Locale.ROOT, GooglePlacesAPI.PLACE_ID, params));
            HttpURLConnection conn = (HttpURLConnection) _url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new ServiceException("HTTP Error " + conn.getResponseCode());
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String output;
                while ((output = br.readLine()) != null) {
                    result = result.append(output);
                }
            }

            conn.disconnect();
        } catch (MalformedURLException e) {
            LOG.error("MalformedURLException", e);
        } catch (IOException e) {
            throw new ServiceException("IO Error");
        }

        return result.toString();
    }


    // Classes for response deserialization

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceSearchResult {
        private String status;
        private List<PlaceCandidate> candidates;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<PlaceCandidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<PlaceCandidate> candidates) {
            this.candidates = candidates;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceCandidate {
        @JsonProperty("formatted_address") private String formattedAddress;
        private PlaceGeometry geometry;

        public String getFormattedAddress() {
            return formattedAddress;
        }

        public void setFormattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
        }

        public PlaceGeometry getGeometry() {
            return geometry;
        }

        public void setGeometry(PlaceGeometry geometry) {
            this.geometry = geometry;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceGeometry {
        private PlaceLocation location;

        public PlaceLocation getLocation() {
            return location;
        }

        public void setLocation(PlaceLocation location) {
            this.location = location;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceLocation {
        private float lat;
        private float lng;

        public float getLat() {
            return lat;
        }

        public void setLat(float lat) {
            this.lat = lat;
        }

        public float getLng() {
            return lng;
        }

        public void setLng(float lng) {
            this.lng = lng;
        }
    }
}
