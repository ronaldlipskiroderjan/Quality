package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);
}
