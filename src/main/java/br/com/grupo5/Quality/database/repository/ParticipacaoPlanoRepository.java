package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipacaoPlanoRepository
        extends JpaRepository<ParticipacaoPlanoEntity, UUID> {

    @Query("""
            select participacao
            from ParticipacaoPlanoEntity participacao
            where lower(participacao.usuario.email) = lower(:email)
              and participacao.ativo = true
              and (select count(papel)
                   from participacao.papeis papel
                   where papel in :papeis) > 0
            """)
    Page<ParticipacaoPlanoEntity> listarPlanosVisiveis(
            @Param("email") String email,
            @Param("papeis") Collection<PapelPlano> papeis,
            Pageable pageable
    );

    @Query("""
            select participacao
            from ParticipacaoPlanoEntity participacao
            where participacao.plano.id = :planoId
              and participacao.ativo = true
            """)
    Page<ParticipacaoPlanoEntity> findAllByPlanoId(
            @Param("planoId")
            UUID planoId,
            Pageable pageable
    );

    @Query("""
            select participacao
            from ParticipacaoPlanoEntity participacao
            where participacao.plano.id = :planoId
              and lower(participacao.usuario.email) = lower(:email)
              and participacao.ativo = true
            """)
    Optional<ParticipacaoPlanoEntity> findByPlanoIdAndUsuarioEmailIgnoreCase(
            @Param("planoId")
            UUID planoId,
            @Param("email")
            String email
    );

    @Query("""
            select participacao
            from ParticipacaoPlanoEntity participacao
            where participacao.id = :participanteId
              and participacao.plano.id = :planoId
              and participacao.ativo = true
            """)
    Optional<ParticipacaoPlanoEntity> findByIdAndPlanoId(
            @Param("participanteId")
            UUID participanteId,
            @Param("planoId")
            UUID planoId
    );

    @Query("""
            select participacao
            from ParticipacaoPlanoEntity participacao
            where participacao.plano.id = :planoId
              and participacao.usuario.id = :usuarioId
            """)
    Optional<ParticipacaoPlanoEntity> buscarQualquerParticipacao(
            @Param("planoId") UUID planoId,
            @Param("usuarioId") UUID usuarioId
    );

    @Query("""
            select distinct participacao
            from ParticipacaoPlanoEntity participacao
            join participacao.papeis papel
            where participacao.plano.id = :planoId
              and participacao.ativo = true
              and papel = :papel
            """)
    List<ParticipacaoPlanoEntity> buscarPorPapelNoPlano(
            @Param("planoId") UUID planoId,
            @Param("papel") PapelPlano papel
    );

    @Query("""
            select (count(participacao) > 0)
            from ParticipacaoPlanoEntity participacao
            where participacao.plano.id = :planoId
              and participacao.usuario.id = :usuarioId
              and participacao.ativo = true
            """)
    boolean existsByPlanoIdAndUsuarioId(
            @Param("planoId") UUID planoId,
            @Param("usuarioId") UUID usuarioId
    );
}
