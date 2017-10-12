package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.objects.Update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.simpleplus.telegram.bots.components.SunriseSunsetBot.getChatId;

public class CommandHandler implements BotBean {
    private static final Logger LOG = Logger.getLogger(CommandHandler.class);

    private SunriseSunsetBot bot;
    private MessageHandler messageHandler;
    private PersistenceManager persistenceManager;
    private AdminCommandHandler adminCommandHandler;
    private BotScheduler scheduler;
    private Notifier notifier;
    private UserAlertsManager userAlertsManager;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.messageHandler = (MessageHandler) BotContext.getDefaultContext().getBean(MessageHandler.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        this.adminCommandHandler =
                (AdminCommandHandler) BotContext.getDefaultContext().getBean(AdminCommandHandler.class);
        this.scheduler =
                (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
        this.notifier =
                (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        this.userAlertsManager =
                (UserAlertsManager) BotContext.getDefaultContext().getBean(UserAlertsManager.class);
    }

    public boolean isCommand(Update update) {
        String text;

        if (update.hasMessage() && update.getMessage().hasText()) {
            text = update.getMessage().getText();
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
        } else {
            return false;
        }

        return text.charAt(0) == '/';
    }

    public void handleCommand(Update update) {
        long chatId = getChatId(update);

        switch (getCommand(update)) {
            case START: {
                // It's the same as a normal message sent from a new chat.
                messageHandler.handleMessage(update);
            }
            break;

            case STOP: {
                handleStop(chatId);
            }
            break;

            case RESUME: {
                handleResume(chatId);
            }
            break;

            case REENTER_LOCATION: {
                persistenceManager.setStep(chatId, Step.TO_REENTER_LOCATION);
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
                    persistenceManager.setStep(chatId, Step.TO_ENTER_SUPPORT_MESSAGE);
                    bot.reply(chatId, "Ok, send me your message for support.");
                }
            }
            break;

            case ALERTS: {
                String commandArguments = getCommandArguments(update).trim();
                if (userAlertsManager.validateSyntax(commandArguments)) {
                    // TODO
                } else {
                    userAlertsManager.sendAlertsListAndActions(chatId);
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

    public void handleResume(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);

        if (userState.getStep() == Step.STOPPED) {
            try {
                notifier.tryToInstallNotifier(chatId, 5);
                persistenceManager.setNextStep(chatId);
                bot.reply(chatId, "The bot has been resumed. You will be notified at sunrise and sunset.");
            } catch (ServiceException e) {
                bot.replyAndLogError(chatId, "ServiceException while resuming chat.", e);
            }
        } else {
            bot.reply(chatId, "The bot is already running.");
        }
    }

    private void handleStop(long chatId) {
        UserState userState = persistenceManager.getUserState(chatId);

        if (userState.getStep() != Step.STOPPED) {
            scheduler.cancelAllScheduledMessages(chatId);
            persistenceManager.setStep(chatId, Step.STOPPED);
            bot.reply(chatId, "The bot has been stopped and you won't receive any more messages from now. " +
                    "To receive messages again, use the /resume command.");
        } else {
            bot.reply(chatId, "The bot is already stopped.");
        }
    }

    @VisibleForTesting
    Command getCommand(Update update) {
        String text;
        if (update.hasMessage()) {
            text = update.getMessage().getText();
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
        } else {
            return null;
        }

        Pattern pattern = Pattern.compile("\\/([_a-z]*) ?.*");
        Matcher matcher = pattern.matcher(text);
        String command = "";

        if (matcher.find()) {
            command = matcher.group(1);
        }

        switch (command) {
            case "start":
                return Command.START;
            case "stop":
                return Command.STOP;
            case "resume":
                return Command.RESUME;
            case "change_location":
                return Command.REENTER_LOCATION;
            case "set_administrator":
                return Command.SET_ADMINISTRATOR;
            case "admin":
                return Command.ADMIN_COMMAND;
            case "support":
                return Command.SEND_TO_ADMINISTRATORS;
            case "alerts":
                return Command.ALERTS;

            default:
                return Command.UNKNOWN_COMMAND;
        }
    }

    @VisibleForTesting
    String getCommandArguments(Update update) {
        String text;
        if (update.hasMessage() && update.getMessage().hasText()) {
            text = update.getMessage().getText();
        } else if (update.hasCallbackQuery()) {
            text = update.getCallbackQuery().getData();
        } else {
            return null;
        }
        Pattern pattern = Pattern.compile("\\/[_a-z]* ?(.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        return matcher.find() ? matcher.group(1) : "";
    }

    @VisibleForTesting
    enum Command {
        START,
        STOP,
        RESUME,
        REENTER_LOCATION,
        SET_ADMINISTRATOR,
        SEND_TO_ADMINISTRATORS,
        ALERTS,
        ADMIN_COMMAND,
        UNKNOWN_COMMAND
    }
}
