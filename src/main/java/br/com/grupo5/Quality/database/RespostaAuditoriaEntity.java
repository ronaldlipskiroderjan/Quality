package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.ResultadoItem;
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
import jakarta.persistence.Version;
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
        name = "respostas_auditoria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resposta_auditoria_item",
                columnNames = {"auditoria_id", "item_checklist_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespostaAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private AuditoriaEntity auditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_checklist_id", nullable = false)
    private ItemChecklistEntity item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoItem resultado;

    @Column(length = 3000)
    private String observacao;

    @Column(name = "respondido_em", nullable = false)
    private LocalDateTime respondidoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof RespostaAuditoriaEntity resposta)) {
            return false;
        }
        return id != null && Objects.equals(id, resposta.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
