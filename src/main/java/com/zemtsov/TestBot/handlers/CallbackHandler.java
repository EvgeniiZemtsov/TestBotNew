package com.zemtsov.TestBot.handlers;

import com.zemtsov.TestBot.service.TelegramBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackHandler {
    void handleCallback(CallbackQuery callbackQuery, TelegramBot bot);
}
