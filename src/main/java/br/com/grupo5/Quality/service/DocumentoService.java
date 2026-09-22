package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.Classificacao;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.DocumentoRepository;
import br.com.grupo5.Quality.dto.request.AtualizarDocumentoRequestDTO;
import br.com.grupo5.Quality.dto.request.DocumentoRequestDTO;
import br.com.grupo5.Quality.dto.response.DocumentoArquivoResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final String TIPO_PADRAO = "application/octet-stream";

    private final DocumentoRepository documentoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public DocumentoDetalhadoResponseDTO adicionar(
            Authentication auth,
            UUID planoId,
            DocumentoRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        );
        MultipartFile arquivo = dto.arquivo();
        validarArquivo(arquivo);

        DocumentoEntity documento = DocumentoEntity.builder()
                .plano(plano)
                .nome(dto.nome().trim())
                .versao(dto.versao().trim())
                .conteudo(lerConteudo(arquivo))
                .nomeArquivo(limparNome(arquivo))
                .tipoArquivo(Optional.ofNullable(arquivo.getContentType()).orElse(TIPO_PADRAO))
                .tamanho(arquivo.getSize())
                .classificacao(dto.classificacao())
                .build();

        return toDetalhado(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<DocumentoResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            Classificacao classificacao,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);

        Page<DocumentoEntity> documentos = classificacao == null
                ? documentoRepository.findAllByPlanoId(planoId, pageable)
                : documentoRepository.findAllByPlanoIdAndClassificacao(
                        planoId,
                        classificacao,
                        pageable
                );

        return PaginaResponseDTO.de(documentos.map(this::toResumo));
    }

    @Transactional(readOnly = true)
    public DocumentoDetalhadoResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID documentoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return toDetalhado(buscarDocumento(planoId, documentoId));
    }

    @Transactional
    public DocumentoDetalhadoResponseDTO atualizar(
            Authentication auth,
            UUID planoId,
            UUID documentoId,
            AtualizarDocumentoRequestDTO dto
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        );
        DocumentoEntity documento = buscarDocumento(planoId, documentoId);

        documento.setNome(dto.nome().trim());
        documento.setVersao(dto.versao().trim());
        substituirArquivo(documento, dto.arquivo());

        return toDetalhado(documentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public DocumentoArquivoResponseDTO baixar(
            Authentication auth,
            UUID planoId,
            UUID documentoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        DocumentoEntity documento = buscarDocumento(planoId, documentoId);

        return new DocumentoArquivoResponseDTO(
                documento.getNomeArquivo(),
                documento.getTipoArquivo(),
                documento.getConteudo()
        );
    }

    @Transactional
    public void remover(Authentication auth, UUID planoId, UUID documentoId) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        );
        buscarDocumento(planoId, documentoId);
        documentoRepository.excluirComDependencias(documentoId);
    }

    private DocumentoEntity buscarDocumento(UUID planoId, UUID documentoId) {
        return documentoRepository.findByIdAndPlanoId(documentoId, planoId)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("O arquivo não pode estar vazio.");
        }
    }

    private void substituirArquivo(
            DocumentoEntity documento,
            MultipartFile arquivo
    ) {
        if (arquivo == null) {
            return;
        }

        validarArquivo(arquivo);
        documento.setConteudo(lerConteudo(arquivo));
        documento.setNomeArquivo(limparNome(arquivo));
        documento.setTipoArquivo(
                Optional.ofNullable(arquivo.getContentType()).orElse(TIPO_PADRAO)
        );
        documento.setTamanho(arquivo.getSize());
    }

    private byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException ex) {
            throw new InvalidRequestException("Não foi possível ler o arquivo.");
        }
    }

    private String limparNome(MultipartFile arquivo) {
        String nome = StringUtils.cleanPath(
                Optional.ofNullable(arquivo.getOriginalFilename()).orElse("arquivo")
        );
        if (nome.contains("..")) {
            throw new InvalidRequestException("Nome de arquivo inválido.");
        }
        return nome;
    }

    private DocumentoResponseDTO toResumo(DocumentoEntity documento) {
        return new DocumentoResponseDTO(
                documento.getId(),
                documento.getNome(),
                documento.getNomeArquivo(),
                documento.getVersao(),
                documento.getTipoArquivo(),
                documento.getTamanho(),
                documento.getClassificacao()
        );
    }

    private DocumentoDetalhadoResponseDTO toDetalhado(DocumentoEntity documento) {
        return new DocumentoDetalhadoResponseDTO(
                documento.getId(),
                documento.getPlano().getId(),
                documento.getNome(),
                documento.getNomeArquivo(),
                documento.getVersao(),
                documento.getTipoArquivo(),
                documento.getTamanho(),
                documento.getClassificacao()
        );
    }
}
