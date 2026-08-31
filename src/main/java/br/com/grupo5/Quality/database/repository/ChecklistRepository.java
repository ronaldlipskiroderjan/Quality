package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChecklistRepository extends JpaRepository<ChecklistEntity, UUID> {
}
