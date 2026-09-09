package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.NovoParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PapeisParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.response.ParticipanteResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        Set.of(PapelPlano.RESPONSAVEL_QUALIDADE)
                )
        );

        assertEquals(usuario.getId(), response.usuarioId());
        assertTrue(response.papeis().contains(PapelPlano.RESPONSAVEL_QUALIDADE));
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
                        novo(PapelPlano.AUDITOR)
                )
        );
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void naoDeveAtribuirPropriedadeDiretamente() {
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
                        novo(PapelPlano.PROPRIETARIO)
                )
        );
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void deveListarParticipantesParaMembroDoPlano() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId())).thenReturn(plano);
        when(participacaoRepository.findAllByPlanoIdOrderByCriadoEmAsc(
                plano.getId()
        )).thenReturn(List.of(participacao));

        List<ParticipanteResponseDTO> response = service().listar(
                auth,
                plano.getId()
        );

        assertEquals(1, response.size());
        assertEquals(participacao.getId(), response.getFirst().id());
    }

    @Test
    void deveAtualizarPapeisDeParticipante() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participacao = participacao(
                plano,
                usuario(),
                PapelPlano.AUDITOR
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
                        Set.of(PapelPlano.RESPONSAVEL_QUALIDADE)
                )
        );

        assertEquals(
                Set.of(PapelPlano.RESPONSAVEL_QUALIDADE),
                response.papeis()
        );
        assertTrue(response.permissoes().contains(
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        ));
    }

    @Test
    void naoDeveAlterarProprietario() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity proprietario = participacao(
                plano,
                usuario(),
                PapelPlano.PROPRIETARIO
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

        assertThrows(
                InvalidRequestException.class,
                () -> service().atualizar(
                        auth,
                        plano.getId(),
                        proprietario.getId(),
                        new PapeisParticipanteRequestDTO(Set.of(PapelPlano.AUDITOR))
                )
        );
        verify(participacaoRepository, never()).save(any());
    }

    @Test
    void deveRemoverParticipanteQueNaoSejaProprietario() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity participante = participacao(
                plano,
                usuario(),
                PapelPlano.PARTICIPANTE
        );
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        )).thenReturn(plano);
        when(participacaoRepository.findByIdAndPlanoId(
                participante.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participante));

        service().remover(auth, plano.getId(), participante.getId());

        verify(participacaoRepository).delete(participante);
    }

    private ParticipanteService service() {
        return new ParticipanteService(
                participacaoRepository,
                usuarioRepository,
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
