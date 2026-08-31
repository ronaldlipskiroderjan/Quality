package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.DocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<DocumentoEntity, UUID> {
}
