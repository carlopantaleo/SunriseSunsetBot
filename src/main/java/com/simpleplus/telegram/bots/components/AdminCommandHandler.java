package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommandHandler extends CommandHandler implements BotBean {
    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;
    private PersistenceManager persistenceManager;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean(MessageHandler.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    public boolean verifyAdminToken(Update update) {
        return getCommand(update) == Command.SET_ADMINISTRATOR &&
                getCommandArguments(update).equals(bot.getBotToken());
    }

    @Override
    public void handleCommand(Update update) {
        String chatArgs = getCommandArguments(update);

        switch (chatArgs.split(" ")[0]) {
            case "broadcast": {
                String message = getBroadcastMessage(chatArgs);
                broadcast(message);
            }
            break;
        }
    }

    private void broadcast(String message) {
        Map<Long, UserState> userStatesMap = persistenceManager.getUserStatesMap();

        for (Map.Entry<Long, UserState> entry : userStatesMap.entrySet()) {
            bot.reply(entry.getKey(), message);
        }
    }

    @VisibleForTesting
    String getBroadcastMessage(String chatArgs) {
        Pattern pattern = Pattern.compile("broadcast (.*)");
        Matcher matcher = pattern.matcher(chatArgs);

        return matcher.find() ? matcher.group(1) : "";

    }

    public boolean isAdminChat(long chatId) {
        return persistenceManager.getUserState(chatId).isAdmin();
    }
}
