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
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    private final ParticipacaoPlanoRepository participacaoRepository;
    private final UsuarioRepository usuarioRepository;
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
        Set<PapelPlano> papeis = copiarPapeis(dto.papeis());
        validarPapeisGerenciaveis(papeis);

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

        ParticipacaoPlanoEntity participacao = ParticipacaoPlanoEntity.builder()
                .plano(plano)
                .usuario(usuario)
                .papeis(papeis)
                .criadoEm(LocalDateTime.now())
                .build();

        participacao = participacaoRepository.save(participacao);
        plano.getParticipacoes().add(participacao);
        usuario.getParticipacoes().add(participacao);
        return toResponse(participacao);
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponseDTO> listar(
            Authentication auth,
            UUID planoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return participacaoRepository.findAllByPlanoIdOrderByCriadoEmAsc(planoId)
                .stream()
                .map(this::toResponse)
                .toList();
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
        validarAlteracao(participacao);

        Set<PapelPlano> papeis = copiarPapeis(dto.papeis());
        validarPapeisGerenciaveis(papeis);
        participacao.setPapeis(papeis);

        return toResponse(participacaoRepository.save(participacao));
    }

    @Transactional
    public void remover(
            Authentication auth,
            UUID planoId,
            UUID participanteId
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_PARTICIPANTES
        );
        ParticipacaoPlanoEntity participacao = buscar(planoId, participanteId);
        validarAlteracao(participacao);
        participacaoRepository.delete(participacao);
    }

    private ParticipacaoPlanoEntity buscar(UUID planoId, UUID participanteId) {
        return participacaoRepository.findByIdAndPlanoId(participanteId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Participante não encontrado."
                ));
    }

    private Set<PapelPlano> copiarPapeis(Set<PapelPlano> papeis) {
        if (papeis == null
                || papeis.isEmpty()
                || papeis.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRequestException("Informe ao menos um papel válido.");
        }
        return EnumSet.copyOf(papeis);
    }

    private void validarPapeisGerenciaveis(Set<PapelPlano> papeis) {
        if (papeis.contains(PapelPlano.PROPRIETARIO)) {
            throw new InvalidRequestException(
                    "A propriedade do plano não pode ser atribuída por esta operação."
            );
        }
    }

    private void validarAlteracao(ParticipacaoPlanoEntity participacao) {
        if (participacao.possuiPapel(PapelPlano.PROPRIETARIO)) {
            throw new InvalidRequestException(
                    "O proprietário do plano não pode ser alterado ou removido."
            );
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
                Set.copyOf(participacao.getPapeis()),
                Set.copyOf(participacao.getPermissoes()),
                participacao.getCriadoEm()
        );
    }
}
