package com.simpleplus.telegram.bots;

import java.util.TimerTask;

public class ScheduledNotifiersInstaller extends TimerTask {
    private SunriseSunsetBot bot;

    public ScheduledNotifiersInstaller(SunriseSunsetBot bot) {
        this.bot = bot;
    }

    public void run() {
        bot.installAllNotifiers();
    }

    @Override
    public String toString() {
        return "ScheduledNotifiersInstaller";
    }
}
