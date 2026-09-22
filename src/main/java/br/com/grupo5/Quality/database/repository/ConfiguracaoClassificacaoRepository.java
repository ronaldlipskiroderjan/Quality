package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ConfiguracaoClassificacaoEntity;
import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoClassificacaoRepository
        extends JpaRepository<ConfiguracaoClassificacaoEntity, UUID> {

    List<ConfiguracaoClassificacaoEntity> findAllByPlanoIdOrderByClassificacao(
            UUID planoId
    );

    Optional<ConfiguracaoClassificacaoEntity>
            findByPlanoIdAndClassificacaoAndAtivaTrue(
                    UUID planoId,
                    ClassificacaoNaoConformidade classificacao
            );
}
