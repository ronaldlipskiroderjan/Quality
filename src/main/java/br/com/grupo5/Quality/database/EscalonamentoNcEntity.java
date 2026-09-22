package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "escalonamentos_nc",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_escalonamento_nc_idempotencia",
                        columnNames = {
                                "nao_conformidade_id",
                                "chave_idempotencia"
                        }
                ),
                @UniqueConstraint(
                        name = "uk_escalonamento_nc_nivel",
                        columnNames = {
                                "nao_conformidade_id",
                                "nivel"
                        }
                )
        },
        indexes = @Index(
                name = "idx_escalonamento_nc_data",
                columnList = "nao_conformidade_id,escalonado_em"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalonamentoNcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nao_conformidade_id", nullable = false)
    private NaoConformidadeEntity naoConformidade;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelEscalonamento nivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity auditor;

    @Column(length = 3000)
    private String observacao;

    @Column(name = "prazo_horas", nullable = false)
    private int prazoHoras;

    @Column(name = "escalonado_em", nullable = false)
    private OffsetDateTime escalonadoEm;

    @Column(name = "prazo_em", nullable = false)
    private OffsetDateTime prazoEm;

    @Column(name = "prazo_original_em", nullable = false)
    private OffsetDateTime prazoOriginalEm;

    @Column(name = "revisado_em")
    private OffsetDateTime revisadoEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof EscalonamentoNcEntity escalonamento)) {
            return false;
        }
        return id != null && Objects.equals(id, escalonamento.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
