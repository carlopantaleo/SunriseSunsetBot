package com.simpleplus.telegram.bots.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * A object that can store many beans which form the bot context. In this implementation, each bean is a
 * singleton and there can't exists to instances of the same {@code BotBean} withing the same {@code BotContext}.
 */
public class BotContext {
    private static final Logger LOG = LogManager.getLogger(BotContext.class);
    private static BotContext defaultContext;
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
     *
     * @param clazz    The Class of the {@code BotBean}.
     * @param beanArgs Arguments to be passed to the constructor of the {@code BotBean}.
     */
    public void addBean(Class<? extends BotBean> clazz, Object... beanArgs) {
        try {
            String canonicalName = clazz.getCanonicalName();
            LOG.debug("Adding bean {}.", canonicalName);

            if (beanArgs != null) {
                beans.put(canonicalName, (BotBean) clazz.getConstructors()[0].newInstance(beanArgs));
            } else {
                beans.put(canonicalName, clazz.newInstance());
            }
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            LOG.fatal("Unable to instantiate class " + clazz.toString() + ": InstantiationException.", e);
            throw new Error("Failed to initialise BotContext.", e);
        }
    }

    /**
     * Adds a already-constructed {@link BotBean}
     *
     * @param clazz the Class of the {@code BotBean}. It may be a superclass of the one of {@code bean}.
     * @param bean  constructed {@link BotBean} to add.
     */
    public void addBean(Class<? extends BotBean> clazz, BotBean bean) {
        String canonicalName = clazz.getCanonicalName();
        LOG.debug("Adding already constructed bean {}.", canonicalName);
        beans.put(canonicalName, bean);
    }

    public void addBean(Class<? extends BotBean> clazz) {
        addBean(clazz, (Object[]) null);
    }

    public BotBean getBean(Class<? extends BotBean> clazz) {
        return beans.get(clazz.getCanonicalName());
    }
}
