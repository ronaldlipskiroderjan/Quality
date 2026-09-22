package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusChecklist;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "checklists",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_checklist_auditoria_versao",
                columnNames = {"auditoria_id", "versao"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private AuditoriaEntity auditoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "versao_anterior_id")
    private ChecklistEntity versaoAnterior;

    @Column(nullable = false, length = 30)
    private String versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChecklist status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Builder.Default
    @OrderBy("ordem ASC")
    @OneToMany(
            mappedBy = "checklist",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemChecklistEntity> itens = new ArrayList<>();

    public boolean rascunho() {
        return status == StatusChecklist.RASCUNHO;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ChecklistEntity checklist)) {
            return false;
        }
        return id != null && Objects.equals(id, checklist.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
