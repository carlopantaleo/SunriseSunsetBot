package com.simpleplus.telegram.bots.components;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This {@link BotBean} is delegated to read properties from various sources with the following priorities:
 * <ol>
 * <li>CLI arguments</li>
 * <li>System options</li>
 * <li>bot.properties file in the path specified in the --bot-properties-path directory</li>
 * <li>bot.properties file in the current working directory</li>
 * </ol>
 * If a property is found in more than one source, the property with the highest priority in the list above
 * will be used.
 */
public class PropertiesManager implements BotBean {
    private static final Logger LOG = LogManager.getLogger(PropertiesManager.class);
    private static String[] argv;

    private Map<String, String> propertiesMap = new HashMap<>();

    public PropertiesManager() {
        setPropertiesFromPropertiesFile();
        setPropertiesFromSystemOptions();
        setPropertiesFromArgv();
    }

    public static String[] getArgv() {
        return argv;
    }

    public static void setArgv(String[] argv) {
        PropertiesManager.argv = argv;
    }

    public String getBotToken() {
        return propertiesMap.get("bot-token");
    }

    public String getBotName() {
        return propertiesMap.get("bot-name");
    }

    public @Nullable
    String getBotDatabase() {
        return propertiesMap.get("bot-database");
    }

    @Override
    public void init() {
    }

    private void setPropertiesFromPropertiesFile() {
        CommandLineParser parser = new DefaultParser();
        String botPropertiesPath = null;

        try {
            CommandLine line = parser.parse(getDeclaredOptions(), argv);
            if (line.hasOption("bot-properties-path")) {
                botPropertiesPath = line.getOptionValue("bot-properties-path");
            }
        } catch (ParseException e) {
            LOG.error("Cannot parse options");
        }

        try (InputStream is = new FileInputStream(
                (botPropertiesPath != null ? botPropertiesPath + "/" : "") + "bot.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            parseProperties(properties);
        } catch (FileNotFoundException e) {
            LOG.warn("bot.properties not found.");
        } catch (IOException e) {
            LOG.error("Exception while loading bot.properties from path " + botPropertiesPath, e);
        }

        LOG.debug("From properties file: " + propertiesMap.toString());
    }

    private void setPropertiesFromSystemOptions() {
        Properties properties = System.getProperties();
        parseProperties(properties);
        LOG.debug("From system options: " + propertiesMap.toString());
    }

    private void parseProperties(Properties properties) {
        Set<Object> props = properties.keySet();

        for (Object propr : props.toArray()) {
            propertiesMap.put(propr.toString(), properties.getProperty(propr.toString()));
        }
    }

    private void setPropertiesFromArgv() {
        Options options = getDeclaredOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(options, argv);
            Option[] opts = line.getOptions();

            for (Option opt : opts) {
                String value = opt.getValue() != null ? opt.getValue() : "true"; // Workaround for no-arg options
                propertiesMap.put(opt.getLongOpt(), value);
            }
        } catch (ParseException e) {
            LOG.error("Exception while parsing argv.", e);
        }

        LOG.debug("From argv: " + propertiesMap.toString());
    }

    private Options getDeclaredOptions() {
        return new Options()
                .addOption(new Option("n", "bot-name", true, "Bot name as registered on Telegram"))
                .addOption(new Option("t", "bot-token", true, "Bot token provided by Telegram"))
                .addOption(new Option("p", "bot-properties-path", true, "bot.properties path"))
                .addOption(new Option("d", "bot-database", true, "bot database name"))
                .addOption(new Option("w", "embed-web-server", false, "start embedded H2 web server"));
    }

    public String getProperty(String property) {
        return propertiesMap.get(property);
    }

    public String getPropertyOrDefault(String property, String defaultValue) {
        String value = propertiesMap.get(property);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }
}
