package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.ArtefatoRepository;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.NovoParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PapeisParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.ParticipanteResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipanteServiceTest {

    private static final String EMAIL = "participante@quality.com";

    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ArtefatoRepository artefatoRepository;
    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void deveAdicionarUsuarioComPapeisContextuais() {
        PlanoEntity plano = plano();
        UsuarioEntity usuario = usuario();
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(
                Optional.of(usuario)
        );
        when(participacaoRepository.existsByPlanoIdAndUsuarioId(
                plano.getId(),
                usuario.getId()
        )).thenReturn(false);
        when(participacaoRepository.save(any(ParticipacaoPlanoEntity.class)))
                .thenAnswer(invocation -> {
                    ParticipacaoPlanoEntity participacao = invocation.getArgument(0);
                    participacao.setId(UUID.randomUUID());
                    return participacao;
                });

        ParticipanteResponseDTO response = service().adicionar(
                auth,
                plano.getId(),
                new NovoParticipanteRequestDTO(
                        EMAIL,
                        Set.of(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)
                )
        );

        assertEquals(usuario.getId(), response.usuarioId());
        assertTrue(response.papeis().contains(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE));
        assertTrue(response.permissoes().contains(PermissaoPlano.EDITAR));
        assertTrue(plano.getParticipacoes().stream()
                .anyMatch(item -> item.getUsuario().equals(usuario)));
    }

    @Test
    void naoDeveAdicionarUsuarioDuplicado() {
        PlanoEntity plano = plano();
        UsuarioEntity usuario = usuario();
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(
                Optional.of(usuario)
        );
        when(participacaoRepository.existsByPlanoIdAndUsuarioId(
                plano.getId(),
                usuario.getId()
        )).thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> service().adicionar(
                        auth,
                        plano.getId(),
                        novo(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)
                )
        );
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void naoDeveAdicionarParticipanteSemPapel() {
        PlanoEntity plano = plano();
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);

        assertThrows(
                InvalidRequestException.class,
                () -> service().adicionar(
                        auth,
                        plano.getId(),
                        new NovoParticipanteRequestDTO(
                                EMAIL,
                                (PapelPlano) null
                        )
                )
        );
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void deveListarParticipantesParaMembroDoPlano() {
        PlanoEntity plano = plano();
        UsuarioEntity usuario = usuario();
        usuario.setTipoImagem("image/png");
        usuario.setFotoPerfil(new byte[]{1, 2, 3});
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario,
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId())).thenReturn(plano);
        PageRequest pageable = PageRequest.of(0, 15);
        when(participacaoRepository.findAllByPlanoId(
                plano.getId(),
                pageable
        )).thenReturn(new PageImpl<>(List.of(participacao), pageable, 1));

        PaginaResponseDTO<ParticipanteResponseDTO> response = service().listar(
                auth,
                plano.getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertTrue(response.conteudo().getFirst().temImagem());
    }

    @Test
    void deveRetornarImagemDoParticipante() {
        PlanoEntity plano = plano();
        UsuarioEntity usuario = usuario();
        usuario.setTipoImagem("image/png");
        usuario.setFotoPerfil(new byte[]{1, 2, 3});
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario,
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId()))
                .thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                participacao.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participacao));

        ImagemResponseDTO response = service().buscarImagem(
                auth,
                plano.getId(),
                participacao.getId()
        );

        assertEquals("image/png", response.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, response.imagem());
    }

    @Test
    void deveInformarQuandoParticipanteNaoPossuiImagem() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId()))
                .thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                participacao.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participacao));

        assertThrows(
                NotFoundException.class,
                () -> service().buscarImagem(
                        auth,
                        plano.getId(),
                        participacao.getId()
                )
        );
    }

    @Test
    void deveAtualizarPapeisDeParticipante() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                participacao.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participacao));
        when(participacaoRepository.save(participacao)).thenReturn(participacao);

        ParticipanteResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                participacao.getId(),
                new PapeisParticipanteRequestDTO(
                        Set.of(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)
                )
        );

        assertEquals(
                Set.of(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE),
                response.papeis()
        );
        assertTrue(response.permissoes().contains(
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        ));
    }

    @Test
    void naoDeveAlterarPapelDoUltimoAuditorResponsavelDeQualidade() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity proprietario = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                proprietario.getId(),
                plano.getId()
        )).thenReturn(Optional.of(proprietario));
        when(participacaoRepository.buscarPorPapelNoPlano(
                plano.getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )).thenReturn(List.of(proprietario));

        assertThrows(
                InvalidRequestException.class,
                () -> service().atualizar(
                        auth,
                        plano.getId(),
                        proprietario.getId(),
                        new PapeisParticipanteRequestDTO(
                                Set.of(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO)
                        )
                )
        );
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void naoDeveRemoverPapelDeAuditorEmUso() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                auditor.getId(),
                plano.getId()
        )).thenReturn(Optional.of(auditor));
        when(participacaoRepository.buscarPorPapelNoPlano(
                plano.getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )).thenReturn(List.of(
                auditor,
                participacao(
                        plano,
                        usuario(),
                        PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
                )
        ));
        when(artefatoRepository.existsByAuditorId(auditor.getId()))
                .thenReturn(true);

        assertThrows(
                InvalidRequestException.class,
                () -> service().atualizar(
                        auth,
                        plano.getId(),
                        auditor.getId(),
                        new PapeisParticipanteRequestDTO(
                                Set.of(PapelPlano.SUPERIOR_N1)
                        )
                )
        );
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void deveRemoverParticipantePreservandoHistoricoDeAuditoria() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        ParticipacaoPlanoEntity novoAuditor = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(novoAuditor);
        when(participacaoRepository.findByIdAndPlanoId(
                auditor.getId(),
                plano.getId()
        )).thenReturn(Optional.of(auditor));
        when(participacaoRepository.buscarPorPapelNoPlano(
                plano.getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )).thenReturn(List.of(
                auditor,
                novoAuditor
        ));
        service().remover(auth, plano.getId(), auditor.getId());

        assertFalse(auditor.isAtivo());
        verify(participacaoRepository).save(auditor);
        verify(artefatoRepository).reatribuirEmExecucao(
                org.mockito.ArgumentMatchers.eq(auditor.getId()),
                org.mockito.ArgumentMatchers.eq(novoAuditor),
                any()
        );
        verify(auditoriaRepository).reatribuirEmExecucao(
                org.mockito.ArgumentMatchers.eq(auditor.getId()),
                org.mockito.ArgumentMatchers.eq(novoAuditor),
                any()
        );
        verify(participacaoRepository, never()).delete(any());
    }

    @Test
    void naoDeveRemoverPapelDeResponsavelComNcVinculada() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity responsavel = participacao(
                plano,
                usuario(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                responsavel.getId(),
                plano.getId()
        )).thenReturn(Optional.of(responsavel));
        when(naoConformidadeRepository.existsByResponsavelId(
                responsavel.getId()
        )).thenReturn(true);

        assertThrows(
                InvalidRequestException.class,
                () -> service().atualizar(
                        auth,
                        plano.getId(),
                        responsavel.getId(),
                        new PapeisParticipanteRequestDTO(
                                Set.of(PapelPlano.SUPERIOR_N1)
                        )
                )
        );

        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void deveRemoverResponsavelEDevolverNcAbertaParaFila() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity responsavel = participacao(
                plano,
                usuario(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        ));
        when(participacaoRepository.findByIdAndPlanoId(
                responsavel.getId(),
                plano.getId()
        )).thenReturn(Optional.of(responsavel));
        service().remover(auth, plano.getId(), responsavel.getId());

        assertFalse(responsavel.isAtivo());
        verify(participacaoRepository).save(responsavel);
        verify(naoConformidadeRepository).desatribuirEmAberto(
                org.mockito.ArgumentMatchers.eq(responsavel.getId()),
                any(),
                any()
        );
        verify(participacaoRepository, never()).delete(any());
    }


    @Test
    void deveDesativarParticipanteSemVinculos() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participante = participacao(
                plano,
                usuario(),
                PapelPlano.SUPERIOR_N1
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        ));
        when(participacaoRepository.findByIdAndPlanoId(
                participante.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participante));

        service().remover(auth, plano.getId(), participante.getId());

        assertFalse(participante.isAtivo());
        verify(participacaoRepository).save(participante);
        verify(participacaoRepository, never()).delete(any());
    }

    @Test
    void naoDeveAdicionarSegundoResponsavelN1() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity existente = participacao(
                plano,
                usuario(),
                PapelPlano.SUPERIOR_N1
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.buscarPorPapelNoPlano(
                plano.getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(existente));

        assertThrows(
                InvalidRequestException.class,
                () -> service().adicionar(
                        auth,
                        plano.getId(),
                        novo(PapelPlano.SUPERIOR_N1)
                )
        );

        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void devePermitirManterOAtualResponsavelN1() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity responsavel = participacao(
                plano,
                usuario(),
                PapelPlano.SUPERIOR_N1
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                responsavel.getId(),
                plano.getId()
        )).thenReturn(Optional.of(responsavel));
        when(participacaoRepository.buscarPorPapelNoPlano(
                plano.getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(responsavel));
        when(participacaoRepository.save(responsavel))
                .thenReturn(responsavel);

        ParticipanteResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                responsavel.getId(),
                new PapeisParticipanteRequestDTO(
                        Set.of(PapelPlano.SUPERIOR_N1)
                )
        );

        assertEquals(
                Set.of(PapelPlano.SUPERIOR_N1),
                response.papeis()
        );
    }

    @Test
    void deveRemoverResponsavelPreservandoHistoricoDeEscalonamento() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity responsavel = participacao(
                plano,
                usuario(),
                PapelPlano.SUPERIOR_N1
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        ));
        when(participacaoRepository.findByIdAndPlanoId(
                responsavel.getId(),
                plano.getId()
        )).thenReturn(Optional.of(responsavel));
        service().remover(auth, plano.getId(), responsavel.getId());

        assertFalse(responsavel.isAtivo());
        verify(participacaoRepository).save(responsavel);
        verify(participacaoRepository, never()).delete(any());
    }

    private ParticipanteService service() {
        return new ParticipanteService(
                participacaoRepository,
                usuarioRepository,
                artefatoRepository,
                auditoriaRepository,
                naoConformidadeRepository,
                acessoPlanoService
        );
    }

    private NovoParticipanteRequestDTO novo(PapelPlano papel) {
        return new NovoParticipanteRequestDTO(EMAIL, Set.of(papel));
    }

    private PlanoEntity plano() {
        return PlanoEntity.builder().id(UUID.randomUUID()).build();
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nome("Participante")
                .email(EMAIL)
                .build();
    }

    private ParticipacaoPlanoEntity participacao(
            PlanoEntity plano,
            UsuarioEntity usuario,
            PapelPlano papel
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(usuario)
                .papeis(EnumSet.of(papel))
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
