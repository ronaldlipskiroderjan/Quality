package br.com.grupo5.Quality.database;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itens_checklist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ChecklistEntity checklist;

    private String reposta;

    private LocalDateTime dataIdentificacaoNc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsavel_id")
    private UsuarioEntity responsavel;

    @Column(name = "atividade_processo", nullable = false)
    private String atividadeProcesso;

    @Column(name = "classificacao_NCF")
    private String classificacaoNCF;

    @Column(name = "acao_corretiva")
    private String acaoCorretiva;

    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    @Column(name = "data_escalonamento")
    private LocalDateTime dataEscalonamento;

    @Column(name = "data_conclusao_NC")
    private LocalDateTime dataConclusaoNC;
}

