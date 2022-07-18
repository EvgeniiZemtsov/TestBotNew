package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    private final String HELP_MESSAGE = "This is my firs Telegram bot, created to learn how to work with Telegram API\n\n" +
            "You can execute the following commands by choosing them from menu or by typing them: \n\n" +
            "/start for getting a welcome message\n\n" +
            "/mydata for checking you personal data\n\n" +
            "/deletedata for deleting your data\n\n" +
            "/help for checking the help desk\n\n" ;

    public TelegramBot(BotConfig config) {
        this.config = config;
        try {
            this.execute(new SetMyCommands(addBotCommands(), new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error occurred while adding menu: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            log.info("User message : " + messageText);

            switch (messageText) {
                case "/start":
                    startCommandAction(chatId, update.getMessage().getChat().getFirstName());
                    log.info("Executed command /start");
                    break;
                case "/help":
                    sendMessage(chatId, HELP_MESSAGE);
                    log.info("Executed command /help");
                    break;
                default:
                    sendMessage(chatId, "Sorry, I don't understand what you're saying\nTry to send /start");
            }

        }

    }

    private void startCommandAction(long chatId, String name) {

        String answer = "Hello, " + name + "!!!\n" +
                "How are you doing?\n" +
                "I'm glad to see you here!!!";

        sendMessage(chatId, answer);
        log.info("Replied to user : + " + name + " , command /start");
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("The error occurred while sending message: " + e.getMessage());
        }
    }

    private List<BotCommand> addBotCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();

        listOfCommands.add(new BotCommand("/start", "get welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get my data"));
        listOfCommands.add(new BotCommand("/deletedata", "deleting user data"));
        listOfCommands.add(new BotCommand("/help", "get help message"));

        return listOfCommands;
    }
}
