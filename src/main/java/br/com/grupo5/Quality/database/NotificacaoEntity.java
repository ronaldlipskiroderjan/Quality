package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
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
        name = "notificacoes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacao_chave_evento",
                columnNames = "chave_evento"
        ),
        indexes = @Index(
                name = "idx_notificacao_destinatario_criada",
                columnList = "destinatario_participacao_id,criada_em"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity destinatario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nao_conformidade_id", nullable = false)
    private NaoConformidadeEntity naoConformidade;

    @Column(name = "chave_evento", nullable = false, length = 150)
    private String chaveEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacao tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "lida_em")
    private OffsetDateTime lidaEm;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    public boolean lida() {
        return status == StatusNotificacao.LIDA;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof NotificacaoEntity notificacao)) {
            return false;
        }
        return id != null && Objects.equals(id, notificacao.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
