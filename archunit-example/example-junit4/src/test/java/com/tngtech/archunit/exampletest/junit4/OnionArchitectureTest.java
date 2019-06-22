package com.tngtech.archunit.exampletest.junit4;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@Category(Example.class)
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "com.tngtech.archunit.onionarchitecture.example")
public class OnionArchitectureTest {
    private static final String BASE_PACKAGE = "com.tngtech.archunit.onionarchitecture.example";

    @ArchTest
    static final ArchRule onion_architecture_is_respected = onionArchitecture()
            .domainModel(String.format("%s.domain.model..", BASE_PACKAGE))
            .domainService(String.format("%s.domain.service..", BASE_PACKAGE))
            .application(String.format("%s.application..", BASE_PACKAGE))
            .adapter("cli", String.format("%s.adapter.cli..", BASE_PACKAGE))
            .adapter("persistence", String.format("%s.adapter.persistence..", BASE_PACKAGE))
            .adapter("rest", String.format("%s.adapter.rest..", BASE_PACKAGE));
}
