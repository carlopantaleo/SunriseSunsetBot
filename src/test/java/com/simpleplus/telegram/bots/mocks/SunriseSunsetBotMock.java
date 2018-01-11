package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.SunriseSunsetBot;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;

import java.util.ArrayList;
import java.util.List;

public class SunriseSunsetBotMock extends SunriseSunsetBot {
    private List<String> sentMessages = new ArrayList<>();

    @Override
    public void reply(SendMessage messageToSend) {
        sentMessages.add(messageToSend.getText());
    }

    @Override
    public void reply(long chatId, String message) {
        sentMessages.add(message);
    }

    @Override
    public void reply(EditMessageText messageToSend) {
        sentMessages.add(messageToSend.getText());
    }

    public List<String> getSentMessages() {
        return sentMessages;
    }

    public String getLastTextMessage() {
        return sentMessages.size() > 0 ? sentMessages.get(sentMessages.size() - 1) : "";
    }

    @Override
    public String getBotToken() {
        return "DummyToken";
    }
}
