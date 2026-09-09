package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "participacoes_plano",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participacao_plano_usuario",
                columnNames = {"plano_id", "usuario_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipacaoPlanoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoEntity plano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "participacao_plano_papeis",
            joinColumns = @JoinColumn(name = "participacao_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 40)
    private Set<PapelPlano> papeis = EnumSet.noneOf(PapelPlano.class);

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public boolean possuiPapel(PapelPlano papel) {
        return papeis.contains(papel);
    }

    public boolean possuiPermissao(PermissaoPlano permissao) {
        return papeis.stream().anyMatch(papel -> papel.permite(permissao));
    }

    public Set<PermissaoPlano> getPermissoes() {
        Set<PermissaoPlano> permissoes = EnumSet.noneOf(PermissaoPlano.class);
        papeis.forEach(papel -> permissoes.addAll(papel.getPermissoes()));
        return permissoes;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ParticipacaoPlanoEntity participacao)) {
            return false;
        }
        return id != null && Objects.equals(id, participacao.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
