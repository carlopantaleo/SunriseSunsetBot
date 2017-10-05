package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.SunriseSunsetBot;
import org.telegram.telegrambots.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

public class SunriseSunsetBotMock extends SunriseSunsetBot {
    private List<SendMessage> sentMessages = new ArrayList<>();

    @Override
    public void reply(SendMessage messageToSend) {
        sentMessages.add(messageToSend);
    }

    public List<SendMessage> getSentMessages() {
        return sentMessages;
    }

    public String getLastTextMessage() {
        return sentMessages.get(sentMessages.size() - 1).getText();
    }

    @Override
    public String getBotToken() {
        return "DummyToken";
    }
}
