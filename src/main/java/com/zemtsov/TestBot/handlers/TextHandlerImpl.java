package com.zemtsov.TestBot.handlers;

import com.zemtsov.TestBot.service.TelegramBot;
import org.springframework.stereotype.Component;

@Component
public class TextHandlerImpl implements TextHandler {
    @Override
    public void handleText(String text, long chatId, TelegramBot bot) {
        switch (bot.getBotState()) {
            case SET_EMAIL:
                bot.setUsersEmail(text, chatId);
                break;
            case CREATE_NOTE:
                bot.createNote(text, chatId);
                break;
            case EDIT_NOTE:
                bot.editNote(text, chatId);
                break;
            default:
                bot.sendMessage(chatId, "Sorry, I don't understand what you're saying\nTry to send /start");
                break;
        }
    }
}
