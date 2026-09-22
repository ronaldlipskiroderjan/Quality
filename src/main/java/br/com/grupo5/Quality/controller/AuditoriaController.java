package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.AutorizarConclusaoAuditoriaRequestDTO;
import br.com.grupo5.Quality.dto.request.AtualizarItemVersaoExecucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.ItemChecklistRequestDTO;
import br.com.grupo5.Quality.dto.request.RespostaAuditoriaRequestDTO;
import br.com.grupo5.Quality.dto.request.SalvarVersaoExecucaoRequestDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaDetalhadaResponseDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.ItemAuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.ItemChecklistResponseDTO;
import br.com.grupo5.Quality.dto.response.ItemVersaoExecucaoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.VersaoExecucaoChecklistResponseDTO;
import br.com.grupo5.Quality.service.AuditoriaService;
import br.com.grupo5.Quality.service.ChecklistAuditoriaService;
import br.com.grupo5.Quality.service.ExecucaoChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/v1/planos/{planoId}/artefatos/{artefatoId}/auditorias"
)
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final ChecklistAuditoriaService checklistService;
    private final ExecucaoChecklistService execucaoChecklistService;

    @GetMapping
    public PaginaResponseDTO<AuditoriaResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PageableDefault(
                    size = 15,
                    sort = "dataInicio",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return auditoriaService.listar(auth, planoId, artefatoId, pageable);
    }

    @GetMapping("/{auditoriaId}")
    public AuditoriaDetalhadaResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId
    ) {
        return auditoriaService.buscar(
                auth,
                planoId,
                artefatoId,
                auditoriaId
        );
    }

    @PostMapping("/{auditoriaId}/checklists/{checklistId}/itens")
    public ResponseEntity<ItemChecklistResponseDTO> adicionarItem(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @Valid @RequestBody ItemChecklistRequestDTO dto
    ) {
        ItemChecklistResponseDTO item = checklistService.adicionarItem(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                dto
        );
        return ResponseEntity.status(201).body(item);
    }

    @PutMapping("/{auditoriaId}/checklists/{checklistId}/itens/{itemId}")
    public ItemChecklistResponseDTO atualizarItem(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemChecklistRequestDTO dto
    ) {
        return checklistService.atualizarItem(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                itemId,
                dto
        );
    }

    @DeleteMapping("/{auditoriaId}/checklists/{checklistId}/itens/{itemId}")
    public ResponseEntity<Void> removerItem(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @PathVariable UUID itemId
    ) {
        checklistService.removerItem(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                itemId
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{auditoriaId}/checklists/{checklistId}/versoes-execucao")
    public List<VersaoExecucaoChecklistResponseDTO> listarVersoesExecucao(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId
    ) {
        return execucaoChecklistService.listarVersoes(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
    }

    @PostMapping("/{auditoriaId}/checklists/{checklistId}/versoes-execucao")
    public VersaoExecucaoChecklistResponseDTO salvarVersaoExecucao(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @Valid @RequestBody SalvarVersaoExecucaoRequestDTO dto
    ) {
        return execucaoChecklistService.salvarVersao(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                dto.observacao()
        );
    }

    @PutMapping("/{auditoriaId}/checklists/{checklistId}/versoes-execucao/{versaoId}/itens/{itemId}")
    public ItemVersaoExecucaoResponseDTO atualizarItemVersaoExecucao(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @PathVariable UUID versaoId,
            @PathVariable UUID itemId,
            @Valid @RequestBody AtualizarItemVersaoExecucaoRequestDTO dto
    ) {
        return execucaoChecklistService.atualizarItemVersao(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                versaoId,
                itemId,
                dto
        );
    }

    @DeleteMapping("/{auditoriaId}/checklists/{checklistId}/versoes-execucao/{versaoId}/itens/{itemId}")
    public VersaoExecucaoChecklistResponseDTO removerItemVersaoExecucao(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId,
            @PathVariable UUID versaoId,
            @PathVariable UUID itemId
    ) {
        return execucaoChecklistService.removerItemVersao(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                checklistId,
                versaoId,
                itemId
        );
    }

    @PostMapping("/{auditoriaId}/checklists/{checklistId}/encerramento")
    public AuditoriaDetalhadaResponseDTO encerrarChecklist(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID checklistId
    ) {
        execucaoChecklistService.encerrar(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        return auditoriaService.buscar(auth, planoId, artefatoId, auditoriaId);
    }

    @PostMapping("/{auditoriaId}/autorizacao-conclusao")
    public AuditoriaDetalhadaResponseDTO autorizarConclusaoExcepcional(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @Valid @RequestBody AutorizarConclusaoAuditoriaRequestDTO dto
    ) {
        execucaoChecklistService.autorizarConclusaoExcepcional(
                auth, planoId, artefatoId, auditoriaId, dto.justificativa()
        );
        return auditoriaService.buscar(auth, planoId, artefatoId, auditoriaId);
    }

    @PutMapping("/{auditoriaId}/respostas/{itemId}")
    public ItemAuditoriaResponseDTO responder(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId,
            @PathVariable UUID itemId,
            @Valid @RequestBody RespostaAuditoriaRequestDTO dto
    ) {
        return auditoriaService.responder(
                auth,
                planoId,
                artefatoId,
                auditoriaId,
                itemId,
                dto
        );
    }

    @PostMapping("/{auditoriaId}/conclusao")
    public AuditoriaDetalhadaResponseDTO concluir(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @PathVariable UUID auditoriaId
    ) {
        return auditoriaService.concluir(
                auth,
                planoId,
                artefatoId,
                auditoriaId
        );
    }
}
