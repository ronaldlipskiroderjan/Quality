package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "nao_conformidades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nc_resposta",
                columnNames = "resposta_id"
        ),
        indexes = @Index(
                name = "idx_nc_status_prazo",
                columnList = "status,prazo_em"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NaoConformidadeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resposta_id", nullable = false)
    private RespostaAuditoriaEntity resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_participacao_id")
    private ParticipacaoPlanoEntity responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassificacaoNaoConformidade classificacao;

    @Column(nullable = false, length = 3000)
    private String descricao;

    @Column(name = "acao_corretiva", nullable = false, length = 3000)
    private String acaoCorretiva;

    @Column(name = "identificado_em", nullable = false)
    private OffsetDateTime identificadoEm;

    @Column(name = "prazo_resolucao_horas", nullable = false)
    private int prazoResolucaoHoras;

    @Column(name = "enviada_em")
    private OffsetDateTime enviadaEm;

    @Column(name = "prazo_em")
    private OffsetDateTime prazoEm;

    @Column(name = "concluida_em")
    private OffsetDateTime concluidaEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusNaoConformidade status;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Builder.Default
    @OneToMany(mappedBy = "naoConformidade", fetch = FetchType.LAZY)
    private List<EscalonamentoNcEntity> escalonamentos = new ArrayList<>();

    public boolean rascunho() {
        return status == StatusNaoConformidade.RASCUNHO;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof NaoConformidadeEntity naoConformidade)) {
            return false;
        }
        return id != null && Objects.equals(id, naoConformidade.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
