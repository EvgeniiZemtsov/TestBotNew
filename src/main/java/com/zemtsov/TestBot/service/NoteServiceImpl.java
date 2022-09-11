package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.models.Note;
import com.zemtsov.TestBot.repositories.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {

    private NoteRepository repository;

    public NoteServiceImpl(NoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Note> getAllNotes() {
        return repository.findAll();
    }

    @Override
    public Note saveNote(Note note) {
        return repository.save(note);
    }

    @Override
    public void updateNote(Long id, String description) {

    }

    @Override
    public void deleteNoteById(Long id) {

    }
}
