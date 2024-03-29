package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.config.BotConfig;
import com.zemtsov.TestBot.handlers.CallbackHandler;
import com.zemtsov.TestBot.handlers.TextHandler;
import com.zemtsov.TestBot.models.Note;
import com.zemtsov.TestBot.models.User;
import com.zemtsov.TestBot.util.BotState;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserService userService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private TextHandler textHandler;
    @Autowired
    private CallbackHandler callbackHandler;

    private BotState botState = BotState.STOPPED;
    List<String> buffer = new ArrayList<>();

    final BotConfig config;
    private final String HELP_MESSAGE = "This is my firs Telegram bot, created to learn how to work with Telegram API\n\n" +
            "You can execute the following commands by choosing them from menu or by typing them: \n\n" +
            "/start for starting using the bot\n\n" +
            "/mydata for checking you personal data\n\n" +
            "/deletedata for deleting your data\n\n" +
            "/createnote for creating a note\n\n" +
            "/setgender for setting your gender\n\n" +
            "/setemail for setting your email\n\n" +
            "/help for checking the help desk\n\n";

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
        if (update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().getText().equals("/start")) {
            startCommandAction(update.getMessage().getChatId(), update.getMessage().getChat().getFirstName());
            registerUser(update);

        } else if (botState != BotState.STOPPED) {
            if (update.hasCallbackQuery()) {
                callbackHandler.handleCallback(update.getCallbackQuery(), this);
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                log.info("User message : " + messageText);

                switch (messageText) {
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
                    case "/mynotes":
                        showUsersNoteCommand(chatId);
                        break;
                    case "/help":
                        sendMessage(chatId, HELP_MESSAGE);
                        botState = BotState.DEFAULT;
                        log.info("Executed command /help");
                        break;
                    default:
                        textHandler.handleText(messageText, chatId, this);
                }
            }
        } else {
            sendMessage(update.getMessage().getChatId(),
                    "You have deleted your data and stopped the bot. Please press /start command to start using the bot again.");
        }

    }

    private void createNoteCommand(long chatId) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(Arrays.asList(
                InlineKeyboardButton
                        .builder()
                        .text("❗️Important")
                        .callbackData("NoteStatus:Important")
                        .build(),
                InlineKeyboardButton
                        .builder()
                        .text("⚠️Normal")
                        .callbackData("NoteStatus:Normal")
                        .build(),
                InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDCDDTo-Do")
                        .callbackData("NoteStatus:To-Do")
                        .build()
        ));
        try {
            execute(
                    SendMessage.builder()
                            .text("What kind of note are you going to create?")
                            .chatId(String.valueOf(chatId))
                            .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                            .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

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
        botState = BotState.STOPPED;
    }

    private void showUsersNoteCommand(long chatId) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(Arrays.asList(
                InlineKeyboardButton
                        .builder()
                        .text("❗️Important")
                        .callbackData("ShowNoteWithStatus:Important")
                        .build(),
                InlineKeyboardButton
                        .builder()
                        .text("⚠️Normal")
                        .callbackData("ShowNoteWithStatus:Normal")
                        .build(),
                InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDCDDTo-Do")
                        .callbackData("ShowNoteWithStatus:To-Do")
                        .build()
        ));
        try {
            execute(
                    SendMessage.builder()
                            .text("What kind of your notes would you like to see?")
                            .chatId(String.valueOf(chatId))
                            .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                            .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void showUsersNote(long chatId, String status) {
        botState = BotState.DEFAULT;
        List<Note> allNotes = noteService.getAllNotes().isEmpty() ? Collections.emptyList() : noteService.getAllNotes();
        List<Note> usersNotes;
        if (!allNotes.isEmpty()) {
            usersNotes = allNotes.stream()
                    .filter(note -> note.getUser().getChatId().equals(chatId))
                    .filter(note -> note.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else {
            sendMessage(chatId, "There are no any notes in bot's db");
            return;
        }

        if (usersNotes.isEmpty()) {
            sendMessage(chatId, "You don't have any notes yet.\n\n" +
                    "Create new note using command /createnote");
        } else {
            for (Note note : usersNotes) {
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                buttons.add(Arrays.asList(
                        InlineKeyboardButton
                                .builder()
                                .text("Edit✏️")
                                .callbackData("NoteAction:Edit:" + note.getId())
                                .build(),
                        InlineKeyboardButton
                                .builder()
                                .text("Delete\uD83D\uDDD1")
                                .callbackData("NoteAction:Delete:" + note.getId())
                                .build()
                ));
                try {
                    execute(
                            SendMessage.builder()
                                    .text(note.getDescription())
                                    .chatId(String.valueOf(chatId))
                                    .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                                    .build()
                    );
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            }
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
        try {
            execute(
                    SendMessage.builder()
                            .text("Please choose your gender")
                            .chatId(String.valueOf(chatId))
                            .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                            .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
        botState = BotState.DEFAULT;
        log.info("Executed command /setgender");
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

    public void createNote(String text, long chatId) {
        Note note = new Note();
        Optional<User> user = userService.findUserById(chatId);
        note.setDescription(text);
        note.setStatus(buffer.get(0));
        buffer.clear();
        note.setDate(new Timestamp(System.currentTimeMillis()));
        user.ifPresent(note::setUser);
        noteService.saveNote(note);
        botState = BotState.DEFAULT;
        log.info("New note was added by user with id " + chatId);
    }

    public void editNote(String text, long chatId) {
        noteService.updateNote(Long.parseLong(buffer.get(0)), text);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setMessageId(Integer.parseInt(buffer.get(1)));
        editMessage.setText(text);
        editMessage.setChatId(String.valueOf(chatId));
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.error("Error while editing message. " + e.getMessage());
        }

        sendMessage(chatId, "You have successfully edited the note \uD83D\uDC4D");
        buffer.clear();
        botState = BotState.DEFAULT;
    }

    public void setUsersEmail(String text, long chatId) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (Pattern.matches(emailRegex, text)) {
            userService.updateUser(chatId, text, null);
            botState = BotState.DEFAULT;
        } else {
            sendMessage(chatId, "Looks like " + text + " is not a valid email. Please enter a valid email.");
        }
    }

    public void sendMessage(long chatId, String textToSend) {
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
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/createnote", "create a new note"));
        listOfCommands.add(new BotCommand("/setemail", "set my email"));
        listOfCommands.add(new BotCommand("/setgender", "set my gender"));
        listOfCommands.add(new BotCommand("/createnote", "create new note"));
        listOfCommands.add(new BotCommand("/mynotes", "show my notes"));
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

    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public UserService getUserService() {
        return userService;
    }

    public NoteService getNoteService() {
        return noteService;
    }

    public void bufferAppend(String token) {
        buffer.add(token);
    }

    public void bufferClear() {
        buffer.clear();
    }

    public List<String> getBuffer() {
        return buffer;
    }
}
