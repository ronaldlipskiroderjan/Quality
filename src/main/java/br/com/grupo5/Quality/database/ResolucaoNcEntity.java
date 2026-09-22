package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusResolucao;
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
@Table(name = "resolucoes_nc")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolucaoNcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nao_conformidade_id", nullable = false)
    private NaoConformidadeEntity naoConformidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity responsavel;

    @Column(nullable = false, length = 5000)
    private String descricao;

    @Column(length = 2000)
    private String evidencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusResolucao status;

    @Column(name = "informada_em", nullable = false)
    private OffsetDateTime informadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_participacao_id")
    private ParticipacaoPlanoEntity auditor;

    @Column(name = "observacao_auditor", length = 3000)
    private String observacaoAuditor;

    @Column(name = "validada_em")
    private OffsetDateTime validadaEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    public boolean aguardaValidacao() {
        return status == StatusResolucao.INFORMADA;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ResolucaoNcEntity resolucao)) {
            return false;
        }
        return id != null && Objects.equals(id, resolucao.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
