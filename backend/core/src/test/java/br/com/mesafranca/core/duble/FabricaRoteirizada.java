package br.com.mesafranca.core.duble;

import br.com.mesafranca.core.port.out.FabricaDeAleatoriedade;
import br.com.mesafranca.core.port.out.FonteDeAleatoriedade;
import java.util.ArrayList;
import java.util.List;

/** Fabrica que entrega sempre a mesma fonte roteirizada e uma semente fixa. */
public final class FabricaRoteirizada implements FabricaDeAleatoriedade {

    private final FonteDeAleatoriedade fonte;
    private final long semente;
    private final List<Long> passosPedidos = new ArrayList<>();

    public FabricaRoteirizada(FonteDeAleatoriedade fonte, long semente) {
        this.fonte = fonte;
        this.semente = semente;
    }

    public static FabricaRoteirizada com(int... valores) {
        return new FabricaRoteirizada(AleatoriedadeRoteirizada.de(valores), 7L);
    }

    @Override
    public long novaSemente() {
        return semente;
    }

    @Override
    public FonteDeAleatoriedade derivada(long semente, long passo) {
        passosPedidos.add(passo);
        return fonte;
    }

    /** Passos de derivacao pedidos, para provar que a fonte varia a cada acao. */
    public List<Long> passosPedidos() {
        return List.copyOf(passosPedidos);
    }
}
