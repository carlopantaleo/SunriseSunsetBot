package com.simpleplus.telegram.bots.components;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A object that can store many beans which form the bot context. In this implementation, each bean is a
 * singleton and there can't exists to instances of the same {@code BotBean} withing the same {@code BotContext}.
 */
public class BotContext {
    private static BotContext defaultContext;
    private static final Logger LOG = Logger.getLogger(BotContext.class);


    private Map<String, BotBean> beans = new HashMap<>();

    public static BotContext getDefaultContext() {
        return defaultContext;
    }

    public static void setDefaultContext(BotContext defaultContext) {
        BotContext.defaultContext = defaultContext;
    }

    public void initContext() {
        for (Map.Entry entry : beans.entrySet()) {
            ((BotBean) entry.getValue()).init();
        }
    }

    /**
     * Adds a {@code BotBean} to the {@code BotContext}.
     * @param clazz The Class of the {@code BotBean}.
     * @param beanArgs Arguments to be passed to the constructor of the {@code BotBean}.
     */
    public void addBean(Class<? extends BotBean> clazz, Object... beanArgs) {
        try {
            if (beanArgs != null) {
                beans.put(clazz.getCanonicalName(), (BotBean) clazz.getConstructors()[0].newInstance(beanArgs));
            } else {
                beans.put(clazz.getCanonicalName(), clazz.newInstance());
            }
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            LOG.fatal("Unable to instantiate class " + clazz.toString() + ": InstantiationException.", e);
            throw new Error("Failed to initialise BotContext.", e);
        }
    }

    public void addBean(Class<? extends BotBean> clazz) {
        addBean(clazz, null);
    }

    public BotBean getBean(Class<? extends BotBean> clazz) {
        return beans.get(clazz.getCanonicalName());
    }
}
