package com.zemtsov.TestBot.handlers;

import com.zemtsov.TestBot.service.NoteService;
import com.zemtsov.TestBot.service.TelegramBot;
import com.zemtsov.TestBot.service.UserService;
import com.zemtsov.TestBot.util.BotState;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CallbackHandlerImpl implements CallbackHandler {

    @Override
    public void handleCallback(CallbackQuery callbackQuery, TelegramBot bot) {
        UserService userService = bot.getUserService();
        NoteService noteService = bot.getNoteService();

        Message message = callbackQuery.getMessage();
        log.info("Handling calbackQuery : " + callbackQuery.getData());

        String[] tokens = callbackQuery.getData().split(":");
        if (tokens[0].equals("Gender")) {
            switch (tokens[1]) {
                case "Male":
                    userService.updateUser(message.getChatId(), null, "male");
                    break;
                case "Female":
                    userService.updateUser(message.getChatId(), null, "female");
                    break;
                default:

            }
            try {
                bot.execute(AnswerCallbackQuery.builder()
                        .cacheTime(10)
                        .text("You've successfully set your gender. Now it's " + tokens[1])
                        .showAlert(true)
                        .callbackQueryId(callbackQuery.getId())
                        .build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else if (tokens[0].equals("NoteAction")) {
            switch (tokens[1]) {
                case "Edit":
                    bot.sendMessage(callbackQuery.getMessage().getChatId(), "Enter new note's text here ⬇️");
                    bot.setBotState(BotState.EDIT_NOTE);
                    if (!bot.getBuffer().isEmpty()) {
                        bot.bufferClear();
                    }
                    bot.bufferAppend(tokens[2]);
                    bot.bufferAppend(String.valueOf(callbackQuery.getMessage().getMessageId()));
                    break;
                case "Delete":
                    noteService.deleteNoteById(Long.parseLong(tokens[2]));
                    try {
                        bot.execute(AnswerCallbackQuery.builder()
                                .cacheTime(10)
                                .text("Note was deleted")
                                .showAlert(true)
                                .callbackQueryId(callbackQuery.getId())
                                .build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                    break;
                default:
            }

        } else if (tokens[0].equals("NoteStatus")) {
            bot.bufferClear();
            bot.bufferAppend(tokens[1]);
            bot.setBotState(BotState.CREATE_NOTE);
            bot.sendMessage(callbackQuery.getMessage().getChatId(), "Enter your note please:");
        } else if (tokens[0].equals("ShowNoteWithStatus")) {
            bot.showUsersNote(callbackQuery.getMessage().getChatId(), tokens[1]);
        } else {
            throw new NotYetImplementedException("We are still working on implementing this feature");
        }
    }
}
