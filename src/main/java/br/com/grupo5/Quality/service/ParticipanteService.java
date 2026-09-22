package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.ArtefatoRepository;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.NovoParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PapeisParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.SuperioresPlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.ParticipanteResponseDTO;
import br.com.grupo5.Quality.dto.response.SuperioresPlanoResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    private static final Set<PapelPlano> PAPEIS_ESCALONAMENTO = Set.of(
            PapelPlano.SUPERIOR_N1,
            PapelPlano.SUPERIOR_N2
    );
    private static final Set<StatusArtefato> STATUS_FINAIS_ARTEFATO = Set.of(
            StatusArtefato.CONCLUIDO,
            StatusArtefato.CANCELADO
    );
    private static final Set<StatusAuditoria> STATUS_FINAIS_AUDITORIA = Set.of(
            StatusAuditoria.CONCLUIDA,
            StatusAuditoria.CANCELADA
    );
    private static final Set<StatusNaoConformidade> STATUS_FINAIS_NC = Set.of(
            StatusNaoConformidade.CONCLUIDA,
            StatusNaoConformidade.CANCELADA
    );

    private final ParticipacaoPlanoRepository participacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArtefatoRepository artefatoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public ParticipanteResponseDTO adicionar(
            Authentication auth,
            UUID planoId,
            NovoParticipanteRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        );
        PapelPlano papel = validarPapel(dto.papel());
        validarUnicidadeEscalonadores(planoId, papel, null);

        UsuarioEntity usuario = usuarioRepository
                .findByEmailIgnoreCase(dto.email().trim())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        if (participacaoRepository.existsByPlanoIdAndUsuarioId(
                planoId,
                usuario.getId()
        )) {
            throw new AlreadyExistsException(
                    "O usuário já participa deste plano."
            );
        }
        return toResponse(criarOuReativar(plano, usuario, papel));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<ParticipanteResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return PaginaResponseDTO.de(participacaoRepository
                .findAllByPlanoId(planoId, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ImagemResponseDTO buscarImagem(
            Authentication auth,
            UUID planoId,
            UUID participanteId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        UsuarioEntity usuario = buscar(planoId, participanteId).getUsuario();
        if (usuario.getFotoPerfil() == null || usuario.getTipoImagem() == null) {
            throw new NotFoundException("Imagem de perfil não encontrada.");
        }
        return new ImagemResponseDTO(
                usuario.getTipoImagem(),
                usuario.getFotoPerfil()
        );
    }

    @Transactional
    public SuperioresPlanoResponseDTO definirSuperiores(
            Authentication auth,
            UUID planoId,
            SuperioresPlanoRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        );
        String emailN1 = normalizarEmail(dto.superiorN1Email());
        String emailN2 = normalizarEmail(dto.superiorN2Email());
        if (emailN1 != null && emailN1.equalsIgnoreCase(emailN2)) {
            throw new InvalidRequestException(
                    "Os superiores N1 e N2 devem ser usuários diferentes."
            );
        }

        ParticipacaoPlanoEntity superiorN1 = definirSuperior(
                plano,
                PapelPlano.SUPERIOR_N1,
                emailN1
        );
        ParticipacaoPlanoEntity superiorN2 = definirSuperior(
                plano,
                PapelPlano.SUPERIOR_N2,
                emailN2
        );

        return new SuperioresPlanoResponseDTO(
                superiorN1 == null ? null : toResponse(superiorN1),
                superiorN2 == null ? null : toResponse(superiorN2)
        );
    }

    @Transactional
    public ParticipanteResponseDTO atualizar(
            Authentication auth,
            UUID planoId,
            UUID participanteId,
            PapeisParticipanteRequestDTO dto
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        );
        ParticipacaoPlanoEntity participacao = buscar(planoId, participanteId);
        PapelPlano papel = validarPapel(dto.papel());
        validarAtualizacao(participacao, papel);
        validarUnicidadeEscalonadores(
                planoId,
                papel,
                participanteId
        );
        participacao.setPapel(papel);

        return toResponse(participacaoRepository.save(participacao));
    }

    @Transactional
    public void remover(
            Authentication auth,
            UUID planoId,
            UUID participanteId
    ) {
        ParticipacaoPlanoEntity solicitante = acessoPlanoService
                .buscarParticipacao(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        );
        ParticipacaoPlanoEntity participacao = buscar(planoId, participanteId);
        validarUltimoResponsavel(participacao, false);
        reatribuirExecucoes(participacao, solicitante);
        naoConformidadeRepository.desatribuirEmAberto(
                participacao.getId(),
                STATUS_FINAIS_NC,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        desativar(participacao);
    }

    private ParticipacaoPlanoEntity buscar(UUID planoId, UUID participanteId) {
        return participacaoRepository.findByIdAndPlanoId(participanteId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Participante não encontrado."
                ));
    }

    private PapelPlano validarPapel(PapelPlano papel) {
        if (papel == null) {
            throw new InvalidRequestException(
                    "Informe um papel disponível na plataforma."
            );
        }
        return papel;
    }

    private ParticipacaoPlanoEntity definirSuperior(
            PlanoEntity plano,
            PapelPlano papel,
            String email
    ) {
        ParticipacaoPlanoEntity atual = participacaoRepository
                .buscarPorPapelNoPlano(plano.getId(), papel)
                .stream()
                .findFirst()
                .orElse(null);
        if (atual != null
                && email != null
                && atual.getUsuario().getEmail().equalsIgnoreCase(email)) {
            return atual;
        }

        UsuarioEntity usuario = email == null
                ? null
                : usuarioRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new NotFoundException(
                                "Usuário do superior não encontrado."
                        ));
        if (usuario != null
                && participacaoRepository.existsByPlanoIdAndUsuarioId(
                        plano.getId(),
                        usuario.getId()
                )) {
            throw new InvalidRequestException(
                    "O usuário informado já possui outro papel neste plano."
            );
        }

        if (atual != null) desativar(atual);
        if (usuario == null) {
            return null;
        }
        return criarOuReativar(plano, usuario, papel);
    }

    private ParticipacaoPlanoEntity criarOuReativar(
            PlanoEntity plano,
            UsuarioEntity usuario,
            PapelPlano papel
    ) {
        ParticipacaoPlanoEntity participacao = participacaoRepository
                .buscarQualquerParticipacao(plano.getId(), usuario.getId())
                .orElse(null);
        if (participacao == null) {
            participacao = ParticipacaoPlanoEntity.builder()
                    .plano(plano)
                    .usuario(usuario)
                    .papeis(java.util.EnumSet.of(papel))
                    .criadoEm(LocalDateTime.now())
                    .build();
            plano.getParticipacoes().add(participacao);
            usuario.getParticipacoes().add(participacao);
        } else {
            participacao.setPapel(papel);
            participacao.setAtivo(true);
            participacao.setCriadoEm(LocalDateTime.now());
        }
        return participacaoRepository.save(participacao);
    }

    private void reatribuirExecucoes(
            ParticipacaoPlanoEntity removida,
            ParticipacaoPlanoEntity solicitante
    ) {
        if (!removida.possuiPapel(
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )) {
            return;
        }
        ParticipacaoPlanoEntity novoAuditor = solicitante.getId()
                .equals(removida.getId())
                ? participacaoRepository.buscarPorPapelNoPlano(
                        removida.getPlano().getId(),
                        PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
                ).stream()
                .filter(auditor -> !auditor.getId().equals(removida.getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Adicione outro auditor antes de remover o auditor atual."
                ))
                : solicitante;
        artefatoRepository.reatribuirEmExecucao(
                removida.getId(),
                novoAuditor,
                STATUS_FINAIS_ARTEFATO
        );
        auditoriaRepository.reatribuirEmExecucao(
                removida.getId(),
                novoAuditor,
                STATUS_FINAIS_AUDITORIA
        );
    }

    private void desativar(ParticipacaoPlanoEntity participacao) {
        participacao.setAtivo(false);
        participacaoRepository.save(participacao);
    }

    private String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
    }


    private void validarAtualizacao(
            ParticipacaoPlanoEntity participacao,
            PapelPlano papel
    ) {
        validarUltimoResponsavel(
                participacao,
                papel == PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );

        validarAuditorEmUso(
                participacao,
                papel == PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        validarResponsavelEmUso(
                participacao,
                papel == PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        );
    }
    private void validarUltimoResponsavel(
            ParticipacaoPlanoEntity participacao,
            boolean manterPapel
    ) {
        if (!participacao.possuiPapel(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)
                || manterPapel) {
            return;
        }
        if (participacaoRepository.buscarPorPapelNoPlano(
                participacao.getPlano().getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        ).size() <= 1) {
            throw new InvalidRequestException(
                    "O plano deve manter ao menos um auditor e responsável de qualidade."
            );
        }

    }
    private void validarAuditorEmUso(
            ParticipacaoPlanoEntity participacao,
            boolean manterPapelAuditor
    ) {
        if (participacao.possuiPapel(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)
                && !manterPapelAuditor
                && artefatoRepository.existsByAuditorId(participacao.getId())) {
            throw new InvalidRequestException(
                    "O participante é auditor de um ou mais artefatos."
            );
        }
    }

    private void validarResponsavelEmUso(
            ParticipacaoPlanoEntity participacao,
            boolean manterPapelResponsavel
    ) {
        if (!manterPapelResponsavel
                && naoConformidadeRepository.existsByResponsavelId(
                        participacao.getId()
                )) {
            throw new InvalidRequestException(
                    "O participante é responsável por uma ou mais não conformidades."
            );
        }
    }

    private void validarUnicidadeEscalonadores(
            UUID planoId,
            PapelPlano papelSelecionado,
            UUID participanteId
    ) {
        for (PapelPlano papel : PAPEIS_ESCALONAMENTO) {
            if (papelSelecionado != papel) {
                continue;
            }
            boolean existeOutro = participacaoRepository
                    .buscarPorPapelNoPlano(planoId, papel)
                    .stream()
                    .anyMatch(participacao -> participanteId == null
                            || !participacao.getId().equals(participanteId));
            if (existeOutro) {
                throw new InvalidRequestException(
                        "Já existe um participante com o papel "
                                + papel
                                + " neste plano."
                );
            }
        }
    }

    private ParticipanteResponseDTO toResponse(
            ParticipacaoPlanoEntity participacao
    ) {
        UsuarioEntity usuario = participacao.getUsuario();
        return new ParticipanteResponseDTO(
                participacao.getId(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFotoPerfil() != null,
                participacao.getPapel(),
                Set.copyOf(participacao.getPermissoes()),
                participacao.getCriadoEm()
        );
    }
}
