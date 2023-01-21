package com.zemtsov.TestBot.handlers;

import com.zemtsov.TestBot.service.TelegramBot;
import com.zemtsov.TestBot.util.BotState;

public interface TextHandler {
    void handleText(String text, long chatId, TelegramBot bot);
}
