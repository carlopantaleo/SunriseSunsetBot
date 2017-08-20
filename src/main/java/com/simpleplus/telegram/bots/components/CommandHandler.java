package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
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
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean(MessageHandler.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        this.adminCommandHandler =
                (AdminCommandHandler) BotContext.getDefaultContext().getBean(AdminCommandHandler.class);

    }

    public boolean isCommand(Update update) {
        if (update.getMessage().hasText()) {
            return update.getMessage().getText().charAt(0) == '/';
        } else {
            return false;
        }
    }

    // TODO: unit test
    public void handleCommand(Update update) {
        long chatId = update.getMessage().getChatId();

        switch (getCommand(update)) {
            case START: {
                // It's the same as a normal message sent from a new chat.
                messageHandler.handleMessage(update);
            }
            break;

            case REENTER_LOCATION: {
                UserState userState = persistenceManager.getUserState(chatId);
                userState.setStep(Step.TO_REENTER_LOCATION);
                persistenceManager.setUserState(chatId, userState);
                LOG.debug(String.format("ChatId[%d] has asked to /change_location.", chatId));
                messageHandler.handleMessage(update);
            }
            break;

            case SET_ADMINISTRATOR: {
                if (adminCommandHandler.verifyAdminToken(update)) {
                    UserState userState = persistenceManager.getUserState(chatId);
                    userState.setAdmin(true);
                    persistenceManager.setUserState(chatId, userState);
                    LOG.info(String.format("Set admin chat for chatId[%d]", chatId));
                    bot.reply(chatId, "Administration commands are now available.");
                } else {
                    LOG.warn(String.format("ChatId[%d] is issuing the /set_administrator command without " +
                            "a valid token.", chatId));
                }
            }
            break;

            case SEND_TO_ADMINISTRATORS: {
                String commandArguments = getCommandArguments(update);
                if (!commandArguments.matches(" *")) {
                    messageHandler.sendToSupport(chatId, commandArguments);
                } else {
                    UserState userState = persistenceManager.getUserState(chatId);
                    userState.setStep(Step.TO_ENTER_SUPPORT_MESSAGE);
                    persistenceManager.setUserState(chatId, userState);
                    bot.reply(chatId, "Ok, send me your message for support.");
                }
            }
            break;

            case ADMIN_COMMAND: {
                if (adminCommandHandler.isAdminChat(chatId)) {
                    adminCommandHandler.handleCommand(update);
                } else {
                    bot.reply(chatId,
                            "You are issuing a admin command on a non-admin chat: operation not permitted!");
                    LOG.warn(String.format("ChatId[%d] is issuing a /admin command on a non-admin chat.", chatId));
                }
            }
            break;

            case UNKNOWN_COMMAND: {
                bot.reply(chatId, "I don't know how to handle this command.");
            }
        }
    }

    @VisibleForTesting
    Command getCommand(Update update) {
        String text = update.getMessage().getText();
        Pattern pattern = Pattern.compile("\\/([_a-z]*) ?.*");
        Matcher matcher = pattern.matcher(text);
        String command = "";

        if (matcher.find()) {
            command = matcher.group(1);
        }

        switch (command) {
            case "start":
                return Command.START;
            case "change_location":
                return Command.REENTER_LOCATION;
            case "set_administrator":
                return Command.SET_ADMINISTRATOR;
            case "admin":
                return Command.ADMIN_COMMAND;
            case "support":
                return Command.SEND_TO_ADMINISTRATORS;

            default:
                return Command.UNKNOWN_COMMAND;
        }
    }

    @VisibleForTesting
    String getCommandArguments(Update update) {
        String text = update.getMessage().getText();
        Pattern pattern = Pattern.compile("\\/[_a-z]* ?(.*)");
        Matcher matcher = pattern.matcher(text);

        return matcher.find() ? matcher.group(1) : "";
    }

    @VisibleForTesting
    enum Command {
        START,
        REENTER_LOCATION,
        SET_ADMINISTRATOR,
        SEND_TO_ADMINISTRATORS,
        ADMIN_COMMAND,
        UNKNOWN_COMMAND
    }
}
