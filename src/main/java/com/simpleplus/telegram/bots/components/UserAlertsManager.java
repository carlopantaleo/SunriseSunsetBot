package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.TimeType;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.simpleplus.telegram.bots.datamodel.TimeType.*;

/**
 * This component handles user alerts. It:
 * <ul>
 * <li>Handles {@code /alert} commands.</li>
 * <li>Serves {@code UserAlert}s for a specific user.</li>
 * </ul>
 */
public class UserAlertsManager implements BotBean {
    /**
     * When issuing '/alerts add <something> delay null', the alert gets saved with a draft delay,
     * so that it's persisted but not scheduled.
     */
    public static final int DRAFT_DELAY = -100;

    private static final Logger LOG = LogManager.getLogger(UserAlertsManager.class);
    private static final String COMMAND_REGEX =
            "(?:(add|remove|edit)( [0-9]+)?" +
                    "( (?:begin|end) of (?:civil|nautical|astronomical) twilight| sunrise| sunset)?" +
                    "(?: delay (-?[0-9]{1,2}|null))?)";

    private PersistenceManager persistenceManager;
    private SunriseSunsetBot bot;
    private Notifier notifier;
    private BotScheduler scheduler;

    @Override
    public void init() {
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.notifier = (Notifier) BotContext.getDefaultContext().getBean(Notifier.class);
        this.scheduler = (BotScheduler) BotContext.getDefaultContext().getBean(BotScheduler.class);
    }

    public Set<UserAlert> getUserAlerts(long chatId) {
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(chatId);
        if (userAlerts.isEmpty()) {
            LOG.info("Going to generate default UserAlerts for chatid {}", chatId);
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
        row.add(new InlineKeyboardButton().setText("Delete...").setCallbackData("/alerts remove"));
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        // Build the message
        SendMessage messageToSend = new SendMessage();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(chatId);
        messageToSend.setText(getAlertsList(chatId));

        bot.reply(messageToSend);
    }

    private String getAlertsList(long chatId) {
        List<UserAlert> orderedUserAlerts = persistenceManager.getUserAlerts(chatId).stream()
                .sorted(Comparator.comparingLong(UserAlert::getId))
                .filter(alert -> alert.getDelay() != DRAFT_DELAY)
                .collect(Collectors.toList());

        if (orderedUserAlerts.isEmpty()) {
            return "No alerts defined. Click 'Add...' to add one.";
        }

        StringBuilder builder = new StringBuilder();
        int i = 1;
        for (UserAlert alert : orderedUserAlerts) {
            builder.append("#")
                    .append(i)
                    .append(": ");

            if (alert.getDelay() != 0) {
                builder.append(Math.abs(alert.getDelay()))
                        .append(" minutes ")
                        .append(alert.getDelay() > 0 ? "after " : "before ")
                        .append(alert.getTimeType().getReadableName().toLowerCase());
            } else {
                builder.append(alert.getTimeType().getReadableName());
            }

            builder.append("\n");
            i++;
        }

        return builder.toString();
    }

    private void sendAlertsTypes(long chatId, CommandParameters parameters) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Build the keyboard (adds a default delay (null) as a workaround in order to not overwrite any existing
        // no-delay alert.
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new InlineKeyboardButton().setText("Sunrise").setCallbackData("/alerts add sunrise delay null"));
        row1.add(new InlineKeyboardButton().setText("Sunset").setCallbackData("/alerts add sunset delay null"));
        keyboard.add(row1);

        addTwilightRow(keyboard, "civil");
        addTwilightRow(keyboard, "nautical");
        addTwilightRow(keyboard, "astronomical");

        keyboardMarkup.setKeyboard(keyboard);

        replyWithEditMessage(chatId, parameters, keyboardMarkup, "When do you want to be alerted?");
    }

    private void addTwilightRow(List<List<InlineKeyboardButton>> keyboard, String twilightType) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(new InlineKeyboardButton().setText("Begin of " + twilightType + " twilight")
                .setCallbackData("/alerts add begin of " + twilightType + " twilight delay null"));
        row.add(new InlineKeyboardButton().setText("End of " + twilightType + " twilight")
                .setCallbackData("/alerts add end of " + twilightType + " twilight delay null"));
        keyboard.add(row);
    }

    private void sendDelays(long chatId, CommandParameters parameters) {
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

        replyWithEditMessage(chatId, parameters, keyboardMarkup,
                "Do you want to be alerted in advance with respect to the time you selected?");
    }

    public void handleCommand(long chatId, String commandArguments, long messageId) {
        CommandParameters parameters = extractParameters(commandArguments);
        parameters.messageId = messageId;

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
        if (parameters.alertId != 0) {
            LOG.info("ChatId {}: Going to edit alert {} and reschedule all alerts.", chatId, parameters.alertId);
            UserAlert editedUserAlert = createEditedUserAlert(chatId, parameters);
            if (editedUserAlert != null) {
                boolean edited = persistenceManager.editUserAlert(editedUserAlert);
                if (!edited) {
                    replyWithEditMessage(chatId, parameters, "Alert already exists.");
                    return;
                }

                reinstallNotifiers(chatId, parameters, "Alert has been created.",
                        "Your alert has been saved, however we are encountering some " +
                                "technical difficulties and it may not be fired for today.",
                        "ServiceException while trying to install notifiers on just edited alert.");
            } else {
                bot.reply(chatId, "An error occurred. For further information, please contact support.");
            }
        }
    }

    private void handleAdd(long chatId, CommandParameters parameters) {
        if (parameters.hasAlertType()) {
            boolean added = addAppropriateUserAlert(chatId, parameters);
            if (!added && parameters.delay != DRAFT_DELAY /* Threat draft alerts separately */) {
                replyWithEditMessage(chatId, parameters, "Alert already exists.");
                return;
            }

            try {
                if (parameters.delay != DRAFT_DELAY) {
                    notifier.tryToInstallNotifiers(chatId, 5);
                } else {
                    sendDelays(chatId, parameters);
                }
            } catch (ServiceException e) {
                bot.reply(chatId, "Your alert has been added, however we are encountering some " +
                        "technical difficulties and it may not be fired for today.");
                LOG.error("ServiceException while trying to install notifier on just created alert.", e);
            }
        } else {
            sendAlertsTypes(chatId, parameters);
        }
    }

    private void handleRemove(long chatId, CommandParameters parameters) {
        if (parameters.alertId != 0) {
            LOG.info("ChatId {}: Going to remove alert {} and reschedule all alerts.", chatId, parameters.alertId);
            persistenceManager.deleteUserAlert(chatId, parameters.alertId);

            reinstallNotifiers(chatId, parameters, "Alert has been deleted.",
                    "Your alert has been deleted, however we are encountering some " +
                            "technical difficulties and it may still be fired for today.",
                    "ServiceException while trying to install notifiers on just deleted alert.");
        } else {
            sendAlertsDeletionList(chatId, parameters);
        }
    }

    private void reinstallNotifiers(long chatId,
                                    CommandParameters parameters,
                                    String correctFeedbackMessage,
                                    String errorFeedbackMessage,
                                    String logMessage) {
        try {
            scheduler.cancelAllScheduledMessages(chatId);
            notifier.tryToInstallNotifiers(chatId, 5);
            replyWithEditMessage(chatId, parameters, null, correctFeedbackMessage);
        } catch (Exception e) {
            bot.reply(chatId, errorFeedbackMessage);
            LOG.error(logMessage, e);
        }
    }

    private UserAlert createEditedUserAlert(long chatId, CommandParameters parameters) {
        for (UserAlert alert : persistenceManager.getUserAlerts(chatId)) {
            if (alert.getId() == parameters.alertId) {
                parameters.alertType = alert.getTimeType().getReadableName().toLowerCase();
                alert.setDelay(parameters.delay);
                alert.setTimeType(getAppropriateTimeType(parameters));
                return alert;
            }
        }
        return null;
    }

    private void sendAlertsDeletionList(long chatId, CommandParameters parameters) {
        List<UserAlert> orderedUserAlerts = persistenceManager.getUserAlerts(chatId).stream()
                .sorted(Comparator.comparingLong(UserAlert::getId))
                .collect(Collectors.toList());

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        int alertsPerRow = (int) Math.ceil((double) orderedUserAlerts.size() /
                Math.ceil((double) orderedUserAlerts.size() / 8));
        int i = 1;
        for (UserAlert alert : orderedUserAlerts) {
            row.add(new InlineKeyboardButton().setText("#" + i)
                    .setCallbackData("/alerts remove " + alert.getId()));

            // Start a new line if too many buttons
            if (i % alertsPerRow == 0) {
                keyboard.add(row);
                row = new ArrayList<>();
            }

            i++;
        }

        if (row.size() != 0) { // May be an empty row
            keyboard.add(row);
        }

        keyboardMarkup.setKeyboard(keyboard);
        replyWithEditMessage(chatId, parameters, keyboardMarkup,
                "Which alert do you want to delete?\n" + getAlertsList(chatId));
    }

    private void replyWithEditMessage(long chatId,
                                      CommandParameters parameters,
                                      InlineKeyboardMarkup keyboardMarkup,
                                      String text) {
        EditMessageText messageToSend = new EditMessageText();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(chatId);
        if (parameters.messageId != 0) {
            messageToSend.setMessageId((int) parameters.messageId);
        }
        messageToSend.setText(text);

        bot.reply(messageToSend);
    }

    private void replyWithEditMessage(long chatId,
                                      CommandParameters parameters,
                                      String text) {
        replyWithEditMessage(chatId, parameters, null, text);
    }

    private boolean addAppropriateUserAlert(long chatId, CommandParameters parameters) {
        TimeType timeType = getAppropriateTimeType(parameters);

        if (timeType != TimeType.DEFAULT) {
            return persistenceManager.addUserAlert(new UserAlert(chatId, timeType, parameters.delay));
        }

        return true;
    }

    private TimeType getAppropriateTimeType(CommandParameters parameters) {
        TimeType timeType = DEFAULT;

        switch (parameters.alertType) {
            case "sunrise":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        SUNRISE_TIME,
                        SUNRISE_TIME_ANTICIPATION);
                break;
            case "sunset":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        SUNSET_TIME,
                        SUNSET_TIME_ANTICIPATION);
                break;
            case "begin of civil twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        CIVIL_TWILIGHT_BEGIN_TIME,
                        CIVIL_TWILIGHT_BEGIN_TIME_ANTICIPATION);
                break;
            case "end of civil twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        CIVIL_TWILIGHT_END_TIME,
                        CIVIL_TWILIGHT_END_TIME_ANTICIPATION);
                break;
            case "begin of nautical twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        NAUTICAL_TWILIGHT_BEGIN_TIME,
                        NAUTICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION);
                break;
            case "end of nautical twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        NAUTICAL_TWILIGHT_END_TIME,
                        NAUTICAL_TWILIGHT_END_TIME_ANTICIPATION);
                break;
            case "begin of astronomical twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        ASTRONOMICAL_TWILIGHT_BEGIN_TIME,
                        ASTRONOMICAL_TWILIGHT_BEGIN_TIME_ANTICIPATION);
                break;
            case "end of astronomical twilight":
                timeType = getTimeTypeFromDelay(parameters.delay,
                        ASTRONOMICAL_TWILIGHT_END_TIME,
                        ASTRONOMICAL_TWILIGHT_END_TIME_ANTICIPATION);
                break;
        }

        return timeType;
    }

    private TimeType getTimeTypeFromDelay(long delay,
                                          TimeType timeIfDelayEq0,
                                          TimeType timeIfDelayLt0) {
        if (delay == 0) {
            return timeIfDelayEq0;
        } else if (delay < 0) {
            return timeIfDelayLt0;
        }

        return DEFAULT;
    }

    private CommandParameters extractParameters(String commandArguments) {
        Pattern pattern = Pattern.compile(COMMAND_REGEX);
        Matcher matcher = pattern.matcher(commandArguments);

        CommandParameters parameters = new CommandParameters();
        if (matcher.find()) {
            String delay = Optional.ofNullable(matcher.group(4)).orElse("0").trim();
            parameters.delay = "null".equals(delay) ? DRAFT_DELAY : Long.parseLong(delay);
            parameters.command = Optional.ofNullable(matcher.group(1)).orElse("").trim();
            parameters.alertType = Optional.ofNullable(matcher.group(3)).orElse("").trim();
            parameters.alertId = Long.parseLong(Optional.ofNullable(matcher.group(2)).orElse("0").trim());
        }

        return parameters;
    }

    private class CommandParameters {
        public String command = "";
        public String alertType = "";
        public long alertId = 0;
        public long delay = 0;
        public long messageId = 0;

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
