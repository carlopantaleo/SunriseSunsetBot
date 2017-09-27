package com.simpleplus.telegram.bots.components;

import com.simpleplus.telegram.bots.datamodel.TimeType;
import com.simpleplus.telegram.bots.datamodel.UserAlert;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * This component handles user alerts. It:
 * <ul>
 * <li>Handles {@code /alert} commands.</li>
 * <li>Serves {@code UserAlert}s for a specific user.</li>
 * </ul>
 */
public class UserAlertsManager implements BotBean {
    private static final Logger LOG = Logger.getLogger(UserAlertsManager.class);

    private PersistenceManager persistenceManager;

    @Override
    public void init() {
        this.persistenceManager =
                (PersistenceManager) BotContext.getDefaultContext().getBean(PersistenceManager.class);
    }

    public Set<UserAlert> getUserAlerts(long chatId) {
        Set<UserAlert> userAlerts = persistenceManager.getUserAlerts(chatId);
        if (userAlerts.isEmpty()) {
            LOG.info(String.format("Going to generate default UserAlerts for chatid %s", chatId));
            generateDefaultUserAlerts(chatId);
        }

        return userAlerts;
    }

    private void generateDefaultUserAlerts(long chatId) {
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNRISE_TIME, 0));
        persistenceManager.addUserAlert(new UserAlert(chatId, TimeType.SUNSET_TIME, 0));
    }

}
