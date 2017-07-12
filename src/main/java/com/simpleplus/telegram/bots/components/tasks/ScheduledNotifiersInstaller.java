package com.simpleplus.telegram.bots.components.tasks;

import com.simpleplus.telegram.bots.components.Notifier;

import java.util.TimerTask;

public class ScheduledNotifiersInstaller extends TimerTask {
    private Notifier notifier;

    public ScheduledNotifiersInstaller(Notifier notifier) {
        this.notifier = notifier;
    }

    public void run() {
        notifier.installAllNotifiers();
    }

    @Override
    public String toString() {
        return "ScheduledNotifiersInstaller";
    }
}
