package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.PlanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanoRepository extends JpaRepository<PlanoEntity, UUID> {

    @Query("""
            SELECT plano
            FROM UsuarioEntity usuario
            JOIN usuario.planos plano
            WHERE LOWER(usuario.email) = LOWER(:email)
            ORDER BY plano.criadoEm DESC
            """)
    List<PlanoEntity> findAllByUsuarioEmail(@Param("email") String email);

    @Query("""
            SELECT plano
            FROM UsuarioEntity usuario
            JOIN usuario.planos plano
            WHERE LOWER(usuario.email) = LOWER(:email)
              AND plano.id = :planoId
            """)
    Optional<PlanoEntity> findByIdAndUsuarioEmail(
            @Param("planoId") UUID planoId,
            @Param("email") String email
    );
}
