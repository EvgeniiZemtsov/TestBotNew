package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.config.BotConfig;
import com.zemtsov.TestBot.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserService userService;

    BotState botState = BotState.DEFAULT;

    final BotConfig config;
    private final String HELP_MESSAGE = "This is my firs Telegram bot, created to learn how to work with Telegram API\n\n" +
            "You can execute the following commands by choosing them from menu or by typing them: \n\n" +
            "/start for getting a welcome message\n\n" +
            "/mydata for checking you personal data\n\n" +
            "/deletedata for deleting your data\n\n" +
            "/createnote for creating a note\n\n" +
            "/setemail for setting your email" +
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
                    registerUser(update);
                    startCommandAction(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/mydata":
                    returnUserHisData(chatId);
                    botState = BotState.DEFAULT;
                    log.info("Executed command /mydata");
                    break;
                case "/setemail":
                    setEmailCommandAction(chatId);
                    break;
                case "/help":
                    sendMessage(chatId, HELP_MESSAGE);
                    botState = BotState.DEFAULT;
                    log.info("Executed command /help");
                    break;
                default:
                    handleTextMessage(messageText, chatId);
            }

        }

    }

    private void returnUserHisData(long chatId) {
        Optional<User> user = userService.findUserById(chatId);
        String userInformation = "Your user name is " + user.get().getUserName() + "\n" +
                "Your first name is " + user.get().getFirstName() + "\n" +
                "Your last name is " + user.get().getLastName() + "\n" +
                "You first messaged the bot " + user.get().getRegisteredAt();
        sendMessage(chatId, userInformation);
    }

    private void startCommandAction(long chatId, String name) {

        String answer = "Hello, " + name + "!!!\n" +
                "How are you doing?\n" +
                "I'm glad to see you here!!!";

        sendMessage(chatId, answer);
        log.info("Replied to user : + " + name + " , command /start");
        botState = BotState.DEFAULT;
        log.info("Executed command /start");
    }

    private void setEmailCommandAction(long chatId) {
        botState = BotState.SET_EMAIL;
        sendMessage(chatId, "Enter your email: ");
        log.info("Executed command /setemail");
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
        listOfCommands.add(new BotCommand("/createnote", "creating a new note"));
        listOfCommands.add(new BotCommand("/setemail", "setting user's email"));
        listOfCommands.add(new BotCommand("/help", "get help message"));

        return listOfCommands;
    }

    private void registerUser(Update update) {

        if (userService.findUserById(update.getMessage().getChatId()).isEmpty()) {
            Long chatId = update.getMessage().getChatId();
            Chat chat = update.getMessage().getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userService.saveUser(user);

            log.info("New user has been saved: " + user);
        }
    }

    private void handleTextMessage(String text, long chatId) {
        if (botState.equals(BotState.SET_EMAIL)) {
            userService.updateUser(chatId, text);
        } else {
            sendMessage(chatId, "Sorry, I don't understand what you're saying\nTry to send /start");
        }
    }

    enum BotState {
        DEFAULT,
        CREATE_NOTE,
        SET_EMAIL
    }
}
