package br.com.mesafranca.core.arquitetura;

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
 * Fronteiras do core verificadas no build (RNF-13).
 *
 * <p>Estas regras existem porque disciplina que depende de memoria humana nao
 * e disciplina. Cada uma corresponde a uma proibicao do PRD secao 44.5 e da
 * ADR-0001, e quebrar qualquer uma reprova o build antes de virar defeito.
 *
 * <p>O conjunto completo, cobrindo tambem adaptadores e jogos, roda em
 * {@code bootstrap}. Este roda aqui para falhar cedo.
 */
@DisplayName("Arquitetura do core")
class ArquiteturaDoCoreTest {

    private static final JavaClasses CORE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.mesafranca.core");

    @Test
    @DisplayName("o core nao conhece framework, ORM, serializacao nem rede")
    void core_nao_depende_de_infraestrutura() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "javax.persistence..",
                        "com.fasterxml..",
                        "java.net..",
                        "java.sql..",
                        "javax.sql..")
                .because("ADR-0001: a dependencia aponta para dentro; "
                        + "infraestrutura fica nos adaptadores")
                .check(CORE);
    }

    @Test
    @DisplayName("o core nao conhece jogo especifico")
    void core_nao_depende_de_jogo() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.com.mesafranca.games..",
                        "br.com.mesafranca.adapter..")
                .because("RNF-50: acrescentar um jogo nao pode exigir alterar o core")
                .check(CORE);
    }

    @Test
    @DisplayName("acaso e tempo entram por porta, nunca por chamada direta")
    void core_nao_sorteia_nem_le_o_relogio() {
        noClasses()
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
                .check(CORE);
    }

    @Test
    @DisplayName("toda porta de saida e uma interface")
    void portas_de_saida_sao_interfaces() {
        classes()
                .that().resideInAPackage("br.com.mesafranca.core.port.out..")
                .should().beInterfaces()
                .because("uma porta com implementacao deixa de ser ponto de troca")
                .check(CORE);
    }

    @Test
    @DisplayName("o dominio nao tem campo mutavel")
    void dominio_e_imutavel() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("br.com.mesafranca.core.domain..")
                .and().areNotStatic()
                .should().beFinal()
                .because("PRD secao 44.4: aplicar devolve novo estado, nao muta o antigo")
                .check(CORE);
    }

    @Test
    @DisplayName("dominio, portas e SPI nao dependem da camada de aplicacao")
    void a_dependencia_aponta_para_dentro() {
        noClasses()
                .that().resideInAnyPackage(
                        "br.com.mesafranca.core.domain..",
                        "br.com.mesafranca.core.spi..",
                        "br.com.mesafranca.core.port..")
                .should().dependOnClassesThat()
                .resideInAPackage("br.com.mesafranca.core.application..")
                .because("casos de uso orquestram o dominio; o dominio nao os conhece")
                .check(CORE);
    }
}
