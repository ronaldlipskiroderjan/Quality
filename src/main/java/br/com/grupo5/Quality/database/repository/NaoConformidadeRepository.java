package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NaoConformidadeRepository
        extends JpaRepository<NaoConformidadeEntity, UUID> {

    Page<NaoConformidadeEntity>
            findAllByRespostaAuditoriaArtefatoDocumentoPlanoId(
                    UUID planoId,
                    Pageable pageable
            );

    Optional<NaoConformidadeEntity>
            findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                    UUID naoConformidadeId,
                    UUID planoId
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select naoConformidade
            from NaoConformidadeEntity naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            where naoConformidade.id = :naoConformidadeId
              and artefato.documento.plano.id = :planoId
            """)
    Optional<NaoConformidadeEntity> buscarParaAtualizacao(
            @Param("naoConformidadeId") UUID naoConformidadeId,
            @Param("planoId") UUID planoId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select naoConformidade
            from NaoConformidadeEntity naoConformidade
            where naoConformidade.id = :naoConformidadeId
            """)
    Optional<NaoConformidadeEntity> buscarPorIdParaAtualizacao(
            @Param("naoConformidadeId") UUID naoConformidadeId
    );

    @Query("""
            select naoConformidade.id
            from NaoConformidadeEntity naoConformidade
            where naoConformidade.prazoEm is not null
              and naoConformidade.prazoEm <= :agora
              and naoConformidade.status in :status
            order by naoConformidade.prazoEm asc
            """)
    List<UUID> listarIdsComPrazoVencido(
            @Param("agora") OffsetDateTime agora,
            @Param("status") Collection<StatusNaoConformidade> status,
            Pageable pageable
    );

    boolean existsByRespostaId(UUID respostaId);

    Optional<NaoConformidadeEntity> findByRespostaId(UUID respostaId);

    List<NaoConformidadeEntity> findAllByRespostaAuditoriaId(UUID auditoriaId);

    List<NaoConformidadeEntity>
            findAllByRespostaAuditoriaIdAndRespostaItemChecklistId(
                    UUID auditoriaId,
                    UUID checklistId
            );

    boolean existsByResponsavelId(UUID participanteId);

    @Modifying
    @Query("""
            update NaoConformidadeEntity naoConformidade
            set naoConformidade.responsavel = null,
                naoConformidade.atualizadoEm = :agora
            where naoConformidade.responsavel.id = :participanteId
              and naoConformidade.status not in :statusFinais
            """)
    int desatribuirEmAberto(
            @Param("participanteId") UUID participanteId,
            @Param("statusFinais") Collection<StatusNaoConformidade> statusFinais,
            @Param("agora") OffsetDateTime agora
    );

    @Query("""
            select naoConformidade
            from NaoConformidadeEntity naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            join ParticipacaoPlanoEntity participacao
              on participacao.plano.id = artefato.documento.plano.id
            join participacao.papeis papel
            where lower(participacao.usuario.email) = lower(:email)
              and participacao.ativo = true
              and papel = :papel
              and naoConformidade.status <> :statusExcluido
              and exists (
                  select encaminhamento.id
                  from EncaminhamentoNcEntity encaminhamento
                  where encaminhamento.naoConformidade = naoConformidade
              )
            order by naoConformidade.atualizadoEm desc
            """)
    Page<NaoConformidadeEntity> listarDaEquipe(
            @Param("email") String email,
            @Param("papel") br.com.grupo5.Quality.database.enums.PapelPlano papel,
            @Param("statusExcluido") StatusNaoConformidade statusExcluido,
            Pageable pageable
    );

    @Query("""
            select naoConformidade
            from NaoConformidadeEntity naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            join naoConformidade.responsavel responsavel
            where lower(responsavel.usuario.email) = lower(:email)
              and responsavel.ativo = true
              and naoConformidade.status not in :statusFinais
              and exists (
                  select encaminhamento.id
                  from EncaminhamentoNcEntity encaminhamento
                  where encaminhamento.naoConformidade = naoConformidade
              )
            order by naoConformidade.atualizadoEm desc
            """)
    Page<NaoConformidadeEntity> listarAtribuidasAoResponsavel(
            @Param("email") String email,
            @Param("statusFinais") Collection<StatusNaoConformidade> statusFinais,
            Pageable pageable
    );

    @Query("""
            select naoConformidade
            from NaoConformidadeEntity naoConformidade
            join naoConformidade.resposta resposta
            join resposta.auditoria auditoria
            join auditoria.artefato artefato
            join ParticipacaoPlanoEntity participacao
              on participacao.plano.id = artefato.documento.plano.id
            join participacao.papeis papel
            where artefato.documento.plano.id = :planoId
              and lower(participacao.usuario.email) = lower(:email)
              and participacao.ativo = true
              and papel = :papel
              and naoConformidade.status <> :statusExcluido
              and exists (
                  select encaminhamento.id
                  from EncaminhamentoNcEntity encaminhamento
                  where encaminhamento.naoConformidade = naoConformidade
              )
            order by naoConformidade.atualizadoEm desc
            """)
    Page<NaoConformidadeEntity> listarDaEquipeNoPlano(
            @Param("planoId") UUID planoId,
            @Param("email") String email,
            @Param("papel") br.com.grupo5.Quality.database.enums.PapelPlano papel,
            @Param("statusExcluido") StatusNaoConformidade statusExcluido,
            Pageable pageable
    );
}
