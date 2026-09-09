package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public PlanoDetalhadoResponseDTO criar(Authentication auth, PlanoRequestDTO dto) {
        UsuarioEntity usuario = buscarUsuario(auth);

        PlanoEntity plano = PlanoEntity.builder()
                .nomeProjeto(dto.nomeProjeto().trim())
                .versao(dto.versao().trim())
                .objetivo(dto.objetivo().trim())
                .visaoGeral(dto.visaoGeral().trim())
                .status(Status.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .build();

        plano = planoRepository.save(plano);
        usuario.getPlanos().add(plano);
        usuarioRepository.save(usuario);

        return toDetalhado(plano);
    }

    @Transactional(readOnly = true)
    public List<PlanoResponseDTO> listar(Authentication auth) {
        return planoRepository.findAllByUsuarioEmail(auth.getName()).stream()
                .map(this::toResumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanoDetalhadoResponseDTO buscar(Authentication auth, UUID id) {
        return toDetalhado(acessoPlanoService.buscar(auth, id));
    }

    @Transactional
    public PlanoDetalhadoResponseDTO atualizar(
            Authentication auth,
            UUID id,
            PlanoRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscar(auth, id);
        plano.setNomeProjeto(dto.nomeProjeto().trim());
        plano.setVersao(dto.versao().trim());
        plano.setObjetivo(dto.objetivo().trim());
        plano.setVisaoGeral(dto.visaoGeral().trim());

        return toDetalhado(planoRepository.save(plano));
    }

    @Transactional
    public void concluir(Authentication auth, UUID id) {
        PlanoEntity plano = acessoPlanoService.buscar(auth, id);
        plano.setStatus(Status.CONCLUIDO);
        planoRepository.save(plano);
    }

    @Transactional
    public void excluir(Authentication auth, UUID id) {
        PlanoEntity plano = acessoPlanoService.buscar(auth, id);
        List<UsuarioEntity> usuarios = usuarioRepository.findAllByPlanosId(id);

        usuarios.forEach(usuario -> usuario.getPlanos().remove(plano));
        usuarioRepository.saveAll(usuarios);
        planoRepository.delete(plano);
    }

    private UsuarioEntity buscarUsuario(Authentication auth) {
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private PlanoResponseDTO toResumo(PlanoEntity plano) {
        return new PlanoResponseDTO(
                plano.getId(),
                plano.getNomeProjeto(),
                plano.getVersao(),
                plano.getStatus(),
                plano.getCriadoEm()
        );
    }

    private PlanoDetalhadoResponseDTO toDetalhado(PlanoEntity plano) {
        return new PlanoDetalhadoResponseDTO(
                plano.getId(),
                plano.getNomeProjeto(),
                plano.getVersao(),
                plano.getObjetivo(),
                plano.getVisaoGeral(),
                plano.getStatus(),
                plano.getCriadoEm()
        );
    }
}
