package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "itens_checklist",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_checklist_ordem",
                columnNames = {"checklist_id", "ordem"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ChecklistEntity checklist;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, length = 1000)
    private String pergunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemItemChecklist origem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ItemChecklistEntity item)) {
            return false;
        }
        return id != null && Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
