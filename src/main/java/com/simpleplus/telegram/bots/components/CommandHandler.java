package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.telegram.telegrambots.api.objects.Update;

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
        }
    }

    @VisibleForTesting
    Command getCommand(Update update) {
        String text = update.getMessage().getText();
        String command = text.substring(text.indexOf("/")+1).split(" ")[0];

        switch (command) {
            case "change-location":
                return Command.REENTER_LOCATION;
            default:
                return null;
        }
    }

    @VisibleForTesting
    enum Command {
        REENTER_LOCATION
    }
}
