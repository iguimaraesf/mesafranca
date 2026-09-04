package br.com.mesafranca.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fronteiras de todo o repositório (RNF-13).
 *
 * <p>Este módulo não tem código de produção: existe só para ser o único ponto
 * que enxerga {@code core}, {@code games} e {@code adapters} ao mesmo tempo.
 * Antes ele era o {@code bootstrap}; com o Spring fora do escopo (ADR-0008),
 * as regras precisavam de um lugar que não dependesse de framework nenhum.
 *
 * <p>As regras aqui são as mesmas listadas em docs/ARCHITECTURE.md. Se aquele
 * documento mudar, este arquivo muda junto.
 */
@DisplayName("Arquitetura do repositorio")
class ArquiteturaTest {

    /**
     * Sem {@code DO_NOT_INCLUDE_JARS}, de propósito.
     *
     * <p>Neste módulo, {@code core}, {@code games} e {@code adapters} chegam
     * como <strong>jars</strong> de dependência, e não como diretórios de
     * classes. Excluir jars deixaria o importador vazio e todas as regras
     * abaixo passariam a reprovar por "nenhuma classe verificada" — que é o
     * pior tipo de falha, porque parece rigor e é o contrário.
     *
     * <p>Em {@code ArquiteturaDoCoreTest} o mesmo não acontece: lá as classes
     * estão em {@code target/classes} do próprio módulo.
     */
    private static final JavaClasses TUDO = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.mesafranca");

    @Test
    @DisplayName("1. o core nao conhece framework, ORM, serializacao nem rede")
    void core_nao_depende_de_infraestrutura() {
        noClasses()
                .that().resideInAPackage("br.com.mesafranca.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "javax.persistence..",
                        "com.fasterxml..", "java.net..", "java.sql..", "javax.sql..")
                .because("ADR-0001: a dependencia aponta para dentro")
                .check(TUDO);
    }

    @Test
    @DisplayName("2. o core nao conhece jogo; nenhum jogo conhece adaptador")
    void a_dependencia_aponta_para_dentro() {
        noClasses()
                .that().resideInAPackage("br.com.mesafranca.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.com.mesafranca.games..", "br.com.mesafranca.adapter..")
                .because("RNF-50: acrescentar um jogo nao pode exigir alterar o core")
                .check(TUDO);

        noClasses()
                .that().resideInAPackage("br.com.mesafranca.games..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.com.mesafranca.adapter..", "org.springframework..", "java.sql..")
                .because("um jogo depende do core e de mais nada")
                .check(TUDO);
    }

    @Test
    @DisplayName("3. nenhum jogo conhece outro jogo")
    void os_jogos_nao_se_conhecem() {
        for (String jogo : new String[] {"ludo", "loveletter", "uno"}) {
            noClasses()
                    .that().resideInAPackage("br.com.mesafranca.games." + jogo + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            outros(jogo))
                    .because("cada jogo e um plugin isolado (PRD secao 25)")
                    .check(TUDO);
        }
    }

    private static String[] outros(String jogo) {
        return switch (jogo) {
            case "ludo" -> new String[] {
                    "br.com.mesafranca.games.loveletter..", "br.com.mesafranca.games.uno.." };
            case "loveletter" -> new String[] {
                    "br.com.mesafranca.games.ludo..", "br.com.mesafranca.games.uno.." };
            default -> new String[] {
                    "br.com.mesafranca.games.ludo..", "br.com.mesafranca.games.loveletter.." };
        };
    }

    @Test
    @DisplayName("4. acaso, tempo e identidade entram por porta, no core e nos jogos")
    void nem_core_nem_jogos_sorteiam_ou_leem_o_relogio() {
        noClasses()
                .that().resideInAnyPackage("br.com.mesafranca.core..", "br.com.mesafranca.games..")
                .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Random")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.security.SecureRandom")
                .orShould().callMethod(Math.class, "random")
                .orShould().callMethod(System.class, "currentTimeMillis")
                .orShould().callMethod(System.class, "nanoTime")
                .orShould().callMethod(Instant.class, "now")
                .orShould().callMethod(LocalDate.class, "now")
                .orShould().callMethod(LocalDateTime.class, "now")
                .orShould().callMethod(UUID.class, "randomUUID")
                .because("ADR-0006: sem isso, teste de regra deixa de ser deterministico")
                .check(TUDO);
    }

    @Test
    @DisplayName("5. toda porta de saida e uma interface")
    void portas_de_saida_sao_interfaces() {
        classes()
                .that().resideInAPackage("br.com.mesafranca.core.port.out..")
                .should().beInterfaces()
                .check(TUDO);
    }

    @Test
    @DisplayName("6. o dominio nao tem campo mutavel")
    void dominio_e_imutavel() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("br.com.mesafranca.core.domain..")
                .and().areNotStatic()
                .should().beFinal()
                .because("PRD secao 44.4: aplicar devolve novo estado")
                .check(TUDO);
    }

    @Test
    @DisplayName("7. regra de jogo nao mora em adaptador")
    void adaptadores_nao_implementam_a_spi_de_jogo() {
        noClasses()
                .that().resideInAPackage("br.com.mesafranca.adapter..")
                .should().implement("br.com.mesafranca.core.spi.DefinicaoDeJogo")
                .because("PRD secao 44.5: adaptador nao contem regra de jogo")
                .check(TUDO);
    }

    /**
     * Quem garante o selamento de fato é o compilador: se {@code LudoAcao}
     * deixasse de ser {@code sealed}, o {@code switch} sem {@code default} de
     * {@code LudoDefinicao} não compilaria. Esta regra guarda a forma — que a
     * raiz da hierarquia seja uma interface — para que o selamento continue
     * possível.
     */
    @Test
    @DisplayName("8. a raiz de acao e de evento de cada jogo e uma interface")
    void cada_jogo_sela_a_propria_hierarquia() {
        classes()
                .that().haveSimpleNameEndingWith("Acao")
                .and().resideInAPackage("br.com.mesafranca.games..")
                .should().beInterfaces()
                .because("ADR-0002: a exaustividade do switch depende de sealed no jogo")
                .check(TUDO);

        classes()
                .that().haveSimpleNameEndingWith("Evento")
                .and().resideInAPackage("br.com.mesafranca.games..")
                .should().beInterfaces()
                .check(TUDO);
    }
}
