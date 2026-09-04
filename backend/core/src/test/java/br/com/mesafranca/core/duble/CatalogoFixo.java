package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.domain.IdentificadorDeJogo;
import br.com.mesafranca.core.port.out.CatalogoDeJogos;
import br.com.mesafranca.core.spi.DefinicaoDeJogo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo com as definicoes que o teste escolher. */
public final class CatalogoFixo implements CatalogoDeJogos {

    private final Map<IdentificadorDeJogo, DefinicaoDeJogo> mapa = new LinkedHashMap<>();

    public CatalogoFixo(DefinicaoDeJogo... definicoes) {
        for (DefinicaoDeJogo definicao : definicoes) {
            mapa.put(definicao.identificador(), definicao);
        }
    }

    @Override
    public Optional<DefinicaoDeJogo> buscar(IdentificadorDeJogo identificador) {
        return Optional.ofNullable(mapa.get(identificador));
    }

    @Override
    public List<IdentificadorDeJogo> disponiveis() {
        return List.copyOf(mapa.keySet());
    }
}
