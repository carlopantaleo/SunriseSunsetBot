package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.TimeType;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
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

    private void sendAlertsTypes(long chatId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard (adds a default delay of +1 as a workaround in order to not overwrite any existing
        // no-delay alert.
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new InlineKeyboardButton().setText("Sunrise").setCallbackData("/alerts add sunrise delay 1"));
        row1.add(new InlineKeyboardButton().setText("Sunset").setCallbackData("/alerts add sunset delay 1"));
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(new InlineKeyboardButton().setText("Begin of civil twilight")
                .setCallbackData("/alerts add civil twilight begin delay 1"));
        row2.add(new InlineKeyboardButton().setText("End of civil twilight")
                .setCallbackData("/alerts add civil twilight end delay 1"));
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        // Build the message
        SendMessage messageToSend = new SendMessage();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(chatId);
        messageToSend.setText("When do you want to be alerted?");

        bot.reply(messageToSend);
    }

    private void sendDelays(long chatId) {
        // Get latest inserted user alert
        long id = getUserAlerts(chatId).stream()
                .max(Comparator.comparingLong(UserAlert::getId))
                .get()
                .getId();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new InlineKeyboardButton().setText("No, thanks")
                .setCallbackData(String.format("/alerts edit %d delay 0", id)));
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(new InlineKeyboardButton().setText("5m")
                .setCallbackData(String.format("/alerts edit %d delay -5", id)));
        row2.add(new InlineKeyboardButton().setText("10m")
                .setCallbackData(String.format("/alerts edit %d delay -10", id)));
        row2.add(new InlineKeyboardButton().setText("15m")
                .setCallbackData(String.format("/alerts edit %d delay -15", id)));
        row2.add(new InlineKeyboardButton().setText("30m")
                .setCallbackData(String.format("/alerts edit %d delay -30", id)));
        row2.add(new InlineKeyboardButton().setText("1h")
                .setCallbackData(String.format("/alerts edit %d delay -60", id)));
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);

        // Build the message
        SendMessage messageToSend = new SendMessage();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(chatId);
        messageToSend.setText("Do you want to be alerted in advance with respect to the time you selected?");

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
                case "nop":
                default:
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
            addAppropriateUserAlert(chatId, parameters);

            try {
                notifier.tryToInstallNotifier(chatId, 5);
                sendDelays(chatId);
            } catch (ServiceException e) {
                bot.reply(chatId, "Your alert has been saved, however we are encountering some " +
                        "technical difficulties and it may not be fired for today.");
                LOG.error("ServiceException while trying to install notifier on just created alert.", e);
            }
        } else {
            sendAlertsTypes(chatId);
        }
    }

    private void addAppropriateUserAlert(long chatId, CommandParameters parameters) {
        TimeType timeType = TimeType.DEFAULT;
        switch (parameters.alertType) {
            case "sunrise":
                if (parameters.delay == 0) {
                    timeType = TimeType.SUNRISE_TIME;
                } else if (parameters.delay < 0) {
                    timeType = TimeType.SUNRISE_TIME_ANTICIPATION;
                }
                persistenceManager.addUserAlert(new UserAlert(chatId, timeType, parameters.delay));
                break;
            case "sunset":
                if (parameters.delay == 0) {
                    timeType = TimeType.SUNSET_TIME;
                } else if (parameters.delay < 0) {
                    timeType = TimeType.SUNSET_TIME_ANTICIPATION;
                }
                persistenceManager.addUserAlert(new UserAlert(chatId, timeType, parameters.delay));
                break;
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
