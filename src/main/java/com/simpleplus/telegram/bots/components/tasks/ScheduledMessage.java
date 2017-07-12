package com.simpleplus.telegram.bots.components.tasks;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.TimerTask;

public class ScheduledMessage extends TimerTask {

    private final Long chatID;
    private final String message;
    private final TelegramLongPollingBot bot;

    public ScheduledMessage(Long chatID, String message, TelegramLongPollingBot bot) {
        this.chatID = chatID;
        this.message = message;
        this.bot = bot;
    }

    @Override
    public void run() {
        SendMessage messageToSend = new SendMessage()
                .setChatId(chatID)
                .setText(message);
        try {
            bot.sendMessage(messageToSend);
        } catch (TelegramApiException e) {
            // TODO gestire la rimozione della chat
        }
    }

}
