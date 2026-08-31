package br.com.grupo5.Quality.database;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plano_id",  nullable = false)
    private PlanoEntity plano;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auditoria_id")
    private ArtefatoEntity artefato;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String versao;

    @Lob
    @Column(columnDefinition = "BYTEA")
    private byte[] dados;

    @Column(name = "local_armazenamento", nullable = false)
    private String localArmazenamento;
}

