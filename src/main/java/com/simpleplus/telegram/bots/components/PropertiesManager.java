package com.simpleplus.telegram.bots.components;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This {@link BotBean} is delegated to read properties from various sources with the following priorities:
 * <ol>
 * <li>CLI arguments</li>
 * <li>System options</li>
 * <li>bot.properties file in the path specified in the --bot-properties-path directory</li>
 * <li>bot.properties file in the current working directory</li>
 * </ol>
 */
public class PropertiesManager implements BotBean {
    private static final Logger LOG = Logger.getLogger(PropertiesManager.class);

    private static String[] argv;
    private String botToken;
    private String botName;
    private String botPropertiesPath;
    private String botDatabase;

    public String getBotToken() {
        return botToken;
    }

    public String getBotName() {
        return botName;
    }

    public @Nullable String getBotDatabase() {
        return botDatabase;
    }

    @Override
    public void init() {
        if (argv != null && setPropertiesFromArgv()) {
            LOG.info("Properties set from argv.");
        } else if (setPropertiesFromSystemOptions()) {
            LOG.info("Properties set from system options.");
        } else if (setPropertiesFromPropertiesFile()) {
            LOG.info("Properties set from bot.properties.");
        } else {
            LOG.error("Unable read properties anywhere.");
        }
    }

    private boolean setPropertiesFromPropertiesFile() {
        boolean parsed = false;

        try (InputStream is = new FileInputStream(
                (botPropertiesPath != null ? botPropertiesPath + "/" : "") + "bot.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            if (!parseProperties(properties)) {
                LOG.warn("Properties not parsed from properties file.");
            } else {
                parsed = true;
            }
        } catch (IOException e) {
            LOG.error("Exception while loading bot.properties from path " + botPropertiesPath, e);
        }

        return parsed;
    }

    private boolean setPropertiesFromSystemOptions() {
        Properties properties = System.getProperties();
        if (!parseProperties(properties)) {
            LOG.warn("Properties not parsed from system options.");
            return false;
        } else {
            return true;
        }
    }

    private boolean parseProperties(Properties properties) {
        boolean parsed = false;

        String botName = properties.getProperty("bot-name");
        String botToken = properties.getProperty("bot-token");

        if (botName != null && botToken != null) {
            this.botName = botName;
            this.botToken = botToken;
            parsed = true;
        }

        if (this.botDatabase == null) {
            this.botDatabase = properties.getProperty("bot-database");
        }

        return parsed;
    }

    private boolean setPropertiesFromArgv() {
        Options options = new Options();
        options.addOption(Option.builder("n")
                .longOpt("bot-name")
                .desc("Bot name as registered on Telegram")
                .hasArg()
                .argName("NAME")
                .build());
        options.addOption(Option.builder("t")
                .longOpt("bot-token")
                .desc("Bot token provided by Telegram")
                .hasArg()
                .argName("TOKEN")
                .build());
        options.addOption(Option.builder("p")
                .longOpt("bot-properties-path")
                .desc("bot.properties path")
                .hasArg()
                .argName("PATH")
                .build());
        options.addOption(Option.builder("d")
                .longOpt("bot-database")
                .desc("bot database name")
                .hasArg()
                .argName("NAME")
                .build());

        CommandLineParser parser = new DefaultParser();
        boolean parsed = false;

        try {
            CommandLine line = parser.parse(options, argv);
            if (line.hasOption("bot-name") && line.hasOption("bot-token")) {
                botName = line.getOptionValue("bot-name");
                botToken = line.getOptionValue("bot-token");
                parsed = true;
            } else {
                LOG.warn("Properties not parsed from argv.");
            }

            if (line.hasOption("bot-properties-path")) {
                botPropertiesPath = line.getOptionValue("bot-properties-path");
            }

            if (line.hasOption("bot-database")) {
                botDatabase = line.getOptionValue("bot-database");
            }
        } catch (ParseException e) {
            LOG.error("Exception while parsing argv.", e);
            parsed = false;
        }

        return parsed;
    }

    public static String[] getArgv() {
        return argv;
    }

    public static void setArgv(String[] argv) {
        PropertiesManager.argv = argv;
    }
}
