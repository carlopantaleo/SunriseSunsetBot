package com.simpleplus.telegram.bots;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.TimerTask;

public class AlertTimerTask extends TimerTask {

    private final Long chatID;
    private final String message;
    private final TelegramLongPollingBot bot;

    public AlertTimerTask(Long chatID, String message, TelegramLongPollingBot bot) {
        this.chatID = chatID;
        this.message = message;
        this.bot = bot;
    }


    @Override
    public void run() {
        SendMessage messageToSend = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatID)
                .setText(message);
        try {
            bot.sendMessage(messageToSend); // Call method to send the message
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
