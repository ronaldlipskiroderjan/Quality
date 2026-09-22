package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.StatusArtefato;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "artefatos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtefatoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity auditor;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 30)
    private String versao;

    @Column(name = "data_planejada", nullable = false)
    private LocalDate dataPlanejada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusArtefato status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToOne(
            mappedBy = "artefato",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private AuditoriaEntity auditoria;

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ArtefatoEntity artefato)) {
            return false;
        }
        return id != null && Objects.equals(id, artefato.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
