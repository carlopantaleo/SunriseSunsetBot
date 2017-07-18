package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.oracle.tools.packager.Log;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.objects.Update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHandler implements BotBean {
    private static final Logger LOG = Logger.getLogger(CommandHandler.class);

    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;
    private PersistenceManager persistenceManager;
    private AdminCommandHandler adminCommandHandler;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean("SunriseSunsetBot");
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean("MessageHandler");
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean("PersistenceManager");
        this.adminCommandHandler =
                (AdminCommandHandler) BotContext.getDefaultContext().getBean("AdminCommandHandler");

    }

    public boolean isCommand(Update update) {
        return update.getMessage().getText().charAt(0) == '/';
    }

    // TODO: unit test
    public void handleCommand(Update update) {
        long chatId = update.getMessage().getChatId();

        switch (getCommand(update)) {
            case REENTER_LOCATION: {
                UserState userState = persistenceManager.getUserState(chatId);
                userState.setStep(Step.TO_REENTER_LOCATION);
                persistenceManager.setUserState(chatId, userState);
                messageHandler.handleMessage(update);
                Log.info("ChatId[" + Long.toString(chatId) + "] has asked to /change-location.");
            }
            break;

            case SET_ADMINISTRATOR: {
                if (adminCommandHandler.verifyAdminToken(update)) {
                    UserState userState = persistenceManager.getUserState(chatId);
                    userState.setAdmin(true);
                    persistenceManager.setUserState(chatId, userState);
                    LOG.info("Set admin chat for chatId[" + Long.toString(chatId) + "]");
                } else {
                    LOG.warn("ChatId[" + Long.toString(chatId) + "] is issuing the /set-administrator command without " +
                            "a valid token.");
                }
            }
            break;
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

        return matcher.find() ? matcher.group(1) : "";
    }

    @VisibleForTesting
    enum Command {
        REENTER_LOCATION,
        SET_ADMINISTRATOR
    }
}
