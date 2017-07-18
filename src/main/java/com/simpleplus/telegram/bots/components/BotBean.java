package com.simpleplus.telegram.bots.components;

/**
 * Classes implementing this interface can be inserted in a {@code BotContext}. Every {@code BotBean} must be a
 * singleton within the same {@code BotContext} and must have at most one constructor. If more than one constructor is
 * defined, which one will be used for instantiation in the {@code BotContext} is undefined and may lead to errors.
 */
public interface BotBean {
    default void init() {
        //Empty
    }
}
