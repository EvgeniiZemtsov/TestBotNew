package com.zemtsov.TestBot.models;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Created by")
    private Long userId;

    @Column(nullable = false)
    private String description;

    @Column(name = "Created")
    private Timestamp date;


}
