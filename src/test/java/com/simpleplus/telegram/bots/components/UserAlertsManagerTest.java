package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.MainTest;
import com.simpleplus.telegram.bots.datamodel.*;
import com.simpleplus.telegram.bots.mocks.SunriseSunsetBotMock;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class UserAlertsManagerTest {
    private PersistenceManager persistenceManager;
    private UserAlertsManager userAlertsManager;
    private SunriseSunsetBot bot;


    @Before
    public void init() {
        MainTest.initDefaultBotContext();
        persistenceManager = (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
        userAlertsManager = (UserAlertsManager) BotContext.getDefaultContext().getBean(UserAlertsManager.class);
        bot = (SunriseSunsetBot) BotContext.getDefaultContext().getBean(SunriseSunsetBot.class);
    }

    @Test
    public void noUserAlertsResultsInAddingDefaults() {
        long testChatId = 101L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        Set<UserAlert> userAlerts = userAlertsManager.getUserAlerts(testChatId);
        assertEquals(2, userAlerts.size());
    }

    @Test
    public void validateSyntaxWorks() {
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 5"));
        assertTrue(userAlertsManager.validateSyntax("add sunset delay 55"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay 555"));
        assertFalse(userAlertsManager.validateSyntax("add sunset delay"));
        assertFalse(userAlertsManager.validateSyntax("add civil twilight"));
        assertTrue(userAlertsManager.validateSyntax("add begin of civil twilight"));
        assertTrue(userAlertsManager.validateSyntax("add end of civil twilight"));
        assertTrue(userAlertsManager.validateSyntax("add end of civil twilight delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add end of civil twilight delay -4"));
        assertTrue(userAlertsManager.validateSyntax("add end of civil twilight delay null"));
        assertTrue(userAlertsManager.validateSyntax("add end of nautical twilight delay null"));
        assertTrue(userAlertsManager.validateSyntax("add end of astronomical twilight delay null"));
        assertTrue(userAlertsManager.validateSyntax("add begin of golden hour delay null"));
        assertTrue(userAlertsManager.validateSyntax("add end of golden hour delay null"));
        assertTrue(userAlertsManager.validateSyntax("add"));
        assertTrue(userAlertsManager.validateSyntax("remove 5"));
        assertTrue(userAlertsManager.validateSyntax("remove"));
        assertFalse(userAlertsManager.validateSyntax("remove 5L"));
        assertTrue(userAlertsManager.validateSyntax("edit 5 sunset delay 7"));
        assertTrue(userAlertsManager.validateSyntax("edit 5 delay 7"));
    }

    @Test
    public void dontInsertDuplicateAlerts1() {
        long testChatId = 102L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);
        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);
        assertNotEquals("Alert already exists.", ((SunriseSunsetBotMock) bot).getLastTextMessage());

        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());
    }

    @Test
    public void dontInsertDuplicateAlerts2() {
        long testChatId = 103L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);

        // Retrieve max id -- i.e. id of the only inserted user alert for this chatId
        long maxUserAlert = 0;
        for (UserAlert userAlert : persistenceManager.getUserAlerts(testChatId)) {
            maxUserAlert = Math.max(userAlert.getId(), maxUserAlert);
        }

        userAlertsManager.handleCommand(testChatId, "edit " + (maxUserAlert) + " delay 0", 1L);
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());

        userAlertsManager.handleCommand(testChatId, "add sunrise delay null", 1L);
        userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(2, userAlerts.size());

        userAlertsManager.handleCommand(testChatId, "edit " + (maxUserAlert + 1) + " delay 0", 1L);
        userAlerts = persistenceManager.getUserAlerts(testChatId);
        assertEquals(1, userAlerts.size());
        assertEquals("Alert already exists.", ((SunriseSunsetBotMock) bot).getLastTextMessage().getText());
    }

    @Test
    public void addAlertsKeyboardIsSentCorrectly() throws Exception {
        long testChatId = 200L;
        persistenceManager.setUserState(testChatId, new UserState(
                new Coordinates(0, 0),
                Step.RUNNING,
                false
        ));

        userAlertsManager.handleCommand(testChatId, "add", 1L);
        SunriseSunsetBotMock bot = (SunriseSunsetBotMock) this.bot;
        SendMessage lastMessage = bot.getLastTextMessage();
        InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) lastMessage.getReplyMarkup();
        List<List<InlineKeyboardButton>> keyboard = replyMarkup.getKeyboard();

        assertEquals(TimeType.SUNRISE.getReadableName(), keyboard.get(0).get(0).getText());
        assertEquals(TimeType.SUNSET.getReadableName(), keyboard.get(0).get(1).getText());
        assertEquals(TimeType.CIVIL_TWILIGHT_BEGIN.getReadableName(), keyboard.get(1).get(0).getText());
        assertEquals(TimeType.CIVIL_TWILIGHT_END.getReadableName(), keyboard.get(1).get(1).getText());
        assertEquals(TimeType.NAUTICAL_TWILIGHT_BEGIN.getReadableName(), keyboard.get(2).get(0).getText());
        assertEquals(TimeType.NAUTICAL_TWILIGHT_END.getReadableName(), keyboard.get(2).get(1).getText());
        assertEquals(TimeType.ASTRONOMICAL_TWILIGHT_BEGIN.getReadableName(), keyboard.get(3).get(0).getText());
        assertEquals(TimeType.ASTRONOMICAL_TWILIGHT_END.getReadableName(), keyboard.get(3).get(1).getText());
        assertEquals(TimeType.GOLDEN_HOUR_BEGIN.getReadableName(), keyboard.get(4).get(0).getText());
        assertEquals(TimeType.GOLDEN_HOUR_END.getReadableName(), keyboard.get(4).get(1).getText());
        assertEquals(TimeType.MOONRISE.getReadableName(), keyboard.get(5).get(0).getText());
        assertEquals(TimeType.MOONSET.getReadableName(), keyboard.get(5).get(1).getText());

        assertEquals("/alerts add sunrise delay null", keyboard.get(0).get(0).getCallbackData());
        assertEquals("/alerts add sunset delay null", keyboard.get(0).get(1).getCallbackData());
        assertEquals("/alerts add begin of civil twilight delay null", keyboard.get(1).get(0).getCallbackData());
        assertEquals("/alerts add end of civil twilight delay null", keyboard.get(1).get(1).getCallbackData());
        assertEquals("/alerts add begin of nautical twilight delay null", keyboard.get(2).get(0).getCallbackData());
        assertEquals("/alerts add end of nautical twilight delay null", keyboard.get(2).get(1).getCallbackData());
        assertEquals("/alerts add begin of astronomical twilight delay null", keyboard.get(3).get(0).getCallbackData());
        assertEquals("/alerts add end of astronomical twilight delay null", keyboard.get(3).get(1).getCallbackData());
        assertEquals("/alerts add begin of golden hour delay null", keyboard.get(4).get(0).getCallbackData());
        assertEquals("/alerts add end of golden hour delay null", keyboard.get(4).get(1).getCallbackData());
        assertEquals("/alerts add moonrise delay null", keyboard.get(5).get(0).getCallbackData());
        assertEquals("/alerts add moonset delay null", keyboard.get(5).get(1).getCallbackData());
    }

    @Test
    public void getAppropriatedTimeTypeWorks() throws Exception {
        long iChatId = 104;
        for (int i = 0; i < 12; i++) {
            persistenceManager.setUserState(iChatId + i, new UserState(
                    new Coordinates(0, 0),
                    Step.RUNNING,
                    false
            ));
        }

        userAlertsManager.handleCommand(iChatId, "add sunrise delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add sunrise delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add sunset delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add sunset delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add begin of civil twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add begin of civil twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add end of civil twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add end of civil twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add begin of nautical twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add begin of nautical twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add end of nautical twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add end of nautical twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add begin of astronomical twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add begin of astronomical twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add end of astronomical twilight delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add end of astronomical twilight delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add begin of golden hour delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add begin of golden hour delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add end of golden hour delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add end of golden hour delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add moonrise delay 0", 1L);
        userAlertsManager.handleCommand(iChatId++, "add moonrise delay -5", 1L);
        userAlertsManager.handleCommand(iChatId, "add moonset delay 0", 1L);
        userAlertsManager.handleCommand(iChatId, "add moonset delay -5", 1L);

        iChatId = 104;
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.SUNRISE, 0)));
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.SUNRISE_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.SUNSET, 0)));
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.SUNSET_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.CIVIL_TWILIGHT_BEGIN, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.CIVIL_TWILIGHT_BEGIN_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.CIVIL_TWILIGHT_END, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.CIVIL_TWILIGHT_END_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.NAUTICAL_TWILIGHT_BEGIN, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.NAUTICAL_TWILIGHT_BEGIN_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.NAUTICAL_TWILIGHT_END, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.NAUTICAL_TWILIGHT_END_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(
                setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.ASTRONOMICAL_TWILIGHT_BEGIN, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.ASTRONOMICAL_TWILIGHT_BEGIN_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(
                setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.ASTRONOMICAL_TWILIGHT_END, 0)));
        assertTrue(setContainsUserAlert(userAlerts,
                new UserAlert(iChatId, TimeType.ASTRONOMICAL_TWILIGHT_END_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.GOLDEN_HOUR_BEGIN, 0)));
        assertTrue(
                setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.GOLDEN_HOUR_BEGIN_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.GOLDEN_HOUR_END, 0)));
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.GOLDEN_HOUR_END_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.MOONRISE, 0)));
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.MOONRISE_ANTICIPATION, -5)));
        iChatId++;
        userAlerts = persistenceManager.getUserAlerts(iChatId);
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.MOONSET, 0)));
        assertTrue(setContainsUserAlert(userAlerts, new UserAlert(iChatId, TimeType.MOONSET_ANTICIPATION, -5)));
    }

    private boolean setContainsUserAlert(Set<UserAlert> set, UserAlert expected) {
        for (UserAlert userAlert : set) {
            if (userAlert.equalsNoId(expected)) {
                return true;
            }
        }

        return false;
    }
}