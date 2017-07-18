package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.telegram.telegrambots.api.objects.Update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements BotBean {
    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;
    private PersistenceManager persistenceManager;

    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean("MessageHandler");
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean("PersistenceManager");
    }

    public boolean isCommand(Update update) {
        return update.getMessage().getText().charAt(0) == '/';
    }

    public void handleCommand(Update update) {
        long chatId = update.getMessage().getChatId();

        switch (getCommand(update)) {
            case REENTER_LOCATION: {
                UserState userState = persistenceManager.getUserState(chatId);
                userState.setStep(Step.TO_REENTER_LOCATION);
                persistenceManager.setUserState(chatId, userState);
                messageHandler.handleMessage(update);
            }
            break;

            case SET_ADMINISTRATOR: {
                UserState userState = persistenceManager.getUserState(chatId);

            }
        }
    }

    @VisibleForTesting
    Command getCommand(Update update) {
        String text = update.getMessage().getText();
        Pattern pattern = Pattern.compile("\\/([-a-z]*) ?.*");
        Matcher matcher = pattern.matcher(text);
        String command = "";

        if (matcher.find()) {
            command = matcher.group(1);
        }

        switch (command) {
            case "change-location":
                return Command.REENTER_LOCATION;
            case "set-administrator":
                return Command.SET_ADMINISTRATOR;
            default:
                return null;
        }
    }

    @VisibleForTesting
    String getCommandArguments(Update update) {
        String text = update.getMessage().getText();
        Pattern pattern = Pattern.compile("\\/[-a-z]* ?(.*)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    @VisibleForTesting
    enum Command {
        REENTER_LOCATION,
        SET_ADMINISTRATOR
    }
}
