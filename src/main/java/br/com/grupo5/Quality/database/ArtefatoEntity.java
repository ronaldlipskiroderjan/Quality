package br.com.grupo5.Quality.database;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditoria_id", nullable = false)
    private AuditoriaEntity auditoria;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auditor_id")
    private UsuarioEntity auditor;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private LocalDateTime dataAvaliacao;

    @Column(nullable = false)
    private String versao;

    // ======================================================================

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArtefatoEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
