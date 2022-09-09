package com.zemtsov.TestBot.models;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity(name = "notes")
@Data
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String description;

    @Column(name = "Created")
    private Timestamp date;


}
