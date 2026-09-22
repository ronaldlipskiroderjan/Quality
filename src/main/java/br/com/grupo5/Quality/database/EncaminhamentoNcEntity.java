package br.com.grupo5.Quality.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "encaminhamentos_nc",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_encaminhamento_nc_idempotencia",
                columnNames = {"nao_conformidade_id", "chave_idempotencia"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncaminhamentoNcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nao_conformidade_id", nullable = false)
    private NaoConformidadeEntity naoConformidade;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Column(name = "total_destinatarios", nullable = false)
    private int totalDestinatarios;

    @Column(name = "encaminhado_em", nullable = false)
    private OffsetDateTime encaminhadoEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;
}
