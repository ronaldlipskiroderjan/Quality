package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ConfiguracaoClassificacaoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.ConfiguracaoClassificacaoRepository;
import br.com.grupo5.Quality.dto.request.AtualizarClassificacoesPlanoRequestDTO;
import br.com.grupo5.Quality.dto.request.ConfiguracaoClassificacaoRequestDTO;
import br.com.grupo5.Quality.dto.response.ConfiguracaoClassificacaoResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfiguracaoPlanoService {

    private final ConfiguracaoClassificacaoRepository configuracaoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional(readOnly = true)
    public List<ConfiguracaoClassificacaoResponseDTO> listarClassificacoes(
            Authentication auth,
            UUID planoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return configuracaoRepository
                .findAllByPlanoIdOrderByClassificacao(planoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<ConfiguracaoClassificacaoResponseDTO> atualizarClassificacoes(
            Authentication auth,
            UUID planoId,
            AtualizarClassificacoesPlanoRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.EDITAR
        );
        validarClassificacoes(dto.classificacoes());

        List<ConfiguracaoClassificacaoEntity> existentes = configuracaoRepository
                .findAllByPlanoIdOrderByClassificacao(planoId);

        for (ConfiguracaoClassificacaoRequestDTO entrada : dto.classificacoes()) {
            ConfiguracaoClassificacaoEntity configuracao = existentes.stream()
                    .filter(item -> item.getClassificacao() == entrada.classificacao())
                    .findFirst()
                    .orElseGet(() -> ConfiguracaoClassificacaoEntity.builder()
                            .plano(plano)
                            .classificacao(entrada.classificacao())
                            .build());
            configuracao.setPrazoHoras(totalHoras(entrada));
            configuracao.setAtiva(entrada.ativa());
            configuracaoRepository.save(configuracao);
        }

        return configuracaoRepository
                .findAllByPlanoIdOrderByClassificacao(planoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConfiguracaoClassificacaoEntity buscarAtiva(
            UUID planoId,
            ClassificacaoNaoConformidade classificacao
    ) {
        return configuracaoRepository
                .findByPlanoIdAndClassificacaoAndAtivaTrue(planoId, classificacao)
                .orElseThrow(() -> new InvalidRequestException(
                        "A classificação não está habilitada neste plano."
                ));
    }

    private void validarClassificacoes(
            List<ConfiguracaoClassificacaoRequestDTO> classificacoes
    ) {
        Set<ClassificacaoNaoConformidade> unicas = classificacoes.stream()
                .map(ConfiguracaoClassificacaoRequestDTO::classificacao)
                .collect(Collectors.toCollection(
                        () -> EnumSet.noneOf(ClassificacaoNaoConformidade.class)
                ));
        if (unicas.size() != classificacoes.size()) {
            throw new InvalidRequestException(
                    "Cada classificação deve aparecer apenas uma vez."
            );
        }
        if (classificacoes.stream().noneMatch(ConfiguracaoClassificacaoRequestDTO::ativa)) {
            throw new InvalidRequestException(
                    "Mantenha ao menos uma classificação ativa no plano."
            );
        }
        if (classificacoes.stream()
                .filter(ConfiguracaoClassificacaoRequestDTO::ativa)
                .anyMatch(item -> totalHoras(item) <= 0)) {
            throw new InvalidRequestException(
                    "O prazo das classificações ativas deve ser de ao menos uma hora."
            );
        }
    }

    private int totalHoras(ConfiguracaoClassificacaoRequestDTO configuracao) {
        return configuracao.prazoDias() * 24 + configuracao.prazoHoras();
    }

    private ConfiguracaoClassificacaoResponseDTO toResponse(
            ConfiguracaoClassificacaoEntity configuracao
    ) {
        return new ConfiguracaoClassificacaoResponseDTO(
                configuracao.getId(),
                configuracao.getClassificacao(),
                configuracao.getPrazoHoras() / 24,
                configuracao.getPrazoHoras() % 24,
                configuracao.isAtiva()
        );
    }
}
