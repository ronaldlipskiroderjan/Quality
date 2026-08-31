package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ItemChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemChecklistRepository extends JpaRepository<ItemChecklistEntity, UUID> {
}
