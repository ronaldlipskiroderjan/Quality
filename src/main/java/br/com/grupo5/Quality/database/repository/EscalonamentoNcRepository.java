package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface EscalonamentoNcRepository
        extends JpaRepository<EscalonamentoNcEntity, UUID> {

    List<EscalonamentoNcEntity> findAllByNaoConformidadeId(UUID naoConformidadeId);

    Optional<EscalonamentoNcEntity>
            findByNaoConformidadeIdAndChaveIdempotencia(
                    UUID naoConformidadeId,
                    String chaveIdempotencia
            );

    @Query("""
            select escalonamento
            from EscalonamentoNcEntity escalonamento
            join escalonamento.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where naoConformidade.id = :naoConformidadeId
              and artefato.documento.plano.id = :planoId
            """)
    Page<EscalonamentoNcEntity> listarPorNaoConformidade(
            @Param("planoId") UUID planoId,
            @Param("naoConformidadeId") UUID naoConformidadeId,
            Pageable pageable
    );

    @Query("""
            select escalonamento
            from EscalonamentoNcEntity escalonamento
            join escalonamento.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where escalonamento.id = :escalonamentoId
              and naoConformidade.id = :naoConformidadeId
              and artefato.documento.plano.id = :planoId
            """)
    Optional<EscalonamentoNcEntity> buscarPorId(
            @Param("planoId") UUID planoId,
            @Param("naoConformidadeId") UUID naoConformidadeId,
            @Param("escalonamentoId") UUID escalonamentoId
    );

    @Query("""
            select escalonamento
            from EscalonamentoNcEntity escalonamento
            join escalonamento.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where artefato.documento.plano.id = :planoId
              and escalonamento.responsavel.id = :responsavelId
            """)
    Page<EscalonamentoNcEntity> listarDoSuperior(
            @Param("planoId") UUID planoId,
            @Param("responsavelId") UUID responsavelId,
            Pageable pageable
    );

    @Query("""
            select escalonamento
            from EscalonamentoNcEntity escalonamento
            join escalonamento.naoConformidade naoConformidade
            join escalonamento.responsavel responsavel
            where lower(responsavel.usuario.email) = lower(:email)
              and responsavel.ativo = true
              and (
                  (escalonamento.nivel = :nivelN1
                    and naoConformidade.status in :statusN1)
                  or
                  (escalonamento.nivel = :nivelN2
                    and naoConformidade.status in :statusN2)
              )
            order by escalonamento.escalonadoEm desc
            """)
    Page<EscalonamentoNcEntity> listarAtribuidosAoSuperior(
            @Param("email") String email,
            @Param("nivelN1") NivelEscalonamento nivelN1,
            @Param("statusN1") Collection<StatusNaoConformidade> statusN1,
            @Param("nivelN2") NivelEscalonamento nivelN2,
            @Param("statusN2") Collection<StatusNaoConformidade> statusN2,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select escalonamento
            from EscalonamentoNcEntity escalonamento
            join escalonamento.naoConformidade naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where escalonamento.id = :escalonamentoId
              and artefato.documento.plano.id = :planoId
              and escalonamento.responsavel.id = :responsavelId
            """)
    Optional<EscalonamentoNcEntity> buscarParaRevisao(
            @Param("planoId") UUID planoId,
            @Param("responsavelId") UUID responsavelId,
            @Param("escalonamentoId") UUID escalonamentoId
    );

    boolean existsByResponsavelId(UUID participanteId);
}
