package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.models.Note;

import java.util.List;

public interface NoteService {

    List<Note> getAllNotes();
    Note saveNote(Note note);
    void updateNote(Long id, String description);
    void deleteNoteById(Long id);
}
