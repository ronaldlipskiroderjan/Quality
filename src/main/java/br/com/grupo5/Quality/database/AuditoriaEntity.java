package br.com.grupo5.Quality.database;

import br.com.grupo5.Quality.database.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditorias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artefato_id", nullable = false)
    private ArtefatoEntity artefato;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "checklist_id")
    private ChecklistEntity checklist;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    private int conformes;

    @Column(name = "nao_conforme")
    private int naoConformes;

    @Column(name = "nao_aplicaveis")
    private int naoAplicaveis;

    @Column(name = "aderencia_percentual")
    private BigDecimal aderenciaPercentual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
}
