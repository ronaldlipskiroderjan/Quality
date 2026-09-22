package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusComunicacao;
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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "comunicacoes_nc",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comunicacao_nc_idempotencia",
                columnNames = {
                        "nao_conformidade_id",
                        "chave_idempotencia"
                }
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunicacaoNcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nao_conformidade_id", nullable = false)
    private NaoConformidadeEntity naoConformidade;

    @Column(name = "chave_idempotencia", nullable = false, length = 100)
    private String chaveIdempotencia;

    @Column(nullable = false, length = 254)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(nullable = false, length = 10000)
    private String corpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusComunicacao status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "ultima_tentativa_em")
    private OffsetDateTime ultimaTentativaEm;

    @Column(name = "enviada_em")
    private OffsetDateTime enviadaEm;

    @Column(name = "detalhe_erro", length = 500)
    private String detalheErro;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    public boolean enviada() {
        return status == StatusComunicacao.ENVIADA;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ComunicacaoNcEntity comunicacao)) {
            return false;
        }
        return id != null && Objects.equals(id, comunicacao.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
