package com.zemtsov.TestBot.service;

import com.zemtsov.TestBot.models.Note;
import com.zemtsov.TestBot.repositories.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void updateNote(Long id, String description) {
        Note note = repository.findById(id).orElseThrow(() -> new IllegalStateException("Note with id = " + id + " doesn't exist"));

        if (description != null) {
            note.setDescription(description);
        }
    }

    @Override
    public void deleteNoteById(Long id) {
        Note note = repository.findById(id).orElseThrow(() -> new IllegalStateException("Note with id = " + id + " doesn't exist"));
        repository.delete(note);
    }
}
