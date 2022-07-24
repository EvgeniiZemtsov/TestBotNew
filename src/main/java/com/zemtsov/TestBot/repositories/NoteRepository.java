package com.zemtsov.TestBot.repositories;

import com.zemtsov.TestBot.models.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
}
