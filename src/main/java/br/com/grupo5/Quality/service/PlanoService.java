package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.ConfiguracaoClassificacaoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.PlanoImagemEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.PlanoImagemRepository;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private static final long TAMANHO_MAXIMO_IMAGEM = 5 * 1024 * 1024;
    private static final Set<String> TIPOS_IMAGEM = Set.of(
            "image/jpeg",
            "image/png"
    );

    private static final Map<ClassificacaoNaoConformidade, Integer> PRAZOS_PADRAO = Map.of(
            ClassificacaoNaoConformidade.SIMPLES, 24,
            ClassificacaoNaoConformidade.COMPLEXA, 48,
            ClassificacaoNaoConformidade.SEVERA, 72,
            ClassificacaoNaoConformidade.EXTREMA, 96
    );

    private final PlanoRepository planoRepository;
    private final PlanoImagemRepository planoImagemRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public PlanoDetalhadoResponseDTO criar(Authentication auth, PlanoRequestDTO dto) {
        return criar(auth, dto, null);
    }

    @Transactional
    public PlanoDetalhadoResponseDTO criar(
            Authentication auth,
            PlanoRequestDTO dto,
            MultipartFile imagem
    ) {
        UsuarioEntity usuario = buscarUsuario(auth);

        PlanoEntity plano = PlanoEntity.builder()
                .nomeProjeto(dto.nomeProjeto().trim())
                .versao(dto.versao().trim())
                .objetivo(dto.objetivo().trim())
                .visaoGeral(dto.visaoGeral().trim())
                .status(Status.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .temImagem(false)
                .build();

        for (Map.Entry<ClassificacaoNaoConformidade, Integer> prazo
                : PRAZOS_PADRAO.entrySet()) {
                plano.getConfiguracoesClassificacao().add(
                        ConfiguracaoClassificacaoEntity.builder()
                                .plano(plano)
                                .classificacao(prazo.getKey())
                                .prazoHoras(prazo.getValue())
                                .ativa(true)
                                .build()
                );
        }

        plano = planoRepository.save(plano);
        if (imagem != null) {
            salvarImagem(plano, imagem);
        }
        ParticipacaoPlanoEntity participacao = criarAuditorResponsavel(
                plano,
                usuario
        );
        participacaoRepository.save(participacao);
        plano.getParticipacoes().add(participacao);
        usuario.getParticipacoes().add(participacao);

        return toDetalhado(participacao);
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<PlanoResponseDTO> listar(
            Authentication auth,
            Pageable pageable
    ) {
        return PaginaResponseDTO.de(participacaoRepository
                .listarPlanosVisiveis(
                        auth.getName(),
                        EnumSet.allOf(PapelPlano.class),
                        pageable
                )
                .map(this::toResumo));
    }

    @Transactional(readOnly = true)
    public PlanoDetalhadoResponseDTO buscar(Authentication auth, UUID id) {
        return toDetalhado(acessoPlanoService.buscarParticipacao(auth, id));
    }

    @Transactional
    public PlanoDetalhadoResponseDTO atualizar(
            Authentication auth,
            UUID id,
            PlanoRequestDTO dto
    ) {
        return atualizar(auth, id, dto, null, false);
    }

    @Transactional
    public PlanoDetalhadoResponseDTO atualizar(
            Authentication auth,
            UUID id,
            PlanoRequestDTO dto,
            MultipartFile imagem,
            boolean removerImagem
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService.buscarParticipacao(
                auth,
                id,
                PermissaoPlano.EDITAR
        );
        PlanoEntity plano = participacao.getPlano();
        plano.setNomeProjeto(dto.nomeProjeto().trim());
        plano.setVersao(dto.versao().trim());
        plano.setObjetivo(dto.objetivo().trim());
        plano.setVisaoGeral(dto.visaoGeral().trim());

        if (imagem != null && removerImagem) {
            throw new InvalidRequestException(
                    "Escolha entre substituir ou remover a imagem do plano."
            );
        }
        if (removerImagem) {
            removerImagem(plano);
        } else if (imagem != null) {
            salvarImagem(plano, imagem);
        }

        planoRepository.save(plano);
        return toDetalhado(participacao);
    }

    @Transactional
    public void salvarImagem(
            Authentication auth,
            UUID id,
            MultipartFile imagem
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                id,
                PermissaoPlano.EDITAR
        );
        salvarImagem(plano, imagem);
    }

    @Transactional(readOnly = true)
    public ImagemResponseDTO buscarImagem(Authentication auth, UUID id) {
        acessoPlanoService.buscarPlano(auth, id, PermissaoPlano.VISUALIZAR);
        PlanoImagemEntity imagem = planoImagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Imagem do plano não encontrada."
                ));
        return new ImagemResponseDTO(imagem.getTipoConteudo(), imagem.getConteudo());
    }

    @Transactional
    public void removerImagem(Authentication auth, UUID id) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                id,
                PermissaoPlano.EDITAR
        );
        removerImagem(plano);
        planoRepository.save(plano);
    }

    @Transactional
    public void concluir(Authentication auth, UUID id) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                id,
                PermissaoPlano.CONCLUIR
        );
        plano.setStatus(Status.CONCLUIDO);
        planoRepository.save(plano);
    }

    @Transactional
    public void excluir(Authentication auth, UUID id) {
        acessoPlanoService.buscarPlano(
                auth,
                id,
                PermissaoPlano.EXCLUIR
        );
        planoRepository.excluirComDependencias(id);
    }

    private UsuarioEntity buscarUsuario(Authentication auth) {
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private void salvarImagem(PlanoEntity plano, MultipartFile arquivo) {
        byte[] conteudo = lerImagem(arquivo);
        PlanoImagemEntity imagem = planoImagemRepository.findById(plano.getId())
                .orElseGet(() -> PlanoImagemEntity.builder()
                        .plano(plano)
                        .build());
        imagem.setTipoConteudo(arquivo.getContentType());
        imagem.setConteudo(conteudo);
        planoImagemRepository.save(imagem);
        plano.setTemImagem(true);
    }

    private void removerImagem(PlanoEntity plano) {
        planoImagemRepository.deleteById(plano.getId());
        plano.setTemImagem(false);
    }

    private byte[] lerImagem(MultipartFile imagem) {
        String tipo = imagem.getContentType();
        if (imagem.isEmpty() || tipo == null || !TIPOS_IMAGEM.contains(tipo)) {
            throw new InvalidRequestException("Envie uma imagem PNG ou JPG válida.");
        }
        if (imagem.getSize() > TAMANHO_MAXIMO_IMAGEM) {
            throw new InvalidRequestException("A imagem deve ter no máximo 5 MB.");
        }
        try {
            byte[] conteudo = imagem.getBytes();
            if (ImageIO.read(new ByteArrayInputStream(conteudo)) == null) {
                throw new InvalidRequestException("O arquivo enviado não é uma imagem válida.");
            }
            return conteudo;
        } catch (IOException ex) {
            throw new InvalidRequestException("Não foi possível ler a imagem do plano.");
        }
    }

    private ParticipacaoPlanoEntity criarAuditorResponsavel(
            PlanoEntity plano,
            UsuarioEntity usuario
    ) {
        return ParticipacaoPlanoEntity.builder()
                .plano(plano)
                .usuario(usuario)
                .papeis(EnumSet.of(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE))
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private PlanoResponseDTO toResumo(ParticipacaoPlanoEntity participacao) {
        PlanoEntity plano = participacao.getPlano();
        return new PlanoResponseDTO(
                plano.getId(),
                plano.getNomeProjeto(),
                plano.getVersao(),
                plano.getStatus(),
                plano.getCriadoEm(),
                plano.isTemImagem(),
                Set.copyOf(participacao.getPapeis()),
                Set.copyOf(participacao.getPermissoes())
        );
    }

    private PlanoDetalhadoResponseDTO toDetalhado(
            ParticipacaoPlanoEntity participacao
    ) {
        PlanoEntity plano = participacao.getPlano();
        return new PlanoDetalhadoResponseDTO(
                plano.getId(),
                plano.getNomeProjeto(),
                plano.getVersao(),
                plano.getObjetivo(),
                plano.getVisaoGeral(),
                plano.getStatus(),
                plano.getCriadoEm(),
                plano.isTemImagem(),
                Set.copyOf(participacao.getPapeis()),
                Set.copyOf(participacao.getPermissoes())
        );
    }
}
