package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.config.BotConfig;
import com.zemtsov.TestBot.models.Note;
import com.zemtsov.TestBot.models.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserService userService;
    @Autowired
    private NoteService noteService;

    BotState botState = BotState.DEFAULT;

    final BotConfig config;
    private final String HELP_MESSAGE = "This is my firs Telegram bot, created to learn how to work with Telegram API\n\n" +
            "You can execute the following commands by choosing them from menu or by typing them: \n\n" +
            "/start for starting using the bot\n\n" +
            "/mydata for checking you personal data\n\n" +
            "/deletedata for deleting your data\n\n" +
            "/createnote for creating a note\n\n" +
            "/setgender for setting your gender\n\n" +
            "/setemail for setting your email\n\n" +
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

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }

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
                    myDataCommandAction(chatId);
                    break;
                case "/deletedata":
                    deleteDataCommandAction(chatId);
                    break;
                case "/setemail":
                    setEmailCommandAction(chatId);
                    break;
                case "/setgender":
                    setGenderCommandAction(chatId);
                    break;
                case "/createnote":
                    createNoteCommand(chatId);
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

    private void createNoteCommand(long chatId) {
        botState = BotState.CREATE_NOTE;
        sendMessage(chatId, "Enter your note please:");
    }

    private void deleteDataCommandAction(long chatId) {
        userService.deleteUserById(chatId);
        log.warn("The user with id = " + chatId + " was deleted from database");
        try {
            execute(
                    SendMessage.builder()
                            .text("Your data was deleted from Bot's database")
                            .chatId(String.valueOf(chatId))
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void setGenderCommandAction(long chatId) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(Arrays.asList(
                InlineKeyboardButton
                        .builder()
                        .text("Male")
                        .callbackData("Gender:Male")
                        .build(),
                InlineKeyboardButton
                        .builder()
                        .text("Female")
                        .callbackData("Gender:Female")
                        .build()
        ));
        sendMessage(chatId, "Please choose your gender");
        botState = BotState.DEFAULT;
        log.info("Executed command /setgender");
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();

        String[] tokens = callbackQuery.getData().split(":");
        if (tokens[0].equals("Gender")) {
            switch (tokens[1]) {
                case "Male":
                    userService.updateUser(message.getChatId(), null, "male");
                    break;
                case "Female":
                    userService.updateUser(message.getChatId(), null,"female");
                    break;
                default:

            }
            try {
                execute(AnswerCallbackQuery.builder()
                        .cacheTime(10)
                        .text("You've successfully set your gender. Now it's " + tokens[1])
                        .showAlert(true)
                        .callbackQueryId(callbackQuery.getId())
                        .build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else {
            throw new NotYetImplementedException("We are still working on implementing this feature");
        }
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

    private void myDataCommandAction(long chatId) {
        returnUserHisData(chatId);
        botState = BotState.DEFAULT;
        log.info("Executed command /mydata");
    }

    private void setEmailCommandAction(long chatId) {
        botState = BotState.SET_EMAIL;
        sendMessage(chatId, "Enter your email: ");
        log.info("Executed command /setemail");
    }

    private void handleTextMessage(String text, long chatId) {
        switch (botState) {
            case SET_EMAIL:
                setUsersEmail(text, chatId);
                break;
            case CREATE_NOTE:
                createNote(text, chatId);
                break;
            default:
                sendMessage(chatId, "Sorry, I don't understand what you're saying\nTry to send /start");
                break;
        }
    }

    private void createNote(String text, long chatId) {
        Note note = new Note();
        Optional<User> user = userService.findUserById(chatId);
        note.setDescription(text);
        note.setDate(new Timestamp(System.currentTimeMillis()));
        user.ifPresent(note::setUser);
        noteService.saveNote(note);
        botState = BotState.DEFAULT;
        log.info("New note was added by user with id " + chatId);
    }

    private void setUsersEmail(String text, long chatId) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (Pattern.matches(emailRegex, text)) {
            userService.updateUser(chatId, text, null);
            botState = BotState.DEFAULT;
        } else {
            sendMessage(chatId, "Looks like " + text + " is not a valid email. Please enter a valid email.");
        }
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

        listOfCommands.add(new BotCommand("/start", "start using bot"));
        listOfCommands.add(new BotCommand("/mydata", "get my data"));
        listOfCommands.add(new BotCommand("/deletedata", "deleting user data"));
        listOfCommands.add(new BotCommand("/createnote", "creating a new note"));
        listOfCommands.add(new BotCommand("/setemail", "setting user's email"));
        listOfCommands.add(new BotCommand("/setgender", "setting user's gender"));
        listOfCommands.add(new BotCommand("/createnote", "creating a new note"));
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

    private void returnUserHisData(long chatId) {
        Optional<User> userOptional = userService.findUserById(chatId);
        User user = null;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            return;
        }

        StringBuilder userInf = new StringBuilder("Your first name is " + user.getFirstName() + "\n" +
                "Your last name is " + user.getLastName() + "\n" +
                "You first messaged the bot " + user.getRegisteredAt());

        if (user.getUserName() != null) {
            userInf.append("\n" +
                    "Your user name is " + user.getUserName());
        }

        if (user.getGender() != null) {
            userInf.append("\n" +
                    "Your gender is " + user.getGender());
        }

        if (user.getEmail() != null) {
            userInf.append("\n" +
                    "Your email is " + user.getEmail());
        }

        sendMessage(chatId, userInf.toString());
    }

    enum BotState {
        DEFAULT,
        CREATE_NOTE,
        SET_EMAIL
    }
}
