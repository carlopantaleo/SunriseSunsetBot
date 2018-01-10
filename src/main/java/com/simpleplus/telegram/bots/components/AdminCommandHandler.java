package com.simpleplus.telegram.bots.components;

import com.google.common.annotations.VisibleForTesting;
import com.simpleplus.telegram.bots.datamodel.Step;
import com.simpleplus.telegram.bots.datamodel.UserState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.objects.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.simpleplus.telegram.bots.components.SunriseSunsetBot.getChatId;

public class AdminCommandHandler extends CommandHandler implements BotBean {
    private static final Logger LOG = LogManager.getLogger(AdminCommandHandler.class);

    private SunriseSunsetBot bot;
    private PersistenceManager persistenceManager;

    @Override
    public void init() {
        this.bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    public boolean verifyAdminToken(Update update) {
        return getCommand(update) == Command.SET_ADMINISTRATOR &&
                getCommandArguments(update).equals(bot.getBotToken());
    }

    /**
     * This method can be used to broadcast a message to the bot administrators.
     *
     * @param message the message to be broadcasted.
     */
    public void broadcastToAdmins(String message) {
        broadcast(message, true);
    }

    @Override
    public void handleCommand(Update update) {
        String commandArgs = getCommandArguments(update);

        switch (commandArgs.split(" ")[0]) {
            case "broadcast": {
                String message = getBroadcastMessage(commandArgs);
                broadcast(message);
            }
            break;

            case "send": {
                send(commandArgs, getChatId(update));
            }
            break;
        }
    }

    private void send(String commandArgs, long adminChatId) {
        long chatIdToSendTo;
        try {
            chatIdToSendTo = Long.parseLong(getCommandOptions(commandArgs).get("chatid"));
        } catch (NumberFormatException e) {
            chatIdToSendTo = 0;
        }

        if (chatIdToSendTo == 0) {
            bot.reply(adminChatId, "Missing 'chatid' option.");
            return;
        }

        UserState userState = persistenceManager.getUserState(chatIdToSendTo);

        if (userState != null && userState.getStep() != Step.EXPIRED) {
            bot.reply(chatIdToSendTo, getSendMessage(commandArgs));
        } else {
            bot.reply(adminChatId, String.format("ChatId %d is invalid!", chatIdToSendTo));
        }
    }

    @VisibleForTesting
    String getSendMessage(String commandArgs) {
        Pattern pattern = Pattern.compile("send chatid=[0-9]* (.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(commandArgs);

        return matcher.find() ? matcher.group(1) : "";
    }

    private void broadcast(String message, boolean onlyToAdmins) {
        Map<Long, UserState> userStatesMap =
                persistenceManager.getUserStatesMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().getStep() != Step.EXPIRED) // Get all chats with step != expired
                        .filter(e -> (e.getValue().isAdmin() && onlyToAdmins) || !onlyToAdmins) // If onlyToAdmins, get only admin chats
                        .collect(Collectors.toMap(s -> s.getKey(), p -> p.getValue()));

        LOG.info("Broadcasting message to {} users.", userStatesMap.size());

        for (Map.Entry<Long, UserState> entry : userStatesMap.entrySet()) {
            bot.reply(entry.getKey(), message);
        }
    }

    private void broadcast(String message) {
        broadcast(message, false);
    }

    @VisibleForTesting
    String getBroadcastMessage(String commandArgs) {
        Pattern pattern = Pattern.compile("broadcast (.*)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(commandArgs);

        return matcher.find() ? matcher.group(1) : "";
    }

    @VisibleForTesting
    Map<String, String> getCommandOptions(String commandArgs) {
        String[] tokens = commandArgs.split(" ");
        Map<String, String> options = new HashMap<>();

        // Get only options and map to a Map
        return Arrays.stream(tokens)
                .filter(s -> s.contains("="))
                .collect(Collectors.toMap(s -> s.split("=")[0], t -> t.split("=")[1]));
    }

    public boolean isAdminChat(long chatId) {
        return persistenceManager.getUserState(chatId).isAdmin();
    }
}
