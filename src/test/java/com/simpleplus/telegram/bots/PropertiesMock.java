package com.simpleplus.telegram.bots;

import com.simpleplus.telegram.bots.components.PropertiesManager;

import javax.annotation.Nullable;

public class PropertiesMock extends PropertiesManager {
    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public String getBotToken() {
        return "";
    }

    @Override
    public String getBotName() {
        return "";
    }

    @Nullable
    @Override
    public String getBotDatabase() {
        return "BotDatabaseTest.db";
    }
}
