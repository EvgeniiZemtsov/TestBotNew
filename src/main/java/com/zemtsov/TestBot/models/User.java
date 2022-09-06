package com.zemtsov.TestBot.models;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity(name = "users")
@Data
public class User {

    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private String gender;
    private Timestamp registeredAt;
    private String email;


}

