package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusChecklist;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.ChecklistRepository;
import br.com.grupo5.Quality.database.repository.ItemChecklistRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.dto.request.ItemChecklistRequestDTO;
import br.com.grupo5.Quality.dto.response.ItemChecklistResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistAuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private ChecklistRepository checklistRepository;
    @Mock
    private ItemChecklistRepository itemRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void deveCriarChecklistInicialAbertoComLinhaVazia() {
        Cenario cenario = cenario();
        when(checklistRepository.save(any(ChecklistEntity.class)))
                .thenAnswer(invocation -> {
                    ChecklistEntity checklist = invocation.getArgument(0);
                    checklist.setId(UUID.randomUUID());
                    return checklist;
                });
        when(itemRepository.save(any(ItemChecklistEntity.class)))
                .thenAnswer(invocation -> {
                    ItemChecklistEntity item = invocation.getArgument(0);
                    item.setId(UUID.randomUUID());
                    return item;
                });

        ChecklistEntity response = service().criarInicial(cenario.auditoria());

        assertEquals("1.0", response.getVersao());
        assertEquals(StatusChecklist.PUBLICADO, response.getStatus());
        assertEquals(1, response.getItens().size());
        assertEquals("", response.getItens().getFirst().getPergunta());
        assertEquals(1, cenario.auditoria().getChecklists().size());
    }

    @Test
    void naoDeveCriarSegundoChecklistAtivo() {
        Cenario cenario = cenario();
        cenario.auditoria().getChecklists().add(
                checklist(cenario.auditoria(), "1.0")
        );

        assertThrows(
                AlreadyExistsException.class,
                () -> service().criarInicial(cenario.auditoria())
        );
        verify(checklistRepository, never()).save(any());
    }

    @Test
    void deveAdicionarItemAoChecklistSelecionado() {
        Cenario cenario = cenario();
        ChecklistEntity checklist = checklist(cenario.auditoria(), "1.0");
        cenario.auditoria().getChecklists().add(checklist);
        autorizarEdicao(cenario);
        localizarChecklist(cenario, checklist);
        when(itemRepository.existsByChecklistIdAndOrdem(checklist.getId(), 1))
                .thenReturn(false);
        when(itemRepository.save(any(ItemChecklistEntity.class)))
                .thenAnswer(invocation -> {
                    ItemChecklistEntity item = invocation.getArgument(0);
                    item.setId(UUID.randomUUID());
                    return item;
                });

        ItemChecklistResponseDTO response = service().adicionarItem(
                auth,
                cenario.plano().getId(),
                cenario.artefato().getId(),
                cenario.auditoria().getId(),
                checklist.getId(),
                new ItemChecklistRequestDTO(
                        1, " O requisito possui aceite? "
                )
        );

        assertEquals("O requisito possui aceite?", response.pergunta());
        assertEquals(OrigemItemChecklist.MANUAL, response.origem());
        assertEquals(1, checklist.getItens().size());
    }

    @Test
    void deveAtualizarPerguntaEnquantoChecklistEstaEmExecucao() {
        Cenario cenario = cenario();
        cenario.auditoria().setStatus(StatusAuditoria.EM_ANDAMENTO);
        ChecklistEntity checklist = checklist(cenario.auditoria(), "1.0");
        ItemChecklistEntity item = item(checklist, 1);
        checklist.getItens().add(item);
        cenario.auditoria().getChecklists().add(checklist);
        autorizarEdicao(cenario);
        localizarChecklist(cenario, checklist);
        when(itemRepository.findByIdAndChecklistId(
                item.getId(), checklist.getId()
        )).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        ItemChecklistResponseDTO response = service().atualizarItem(
                auth,
                cenario.plano().getId(),
                cenario.artefato().getId(),
                cenario.auditoria().getId(),
                checklist.getId(),
                item.getId(),
                new ItemChecklistRequestDTO(
                        1,
                        "A pergunta foi revisada durante a auditoria?"
                )
        );

        assertEquals(
                "A pergunta foi revisada durante a auditoria?",
                response.pergunta()
        );
    }

    @Test
    void deveRemoverItemERenumerarOsSeguintes() {
        Cenario cenario = cenario();
        ChecklistEntity checklist = checklist(cenario.auditoria(), "1.0");
        ItemChecklistEntity primeiro = item(checklist, 1);
        ItemChecklistEntity segundo = item(checklist, 2);
        checklist.getItens().add(primeiro);
        checklist.getItens().add(segundo);
        cenario.auditoria().getChecklists().add(checklist);
        autorizarEdicao(cenario);
        localizarChecklist(cenario, checklist);
        when(itemRepository.findByIdAndChecklistId(
                primeiro.getId(), checklist.getId()
        )).thenReturn(Optional.of(primeiro));

        service().removerItem(
                auth,
                cenario.plano().getId(),
                cenario.artefato().getId(),
                cenario.auditoria().getId(),
                checklist.getId(),
                primeiro.getId()
        );

        assertEquals(1, checklist.getItens().size());
        assertEquals(segundo, checklist.getItens().getFirst());
        verify(checklistRepository).saveAndFlush(checklist);
        verify(itemRepository).compactarOrdensApos(checklist.getId(), 1);
    }

    private ChecklistAuditoriaService service() {
        return new ChecklistAuditoriaService(
                auditoriaRepository,
                checklistRepository,
                itemRepository,
                naoConformidadeRepository,
                acessoPlanoService
        );
    }

    private void autorizarEdicao(Cenario cenario) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.AUDITAR
        )).thenReturn(cenario.auditor());
        when(auditoriaRepository
                .findByIdAndArtefatoIdAndArtefatoDocumentoPlanoId(
                        cenario.auditoria().getId(),
                        cenario.artefato().getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(cenario.auditoria()));
    }

    private void localizarChecklist(
            Cenario cenario,
            ChecklistEntity checklist
    ) {
        when(checklistRepository
                .findByIdAndAuditoriaIdAndAuditoriaArtefatoIdAndAuditoriaArtefatoDocumentoPlanoId(
                        checklist.getId(),
                        cenario.auditoria().getId(),
                        cenario.artefato().getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(checklist));
    }

    private Cenario cenario() {
        PlanoEntity plano = PlanoEntity.builder().id(UUID.randomUUID()).build();
        ParticipacaoPlanoEntity auditor = ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome("Auditor")
                        .email("auditor@quality.test")
                        .build())
                .build();
        ArtefatoEntity artefato = ArtefatoEntity.builder()
                .id(UUID.randomUUID())
                .documento(DocumentoEntity.builder()
                        .id(UUID.randomUUID())
                        .plano(plano)
                        .build())
                .auditor(auditor)
                .status(StatusArtefato.EM_ANDAMENTO)
                .build();
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .artefato(artefato)
                .auditor(auditor)
                .status(StatusAuditoria.EM_ANDAMENTO)
                .dataInicio(LocalDateTime.now())
                .build();
        artefato.setAuditoria(auditoria);
        return new Cenario(plano, auditor, artefato, auditoria);
    }

    private ChecklistEntity checklist(
            AuditoriaEntity auditoria,
            String versao
    ) {
        return ChecklistEntity.builder()
                .id(UUID.randomUUID())
                .auditoria(auditoria)
                .versao(versao)
                .status(StatusChecklist.PUBLICADO)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    private ItemChecklistEntity item(
            ChecklistEntity checklist,
            int ordem
    ) {
        return ItemChecklistEntity.builder()
                .id(UUID.randomUUID())
                .checklist(checklist)
                .ordem(ordem)
                .pergunta("Pergunta " + ordem)
                .origem(OrigemItemChecklist.MANUAL)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private record Cenario(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor,
            ArtefatoEntity artefato,
            AuditoriaEntity auditoria
    ) {
    }
}
