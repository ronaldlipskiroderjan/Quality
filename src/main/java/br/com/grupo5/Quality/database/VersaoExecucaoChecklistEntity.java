package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.TipoVersaoExecucaoChecklist;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "versoes_execucao_checklist",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_versao_execucao_checklist_numero",
                columnNames = {"checklist_id", "numero"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersaoExecucaoChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ChecklistEntity checklist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity autor;

    @Column(nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVersaoExecucaoChecklist tipo;

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Builder.Default
    @OrderBy("ordem ASC")
    @OneToMany(
            mappedBy = "versao",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemVersaoExecucaoEntity> itens = new ArrayList<>();
}
