package com.tngtech.archunit.integration.junit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.exampletest.junit.CodingRulesWithRunnerMethodsTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.integration.CodingRulesIntegrationTest.expectViolationByUsingJavaUtilLogging;

@AnalyzeClasses(packages = "com.tngtech.archunit.example")
public class CodingRulesWithRunnerMethodsIntegrationTest extends CodingRulesWithRunnerMethodsTest {
    @ArchTest
    public static void no_java_util_logging_as_method(final JavaClasses classes) {
        ExpectedViolation expectViolation = ExpectedViolation.none();
        expectViolationByUsingJavaUtilLogging(expectViolation);

        expectViolation.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                CodingRulesWithRunnerMethodsTest.no_java_util_logging_as_method(classes);
            }
        }, Description.createTestDescription(CodingRulesWithRunnerMethodsIntegrationTest.class, "no_java_util_logging_as_method"));
    }
}
