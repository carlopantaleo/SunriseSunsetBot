package com.simpleplus.telegram.bots.services.impl;

import com.simpleplus.telegram.bots.components.BotBean;
import com.simpleplus.telegram.bots.datamodel.Coordinates;
import com.simpleplus.telegram.bots.datamodel.SunsetSunriseTimes;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import com.simpleplus.telegram.bots.services.SunsetSunriseService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class SunsetSunriseRemoteAPI implements SunsetSunriseService, BotBean {
    private static final Logger LOG = Logger.getLogger(SunsetSunriseRemoteAPI.class);
    private static final String BASE_URL = "https://api.sunrise-sunset.org/json?lat=%f&lng=%f&date=%s";

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
        JSONObject obj = new JSONObject(result);
        String status, sunsetStr, sunriseStr,
                civilTwilightBeginStr, civilTwilightEndStr,
                nauticalTwilightBeginStr, nauticalTwilightEndStr,
                astronomicalTwilightBeginStr, astronomicalTwilightEndStr;

        try {
            status = obj.getString("status");
            sunsetStr = obj.getJSONObject("results").getString("sunset");
            sunriseStr = obj.getJSONObject("results").getString("sunrise");
            civilTwilightBeginStr = obj.getJSONObject("results").getString("civil_twilight_begin");
            civilTwilightEndStr = obj.getJSONObject("results").getString("civil_twilight_end");
            nauticalTwilightBeginStr = obj.getJSONObject("results").getString("nautical_twilight_begin");
            nauticalTwilightEndStr = obj.getJSONObject("results").getString("nautical_twilight_end");
            astronomicalTwilightBeginStr = obj.getJSONObject("results").getString("astronomical_twilight_begin");
            astronomicalTwilightEndStr = obj.getJSONObject("results").getString("astronomical_twilight_end");
        } catch (JSONException e) {
            throw new ServiceException("Internal service error (JSONException)");
        }

        if (!"OK".equals(status)) {
            throw new ServiceException("Remote service error (" + status + ")");
        }

        LocalTime sunset, sunrise,
                civilTwilightBegin, civilTwilightEnd,
                nauticalTwilightBegin, nauticalTwilightEnd,
                astronomicalTwilightBegin, astronomicalTwilightEnd;
        try {
            sunset = LocalTime.parse(sunsetStr, DateTimeFormatter.ofPattern("h:m:s a"));
            sunrise = LocalTime.parse(sunriseStr, DateTimeFormatter.ofPattern("h:m:s a"));
            civilTwilightBegin = LocalTime.parse(civilTwilightBeginStr, DateTimeFormatter.ofPattern("h:m:s a"));
            civilTwilightEnd = LocalTime.parse(civilTwilightEndStr, DateTimeFormatter.ofPattern("h:m:s a"));
            nauticalTwilightBegin = LocalTime.parse(nauticalTwilightBeginStr, DateTimeFormatter.ofPattern("h:m:s a"));
            nauticalTwilightEnd = LocalTime.parse(nauticalTwilightEndStr, DateTimeFormatter.ofPattern("h:m:s a"));
            astronomicalTwilightBegin =
                    LocalTime.parse(astronomicalTwilightBeginStr, DateTimeFormatter.ofPattern("h:m:s a"));
            astronomicalTwilightEnd =
                    LocalTime.parse(astronomicalTwilightEndStr, DateTimeFormatter.ofPattern("h:m:s a"));
        } catch (DateTimeParseException e) {
            LOG.error("DateTimeParseException", e);
            throw new ServiceException("Internal service error (DateTimeParseException)");
        }

        return new SunsetSunriseTimes(sunset, sunrise,
                civilTwilightEnd, civilTwilightBegin,
                nauticalTwilightEnd, nauticalTwilightBegin,
                astronomicalTwilightEnd, astronomicalTwilightBegin);
    }

    private String callRemoteService(Coordinates coordinates, LocalDate localDate) throws ServiceException {
        String result = "";

        try {
            URL url = new URL(String.format(Locale.ROOT, BASE_URL,
                    coordinates.getLatitude(),
                    coordinates.getLongitude(),
                    localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));
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
            LOG.error("MalformedURLException", e);
        } catch (IOException e) {
            throw new ServiceException("IO Error");
        }

        return result;
    }
}
