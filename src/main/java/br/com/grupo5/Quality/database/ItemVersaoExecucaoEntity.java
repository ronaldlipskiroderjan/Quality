package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "itens_versao_execucao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVersaoExecucaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_execucao_id", nullable = false)
    private VersaoExecucaoChecklistEntity versao;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResultadoItem resultado;

    @Column(length = 3000)
    private String observacao;

    @Column(name = "nc_identificada_em")
    private OffsetDateTime ncIdentificadaEm;

    @Column(name = "responsavel_resolucao", length = 150)
    private String responsavelResolucao;

    @Column(name = "responsavel_participacao_id")
    private UUID responsavelParticipacaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_nc", length = 20)
    private ClassificacaoNaoConformidade classificacaoNc;

    @Column(name = "acao_corretiva", length = 3000)
    private String acaoCorretiva;

    @Column(name = "prazo_resolucao_em")
    private OffsetDateTime prazoResolucaoEm;

    @Column(name = "escalonado_em")
    private OffsetDateTime escalonadoEm;

    @Column(name = "nc_concluida_em")
    private OffsetDateTime ncConcluidaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_nc", length = 30)
    private StatusNaoConformidade statusNc;
}
