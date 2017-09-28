package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.SunriseSunsetBot;
import org.telegram.telegrambots.api.methods.send.SendMessage;

public class SunriseSunsetBotMock extends SunriseSunsetBot {
    private SendMessage lastSendMessage;

    @Override
    public void reply(SendMessage messageToSend) {
        lastSendMessage = messageToSend;
    }

    public SendMessage getLastSendMessage() {
        return lastSendMessage;
    }

    public String getLastTextMessage() {
        return lastSendMessage.getText();
    }
}
