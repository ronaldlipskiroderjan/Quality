package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.PlanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PlanoRepository extends JpaRepository<PlanoEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from planos where id = :planoId", nativeQuery = true)
    void excluirComDependencias(@Param("planoId") UUID planoId);
}
