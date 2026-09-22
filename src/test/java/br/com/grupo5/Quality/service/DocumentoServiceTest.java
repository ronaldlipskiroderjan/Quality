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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;
    @Mock
    private MultipartFile arquivo;

    @Test
    void deveAdicionarDocumentoComPermissao() throws Exception {
        PlanoEntity plano = plano();
        byte[] conteudo = {1, 2, 3};

        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        )).thenReturn(plano);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getBytes()).thenReturn(conteudo);
        when(arquivo.getOriginalFilename()).thenReturn("modelo.pdf");
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(arquivo.getSize()).thenReturn(3L);
        when(documentoRepository.save(any(DocumentoEntity.class)))
                .thenAnswer(invocation -> {
                    DocumentoEntity documento = invocation.getArgument(0);
                    documento.setId(UUID.randomUUID());
                    return documento;
                });

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                " Modelo ",
                " 1.0 ",
                arquivo,
                Classificacao.REFERENCIA
        );
        DocumentoDetalhadoResponseDTO response = service().adicionar(
                auth,
                plano.getId(),
                dto
        );

        assertEquals(plano.getId(), response.planoId());
        assertEquals("Modelo", response.nome());
        assertEquals("modelo.pdf", response.nomeArquivo());
        assertEquals(3L, response.tamanho());
        assertEquals(Classificacao.REFERENCIA, response.classificacao());
    }

    @Test
    void deveRecusarArquivoVazio() {
        PlanoEntity plano = plano();
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        )).thenReturn(plano);
        when(arquivo.isEmpty()).thenReturn(true);

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                "Modelo",
                "1.0",
                arquivo,
                Classificacao.REFERENCIA
        );

        assertThrows(
                InvalidRequestException.class,
                () -> service().adicionar(auth, plano.getId(), dto)
        );
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void deveListarDocumentosDoPlano() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano);
        when(acessoPlanoService.buscarPlano(auth, plano.getId())).thenReturn(plano);
        PageRequest pageable = PageRequest.of(0, 15);
        when(documentoRepository.findAllByPlanoIdAndClassificacao(
                        plano.getId(),
                        Classificacao.AUDITADO,
                        pageable
                )).thenReturn(new PageImpl<>(List.of(documento), pageable, 1));

        PaginaResponseDTO<DocumentoResponseDTO> response = service().listar(
                auth,
                plano.getId(),
                Classificacao.AUDITADO,
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertEquals(documento.getId(), response.conteudo().getFirst().id());
    }

    @Test
    void deveBaixarDocumentoDoPlano() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano);
        when(acessoPlanoService.buscarPlano(auth, plano.getId())).thenReturn(plano);
        when(documentoRepository.findByIdAndPlanoId(documento.getId(), plano.getId()))
                .thenReturn(Optional.of(documento));

        DocumentoArquivoResponseDTO response = service().baixar(
                auth,
                plano.getId(),
                documento.getId()
        );

        assertEquals("modelo.pdf", response.nome());
        assertArrayEquals(new byte[]{1, 2, 3}, response.conteudo());
    }

    @Test
    void deveAtualizarNomeEVersaoPreservandoArquivoAtual() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano);
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        )).thenReturn(plano);
        when(documentoRepository.findByIdAndPlanoId(documento.getId(), plano.getId()))
                .thenReturn(Optional.of(documento));
        when(documentoRepository.save(documento)).thenReturn(documento);

        DocumentoDetalhadoResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                documento.getId(),
                new AtualizarDocumentoRequestDTO(" Modelo revisado ", " 2.0 ", null)
        );

        assertEquals("Modelo revisado", response.nome());
        assertEquals("2.0", response.versao());
        assertEquals("modelo.pdf", response.nomeArquivo());
        assertArrayEquals(new byte[]{1, 2, 3}, documento.getConteudo());
    }

    @Test
    void deveAtualizarDocumentoSubstituindoArquivo() throws Exception {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano);
        byte[] novoConteudo = {4, 5, 6, 7};
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        )).thenReturn(plano);
        when(documentoRepository.findByIdAndPlanoId(documento.getId(), plano.getId()))
                .thenReturn(Optional.of(documento));
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getBytes()).thenReturn(novoConteudo);
        when(arquivo.getOriginalFilename()).thenReturn("modelo-v2.docx");
        when(arquivo.getContentType()).thenReturn(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        when(arquivo.getSize()).thenReturn(4L);
        when(documentoRepository.save(documento)).thenReturn(documento);

        DocumentoDetalhadoResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                documento.getId(),
                new AtualizarDocumentoRequestDTO("Modelo", "2.0", arquivo)
        );

        assertEquals("modelo-v2.docx", response.nomeArquivo());
        assertEquals(4L, response.tamanho());
        assertArrayEquals(novoConteudo, documento.getConteudo());
    }

    @Test
    void deveInformarDocumentoAusenteNoPlano() {
        PlanoEntity plano = plano();
        UUID documentoId = UUID.randomUUID();
        when(acessoPlanoService.buscarPlano(auth, plano.getId())).thenReturn(plano);
        when(documentoRepository.findByIdAndPlanoId(documentoId, plano.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service().buscar(auth, plano.getId(), documentoId)
        );
    }

    @Test
    void deveRemoverDocumentoComPermissao() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano);
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_DOCUMENTOS
        )).thenReturn(plano);
        when(documentoRepository.findByIdAndPlanoId(documento.getId(), plano.getId()))
                .thenReturn(Optional.of(documento));

        service().remover(auth, plano.getId(), documento.getId());

        verify(documentoRepository).excluirComDependencias(documento.getId());
    }

    private DocumentoService service() {
        return new DocumentoService(documentoRepository, acessoPlanoService);
    }

    private PlanoEntity plano() {
        return PlanoEntity.builder().id(UUID.randomUUID()).build();
    }

    private DocumentoEntity documento(PlanoEntity plano) {
        return DocumentoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .nome("Modelo")
                .nomeArquivo("modelo.pdf")
                .versao("1.0")
                .tipoArquivo("application/pdf")
                .tamanho(3L)
                .conteudo(new byte[]{1, 2, 3})
                .classificacao(Classificacao.AUDITADO)
                .build();
    }
}
