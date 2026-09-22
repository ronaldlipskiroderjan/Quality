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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "atividades_plano")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtividadePlanoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoEntity plano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_participacao_id", nullable = false)
    private ParticipacaoPlanoEntity autor;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}
