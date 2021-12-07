package com.simpleplus.telegram.bots.components;

import com.google.common.collect.ImmutableMap;
import com.simpleplus.telegram.bots.datamodel.TimeType;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import com.simpleplus.telegram.bots.exceptions.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.simpleplus.telegram.bots.datamodel.TimeType.*;

/**
 * This component handles user alerts. It: <ul> <li>Handles {@code /alert} commands.</li> <li>Serves {@code UserAlert}s
 * for a specific user.</li> </ul>
 */
public class UserAlertsManager implements BotBean {
    /**
     * When issuing '/alerts add <something> delay null', the alert gets saved with a draft delay, so that it's
     * persisted but not scheduled.
     */
    public static final int DRAFT_DELAY = -100;

    private static final Logger LOG = LogManager.getLogger(UserAlertsManager.class);
    private static final String COMMAND_REGEX =
            "(?:(add|remove|edit)( [0-9]+)?" +
                    "( (?:begin|end) of (?:(?:civil|nautical|astronomical) twilight|golden hour)| sunrise| sunset| " +
                    "moonrise| moonset)?" +
                    "(?: delay (-?[0-9]{1,2}|null))?)";
    private static final ImmutableMap<String, TimeTypesTuple> TIMES_TUPLE =
            new ImmutableMap.Builder<String, TimeTypesTuple>()
                    .put("sunrise", TimeTypesTuple.of(SUNRISE, SUNRISE_ANTICIPATION))
                    .put("sunset", TimeTypesTuple.of(SUNSET, SUNSET_ANTICIPATION))
                    .put("begin of civil twilight",
                            TimeTypesTuple.of(CIVIL_TWILIGHT_BEGIN, CIVIL_TWILIGHT_BEGIN_ANTICIPATION))
                    .put("end of civil twilight",
                            TimeTypesTuple.of(CIVIL_TWILIGHT_END, CIVIL_TWILIGHT_END_ANTICIPATION))
                    .put("begin of nautical twilight",
                            TimeTypesTuple.of(NAUTICAL_TWILIGHT_BEGIN, NAUTICAL_TWILIGHT_BEGIN_ANTICIPATION))
                    .put("end of nautical twilight",
                            TimeTypesTuple.of(NAUTICAL_TWILIGHT_END, NAUTICAL_TWILIGHT_END_ANTICIPATION))
                    .put("begin of astronomical twilight",
                            TimeTypesTuple.of(ASTRONOMICAL_TWILIGHT_BEGIN, ASTRONOMICAL_TWILIGHT_BEGIN_ANTICIPATION))
                    .put("end of astronomical twilight",
                            TimeTypesTuple.of(ASTRONOMICAL_TWILIGHT_END, ASTRONOMICAL_TWILIGHT_END_ANTICIPATION))
                    .put("begin of golden hour",
                            TimeTypesTuple.of(GOLDEN_HOUR_BEGIN, GOLDEN_HOUR_BEGIN_ANTICIPATION))
                    .put("end of golden hour",
                            TimeTypesTuple.of(GOLDEN_HOUR_END, GOLDEN_HOUR_END_ANTICIPATION))
                    .put("moonrise",
                            TimeTypesTuple.of(MOONRISE, MOONRISE_ANTICIPATION))
                    .put("moonset",
                            TimeTypesTuple.of(MOONSET, MOONSET_ANTICIPATION))
                    .build();

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
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNRISE, 0));
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNSET, 0));
        return persistenceManager.getUserAlerts(chatId);
    }

    public boolean validateSyntax(String update) {
        return update.matches(COMMAND_REGEX);
    }

    public void sendAlertsListAndActions(long chatId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(InlineKeyboardButton.builder().text("Add...").callbackData("/alerts add").build());
        row.add(InlineKeyboardButton.builder().text("Delete...").callbackData("/alerts remove").build());
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        // Build the message
        SendMessage messageToSend = new SendMessage();
        messageToSend.setReplyMarkup(keyboardMarkup);
        messageToSend.setChatId(String.valueOf(chatId));
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
        List<List<InlineKeyboardButton>> keyboard = buildAddAlertsKeyboard();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);

        replyWithEditMessage(chatId, parameters, keyboardMarkup, "When do you want to be alerted?");
    }

    private List<List<InlineKeyboardButton>> buildAddAlertsKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        int col = 0;
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (Map.Entry<String, TimeTypesTuple> entry : TIMES_TUPLE.entrySet()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(entry.getValue().time.getReadableName())
                    .callbackData(String.format("/alerts add %s delay null", entry.getKey()))
                    .build();
            row.add(button);
            LOG.debug("Added button on row {}, col {}, button {}", keyboard.size() + 1, row.size(), button);

            col++;

            // Two buttons per row
            if (col % 2 == 0) {
                keyboard.add(row);
                row = new ArrayList<>();
            }
        }

        return keyboard;
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
        row1.add(InlineKeyboardButton.builder()
                .text("No, thanks")
                .callbackData(String.format("/alerts edit %d delay 0", id))
                .build());
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(InlineKeyboardButton.builder()
                .text("5m")
                .callbackData(String.format("/alerts edit %d delay -5", id))
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("10m")
                .callbackData(String.format("/alerts edit %d delay -10", id))
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("15m")
                .callbackData(String.format("/alerts edit %d delay -15", id))
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("30m")
                .callbackData(String.format("/alerts edit %d delay -30", id))
                .build());
        row2.add(InlineKeyboardButton.builder()
                .text("1h")
                .callbackData(String.format("/alerts edit %d delay -60", id))
                .build());
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
            boolean added = addAppropriatedUserAlert(chatId, parameters);
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
                alert.setTimeType(getAppropriatedTimeType(parameters));
                return alert;
            }
        }
        return null;
    }

    private void sendAlertsDeletionList(long chatId, CommandParameters parameters) {
        List<UserAlert> orderedUserAlerts = persistenceManager.getUserAlerts(chatId).stream()
                .sorted(Comparator.comparingLong(UserAlert::getId))
                .toList();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        // Build the keyboard
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        int alertsPerRow = (int) Math.ceil((double) orderedUserAlerts.size() /
                Math.ceil((double) orderedUserAlerts.size() / 8));
        int i = 1;
        for (UserAlert alert : orderedUserAlerts) {
            row.add(InlineKeyboardButton.builder()
                    .text("#" + i)
                    .callbackData("/alerts remove " + alert.getId())
                    .build());

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
        messageToSend.setChatId(String.valueOf(chatId));
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

    private boolean addAppropriatedUserAlert(long chatId, CommandParameters parameters) {
        TimeType timeType = getAppropriatedTimeType(parameters);

        if (timeType != TimeType.DEFAULT) {
            return persistenceManager.addUserAlert(new UserAlert(chatId, timeType, parameters.delay));
        }

        return true;
    }

    private TimeType getAppropriatedTimeType(CommandParameters parameters) {
        TimeTypesTuple times = TIMES_TUPLE.get(parameters.alertType);
        if (times != null) {
            return getTimeTypeFromDelay(parameters.delay, times.time, times.anticipation);
        } else {
            return DEFAULT;
        }
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

    private static class TimeTypesTuple {
        private final TimeType time;
        private final TimeType anticipation;

        private TimeTypesTuple(TimeType time, TimeType anticipation) {
            this.time = time;
            this.anticipation = anticipation;
        }

        public static TimeTypesTuple of(TimeType time, TimeType anticipation) {
            return new TimeTypesTuple(time, anticipation);
        }
    }
}
