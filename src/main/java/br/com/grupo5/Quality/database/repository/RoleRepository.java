package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByNome(String name);
}
