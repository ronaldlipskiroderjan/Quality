package br.com.grupo5.Quality.database;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "checklists")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "auditoria_id", nullable = false)
    private ArtefatoEntity auditoria;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "checklist", fetch = FetchType.LAZY)
    private Set<ItemChecklistEntity> itensChecklit;
}
