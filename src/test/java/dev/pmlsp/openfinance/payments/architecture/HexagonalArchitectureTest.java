package dev.pmlsp.openfinance.payments.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "dev.pmlsp.openfinance.payments",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule layered = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Adapter").definedBy("..adapter..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Adapter");

    @ArchTest
    static final ArchRule noCycles = slices()
            .matching("dev.pmlsp.openfinance.payments.(*)..")
            .should().beFreeOfCycles();
}
