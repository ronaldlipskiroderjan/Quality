package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusAuditoria;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "auditorias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artefato_id", nullable = false, unique = true)
    private ArtefatoEntity artefato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity auditor;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "auditoria_documentos_referencia",
            joinColumns = @JoinColumn(name = "auditoria_id"),
            inverseJoinColumns = @JoinColumn(name = "documento_id")
    )
    private Set<DocumentoEntity> documentosReferencia = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "auditoria",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ChecklistEntity> checklists = new ArrayList<>();

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conclusao_excepcional_autorizada_por_id")
    private ParticipacaoPlanoEntity conclusaoExcepcionalAutorizadaPor;

    @Column(name = "conclusao_excepcional_autorizada_em")
    private LocalDateTime conclusaoExcepcionalAutorizadaEm;

    @Column(name = "justificativa_conclusao_excepcional", length = 2000)
    private String justificativaConclusaoExcepcional;

    private int conformes;

    @Column(name = "nao_conformes")
    private int naoConformes;

    @Column(name = "nao_aplicaveis")
    private int naoAplicaveis;

    @Column(name = "aderencia_percentual", precision = 5, scale = 2)
    private BigDecimal aderenciaPercentual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAuditoria status;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Builder.Default
    @OneToMany(
            mappedBy = "auditoria",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RespostaAuditoriaEntity> respostas = new ArrayList<>();

    public boolean emAndamento() {
        return status == StatusAuditoria.EM_ANDAMENTO;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof AuditoriaEntity auditoria)) {
            return false;
        }
        return id != null && Objects.equals(id, auditoria.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
