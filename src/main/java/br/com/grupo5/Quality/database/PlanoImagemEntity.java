package br.com.grupo5.Quality.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "imagens_plano")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoImagemEntity {

    @Id
    @Column(name = "plano_id")
    private UUID planoId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id")
    private PlanoEntity plano;

    @Column(name = "tipo_conteudo", nullable = false, length = 50)
    private String tipoConteudo;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] conteudo;
}
