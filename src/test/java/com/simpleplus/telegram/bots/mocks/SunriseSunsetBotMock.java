package com.simpleplus.telegram.bots.mocks;

import com.simpleplus.telegram.bots.components.SunriseSunsetBot;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;

import java.util.ArrayList;
import java.util.List;

public class SunriseSunsetBotMock extends SunriseSunsetBot {
    private List<SendMessage> sentMessages = new ArrayList<>();

    @Override
    public void reply(SendMessage messageToSend) {
        sentMessages.add(messageToSend);
    }

    @Override
    public void reply(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(message);
        sentMessages.add(sendMessage);
    }

    @Override
    public void reply(EditMessageText messageToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(messageToSend.getText());
        sendMessage.setReplyToMessageId(messageToSend.getMessageId());
        sendMessage.setReplyMarkup(messageToSend.getReplyMarkup());
        sentMessages.add(sendMessage);
    }

    public List<SendMessage> getSentMessages() {
        return sentMessages;
    }

    public SendMessage getLastTextMessage() {
        return sentMessages.size() > 0 ? sentMessages.get(sentMessages.size() - 1) : null;
    }

    @Override
    public String getBotToken() {
        return "DummyToken";
    }
}
