package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.TimeType;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This component handles user alerts. It:
 * <ul>
 * <li>Handles {@code /alert} commands.</li>
 * <li>Serves {@code UserAlert}s for a specific user.</li>
 * </ul>
 */
public class UserAlertsManager implements BotBean {
    private static final Logger LOG = Logger.getLogger(UserAlertsManager.class);
    private static final String COMMAND_REGEX =
            "(?:(add|remove|edit)( [0-9]+)?" +
                    "( civil twilight (?:begin|end)| sunrise| sunset)?" +
                    "(?: delay (-?[0-9]{1,2}))?)";

    private PersistenceManager persistenceManager;
    private SunriseSunsetBot bot;
    private Notifier notifier;

    @Override
    public void init() {
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
    }

    public Set<UserAlert> getUserAlerts(long chatId) {
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(chatId);
        if (userAlerts.isEmpty()) {
            LOG.info(String.format("Going to generate default UserAlerts for chatid %s", chatId));
            return generateAndGetDefaultUserAlerts(chatId);
        }

        return userAlerts;
    }

    private Set<UserAlert> generateAndGetDefaultUserAlerts(long chatId) {
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNRISE_TIME, 0));
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNSET_TIME, 0));
        return persistenceManager.getUserAlerts(chatId);
    }

    public boolean validateSyntax(String update) {
        return update.matches(COMMAND_REGEX);
    }

    public void sendAlertsListAndActions(long chatId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(new InlineKeyboardButton().setText("Add...").setCallbackData("/alerts add"));
        row.add(new InlineKeyboardButton().setText("Delete...").setCallbackData("/alerts delete"));
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        // Build the message
        SendMessage messageToSend = new SendMessage();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(chatId);
        messageToSend.setText("TBD");

        bot.reply(messageToSend);
    }

    public void handleCommand(long chatId, String commandArguments) {
        CommandParameters parameters = extractParameters(commandArguments);

        if (parameters.hasCommand()) {
            switch (parameters.command) {
                case "add":
                    handleAdd(chatId, parameters);
                    break;
                case "remove":
                    handleRemove(chatId, parameters);
                    break;
                case "edit":
                    handleEdit(chatId, parameters);
                    break;
            }
        }

    }

    private void handleEdit(long chatId, CommandParameters parameters) {
        // TODO
    }

    private void handleRemove(long chatId, CommandParameters parameters) {
        // TODO
    }

    private void handleAdd(long chatId, CommandParameters parameters) {
        if (parameters.hasAlertType()) {
            switch (parameters.alertType) {
                case "sunrise":
                    persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNRISE_TIME, parameters.delay));
                    break;
                case "sunset":
                    persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNSET_TIME, parameters.delay));
                    break;
            }

            try {
                notifier.tryToInstallNotifier(chatId, 5);
            } catch (ServiceException e) {
                bot.reply(chatId, "Your alert has been saved, however we are encountering some " +
                        "technical difficulties and it may not be fired for today.");
                LOG.error("ServiceException while trying to install notifier on just created alert.", e);
            }
        }
    }

    private CommandParameters extractParameters(String commandArguments) {
        Pattern pattern = Pattern.compile(COMMAND_REGEX);
        Matcher matcher = pattern.matcher(commandArguments);

        CommandParameters parameters = new CommandParameters();
        if (matcher.find()) {
            parameters.command = Optional.ofNullable(matcher.group(1)).orElse("").trim();
            parameters.alertType = Optional.ofNullable(matcher.group(3)).orElse("").trim();
            parameters.alertId = Long.parseLong(Optional.ofNullable(matcher.group(2)).orElse("0").trim());
            parameters.delay = Long.parseLong(Optional.ofNullable(matcher.group(4)).orElse("0").trim());
        }

        return parameters;
    }

    private class CommandParameters {
        public String command = "";
        public String alertType = "";
        public long alertId = 0;
        public long delay = 0;

        public boolean hasCommand() {
            return !command.isEmpty();
        }

        public boolean hasAlertType() {
            return !alertType.isEmpty();
        }

        public boolean hasAlertId() {
            return alertId != 0;
        }
    }
}
