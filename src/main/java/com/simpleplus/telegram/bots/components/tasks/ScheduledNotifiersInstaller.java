package com.simpleplus.telegram.bots.components.tasks;

import com.simpleplus.telegram.bots.components.BotContext;
import com.simpleplus.telegram.bots.components.Notifier;

import java.util.TimerTask;

public class ScheduledNotifiersInstaller extends TimerTask {
    private Notifier notifier;

    public ScheduledNotifiersInstaller() {
        this.notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
    }

    public void run() {
        notifier.installAllNotifiers();
    }

    @Override
    public String toString() {
        return "ScheduledNotifiersInstaller";
    }
}
