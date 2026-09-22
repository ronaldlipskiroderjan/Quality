package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ComunicacaoNcEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComunicacaoNcRepository
        extends JpaRepository<ComunicacaoNcEntity, UUID> {

    List<ComunicacaoNcEntity> findAllByNaoConformidadeId(UUID naoConformidadeId);

    Optional<ComunicacaoNcEntity>
            findByNaoConformidadeIdAndChaveIdempotencia(
                    UUID naoConformidadeId,
                    String chaveIdempotencia
            );

    @Query("""
            select comunicacao
            from ComunicacaoNcEntity comunicacao
            join comunicacao.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where comunicacao.naoConformidade.id = :naoConformidadeId
              and artefato.documento.plano.id = :planoId
            """)
    Page<ComunicacaoNcEntity> listarPorNaoConformidade(
            @Param("planoId") UUID planoId,
            @Param("naoConformidadeId") UUID naoConformidadeId,
            Pageable pageable
    );

    @Query("""
            select comunicacao
            from ComunicacaoNcEntity comunicacao
            join comunicacao.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where comunicacao.id = :comunicacaoId
              and naoConformidade.id = :naoConformidadeId
              and artefato.documento.plano.id = :planoId
            """)
    Optional<ComunicacaoNcEntity> buscarPorId(
            @Param("planoId") UUID planoId,
            @Param("naoConformidadeId") UUID naoConformidadeId,
            @Param("comunicacaoId") UUID comunicacaoId
    );
}
