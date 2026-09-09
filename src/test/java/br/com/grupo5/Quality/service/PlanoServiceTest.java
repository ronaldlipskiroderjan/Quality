package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanoServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private PlanoRepository planoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void deveCriarPlanoComUsuarioProprietario() {
        UsuarioEntity usuario = usuario();
        when(auth.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(
                Optional.of(usuario)
        );
        when(planoRepository.save(any(PlanoEntity.class)))
                .thenAnswer(invocation -> {
                    PlanoEntity plano = invocation.getArgument(0);
                    plano.setId(UUID.randomUUID());
                    return plano;
                });
        when(participacaoRepository.save(any(ParticipacaoPlanoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlanoDetalhadoResponseDTO response = service().criar(auth, planoDto());

        ArgumentCaptor<ParticipacaoPlanoEntity> captor =
                ArgumentCaptor.forClass(ParticipacaoPlanoEntity.class);
        verify(participacaoRepository).save(captor.capture());
        ParticipacaoPlanoEntity participacao = captor.getValue();

        assertEquals("Quality", participacao.getPlano().getNomeProjeto());
        assertEquals(Status.PENDENTE, participacao.getPlano().getStatus());
        assertNotNull(participacao.getPlano().getCriadoEm());
        assertTrue(participacao.possuiPapel(PapelPlano.PROPRIETARIO));
        assertTrue(participacao.possuiPermissao(PermissaoPlano.EXCLUIR));
        assertTrue(usuario.getParticipacoes().contains(participacao));
        assertEquals(participacao.getPlano().getId(), response.id());
        assertTrue(response.meusPapeis().contains(PapelPlano.PROPRIETARIO));
    }

    @Test
    void naoDeveCriarPlanoSemUsuarioAutenticado() {
        when(auth.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(
                Optional.empty()
        );

        assertThrows(NotFoundException.class, () -> service().criar(auth, planoDto()));
        verify(planoRepository, never()).save(any());
    }

    @Test
    void deveListarSomentePlanosComParticipacaoDoUsuario() {
        ParticipacaoPlanoEntity primeiro = participacao(
                plano("Projeto A", Status.PENDENTE),
                PapelPlano.PROPRIETARIO
        );
        ParticipacaoPlanoEntity segundo = participacao(
                plano("Projeto B", Status.CONCLUIDO),
                PapelPlano.AUDITOR
        );
        when(auth.getName()).thenReturn(EMAIL);
        when(participacaoRepository
                .findAllByUsuarioEmailIgnoreCaseOrderByPlanoCriadoEmDesc(EMAIL))
                .thenReturn(List.of(primeiro, segundo));

        List<PlanoResponseDTO> response = service().listar(auth);

        assertEquals(2, response.size());
        assertEquals(primeiro.getPlano().getId(), response.getFirst().id());
        assertTrue(response.get(1).meusPapeis().contains(PapelPlano.AUDITOR));
    }

    @Test
    void deveBuscarPlanoComPapeisEPermissoes() {
        ParticipacaoPlanoEntity participacao = participacao(
                plano("Quality", Status.PENDENTE),
                PapelPlano.RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                participacao.getPlano().getId()
        )).thenReturn(participacao);

        PlanoDetalhadoResponseDTO response = service().buscar(
                auth,
                participacao.getPlano().getId()
        );

        assertEquals(participacao.getPlano().getId(), response.id());
        assertTrue(response.meusPapeis().contains(
                PapelPlano.RESPONSAVEL_QUALIDADE
        ));
        assertTrue(response.minhasPermissoes().contains(PermissaoPlano.EDITAR));
    }

    @Test
    void deveAtualizarPlanoComPermissao() {
        PlanoEntity plano = plano("Antigo", Status.PENDENTE);
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                PapelPlano.RESPONSAVEL_QUALIDADE
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.EDITAR
        )).thenReturn(participacao);
        when(planoRepository.save(plano)).thenReturn(plano);

        PlanoDetalhadoResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                planoDto()
        );

        assertEquals("Quality", plano.getNomeProjeto());
        assertEquals("Garantir qualidade", plano.getObjetivo());
        assertEquals("Quality", response.nomeProjeto());
    }

    @Test
    void deveConcluirPlanoComPermissao() {
        PlanoEntity plano = plano("Quality", Status.PENDENTE);
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.CONCLUIR
        )).thenReturn(plano);

        service().concluir(auth, plano.getId());

        assertEquals(Status.CONCLUIDO, plano.getStatus());
        verify(planoRepository).save(plano);
    }

    @Test
    void deveExcluirPlanoComPermissao() {
        PlanoEntity plano = plano("Quality", Status.PENDENTE);
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.EXCLUIR
        )).thenReturn(plano);

        service().excluir(auth, plano.getId());

        verify(planoRepository).delete(plano);
    }

    private PlanoService service() {
        return new PlanoService(
                planoRepository,
                usuarioRepository,
                participacaoRepository,
                acessoPlanoService
        );
    }

    private PlanoRequestDTO planoDto() {
        return new PlanoRequestDTO(
                " Quality ",
                " 1.0 ",
                " Garantir qualidade ",
                " Visão geral "
        );
    }

    private PlanoEntity plano(String nome, Status status) {
        return PlanoEntity.builder()
                .id(UUID.randomUUID())
                .nomeProjeto(nome)
                .versao("1.0")
                .objetivo("Objetivo")
                .visaoGeral("Visão geral")
                .status(status)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .build();
    }

    private ParticipacaoPlanoEntity participacao(
            PlanoEntity plano,
            PapelPlano papel
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(usuario())
                .papeis(EnumSet.of(papel))
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
