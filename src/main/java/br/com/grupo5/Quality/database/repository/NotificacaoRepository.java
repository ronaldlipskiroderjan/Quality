package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.NotificacaoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacaoRepository
        extends JpaRepository<NotificacaoEntity, UUID> {

    List<NotificacaoEntity> findAllByNaoConformidadeId(UUID naoConformidadeId);

    Page<NotificacaoEntity>
            findAllByDestinatarioUsuarioEmailIgnoreCase(
                    String email,
                    Pageable pageable
            );

    Optional<NotificacaoEntity>
            findByIdAndDestinatarioUsuarioEmailIgnoreCase(
                    UUID notificacaoId,
                    String email
            );

    boolean existsByChaveEvento(String chaveEvento);
    boolean existsByNaoConformidadeIdAndDestinatarioId(
            UUID naoConformidadeId,
            UUID destinatarioId
    );

}
