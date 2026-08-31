package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_criador", nullable = false)
    private String criador;

    @Column(nullable = false)
    private String nomeProjeto;

    @Column(nullable = false)
    private String versao;

    @Column(nullable = false, unique = true)
    private String objetivo;

    @Column(name = "visao_geral")
    private String visaoGeral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "criado_em",  nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL)
    private Set<DocumentoEntity> documentos = new HashSet<>();

    // ====================================================
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlanoEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
