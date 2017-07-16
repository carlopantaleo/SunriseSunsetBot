package com.simpleplus.telegram.bots.components;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple object that can store many beans which form the bot context
 */
public class BotContext {
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

    public void addBean(String name, BotBean bean) {
        beans.put(name, bean);
    }

    public BotBean getBean(String name) {
        return beans.get(name);
    }
}
