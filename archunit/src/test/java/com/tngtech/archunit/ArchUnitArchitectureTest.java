package com.tngtech.archunit;

import java.lang.annotation.Annotation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.MayResolveTypesViaReflection;
import com.tngtech.archunit.core.ResolvesTypesViaReflection;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.origin;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DONT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.has;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchUnitArchitectureTest {
    private static final ClassFileImporter importer = new ClassFileImporter()
            .withImportOption(DONT_INCLUDE_TESTS);

    private static JavaClasses archUnitClasses;

    @BeforeClass
    public static void setUp() {
        archUnitClasses = importer.importPackages(ArchUnitArchitectureTest.class.getPackage().getName());
    }

    @Test
    public void layers_are_respected() {
        layeredArchitecture()
                .layer("Base").definedBy("com.tngtech.archunit.base..")
                .layer("Core").definedBy("com.tngtech.archunit.core..")
                .layer("Lang").definedBy("com.tngtech.archunit.lang..")
                .layer("Library").definedBy("com.tngtech.archunit.library..")

                .whereLayer("Library").mayNotBeAccessedByAnyLayer()
                .whereLayer("Lang").mayOnlyBeAccessedByLayers("Library")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Lang", "Library")
                .whereLayer("Base").mayOnlyBeAccessedByLayers("Core", "Lang", "Library")

                .check(archUnitClasses);
    }

    @Test
    public void domain_does_not_access_importer() {
        noClasses().that().resideInAPackage("..core.domain..")
                // FIXME: Add accessClassesThat(Predicate<JavaClass>) to syntax
                .should(ArchConditions.accessClassesThat(belong_to_the_import_context()))
                .check(archUnitClasses);
    }

    private DescribedPredicate<JavaClass> belong_to_the_import_context() {
        return new DescribedPredicate<JavaClass>("belong to the import context") {
            @Override
            public boolean apply(JavaClass input) {
                return input.getPackage().startsWith(ClassFileImporter.class.getPackage().getName())
                        && !input.getName().contains(DomainBuilders.class.getSimpleName());
            }
        };
    }

    @Test
    public void types_are_only_resolved_via_reflection_in_allowed_places() {
        noClasses().should().callMethodWhere(typeIsIllegallyResolvedViaReflection())
                .as("no classes should illegally resolve classes via reflection")
                .check(archUnitClasses);
    }

    private DescribedPredicate<JavaCall<?>> typeIsIllegallyResolvedViaReflection() {
        DescribedPredicate<JavaCall<?>> explicitlyAllowedUsage =
                origin(is(annotatedWith(MayResolveTypesViaReflection.class)))
                        .or(contextIsAnnotatedWith(MayResolveTypesViaReflection.class)).forSubType();

        return classIsResolvedViaReflection().and(not(explicitlyAllowedUsage));
    }

    private DescribedPredicate<JavaAccess<?>> contextIsAnnotatedWith(final Class<? extends Annotation> annotationType) {
        return origin(With.owner(withAnnotation(annotationType)));
    }

    private DescribedPredicate<JavaClass> withAnnotation(final Class<? extends Annotation> annotationType) {
        return new DescribedPredicate<JavaClass>("annotated with @" + annotationType.getName()) {
            @Override
            public boolean apply(JavaClass input) {
                return input.isAnnotatedWith(annotationType)
                        || enclosingClassIsAnnotated(input);
            }

            private boolean enclosingClassIsAnnotated(JavaClass input) {
                return input.getEnclosingClass().isPresent() &&
                        input.getEnclosingClass().get().isAnnotatedWith(annotationType);
            }
        };
    }

    private DescribedPredicate<JavaCall<?>> classIsResolvedViaReflection() {
        DescribedPredicate<JavaCall<?>> defaultClassForName =
                target(HasOwner.Functions.Get.<JavaClass>owner()
                        .is(equivalentTo(Class.class)))
                        .and(target(has(name("forName"))))
                        .forSubType();
        DescribedPredicate<JavaCall<?>> targetIsMarked =
                annotatedWith(ResolvesTypesViaReflection.class).onResultOf(Get.target());

        return defaultClassForName.or(targetIsMarked);
    }
}
