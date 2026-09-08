package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UsuarioEntity> findByEmailIgnoreCase(String email);
    @Query("SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.planos WHERE u.id = :id")
    Optional<UsuarioEntity> findByIdWithPlanos(UUID id);
}
