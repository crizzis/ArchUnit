package com.tngtech.archunit.lang.conditions;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.parameterTypes;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.has;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.TARGET_CLASS;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CodeUnitCallConditionTest {

    @DataProvider
    public static Object[][] calls_to_analyze() {
        return $$(
                $(methodCallToTargetFrom(CALLER_CLASS)),
                $(constructorCallToTargetFrom(CALLER_CLASS)));
    }

    @Test
    @UseDataProvider("calls_to_analyze")
    public void condition_is_satisfied_by_matching_call(MethodCallToAnalyze callToAnalyze) {
        CodeUnitCallCondition targetCodeUnitCallCondition = callConditionBuilderMatching(callToAnalyze).build();
        ConditionEvents events = new ConditionEvents();
        targetCodeUnitCallCondition.check(callToAnalyze.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isTrue();
        assertThat(events.getViolating()).isEmpty();
        assertThat(events.getAllowed()).is(containingMessageFor(callToAnalyze));
    }

    @Test
    @UseDataProvider("calls_to_analyze")
    public void condition_is_not_satisfied_on_target_mismatch(MethodCallToAnalyze callToAnalyze) {
        CodeUnitCallCondition targetCodeUnitCallCondition = callConditionBuilderMatching(callToAnalyze)
                .withTarget(getClass())
                .build();
        ConditionEvents events = new ConditionEvents();
        targetCodeUnitCallCondition.check(callToAnalyze.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyze));
        assertThat(events.getAllowed()).isEmpty();
    }

    @Test
    @UseDataProvider("calls_to_analyze")
    public void condition_is_not_satisfied_on_name_mismatch(MethodCallToAnalyze callToAnalyze) {
        CodeUnitCallCondition targetCodeUnitCallCondition = callConditionBuilderMatching(callToAnalyze)
                .withName("wrong")
                .build();
        ConditionEvents events = new ConditionEvents();
        targetCodeUnitCallCondition.check(callToAnalyze.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyze));
        assertThat(events.getAllowed()).isEmpty();
    }

    @Test
    @UseDataProvider("calls_to_analyze")
    public void condition_is_not_satisfied_on_parameter_type_mismatch(MethodCallToAnalyze callToAnalyze) {
        CodeUnitCallCondition targetCodeUnitCallCondition = callConditionBuilderMatching(callToAnalyze)
                .withParameters(getClass())
                .build();
        ConditionEvents events = new ConditionEvents();
        targetCodeUnitCallCondition.check(callToAnalyze.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyze));
        assertThat(events.getAllowed()).isEmpty();
    }

    private MethodCallConditionBuilder callConditionBuilderMatching(MethodCallToAnalyze callToAnalyze) {
        return new MethodCallConditionBuilder(callToAnalyze);
    }

    private static MethodCallToAnalyze methodCallToTargetFrom(JavaClass callerClass) {
        return new MethodCallToAnalyze(callerClass.getMethodCallsFromSelf());
    }

    private static MethodCallToAnalyze constructorCallToTargetFrom(JavaClass callerClass) {
        return new MethodCallToAnalyze(callerClass.getConstructorCallsFromSelf());
    }

    private static Condition<Iterable<? extends ConditionEvent>> containingMessageFor(final MethodCallToAnalyze callToAnalyze) {
        final String originName = callToAnalyze.call.getOrigin().getFullName();
        final String targetName = callToAnalyze.call.getTarget().getFullName();
        final String ideJumpHook = ideJumpHookFor(callToAnalyze);

        return new Condition<Iterable<? extends ConditionEvent>>() {
            @Override
            public boolean matches(Iterable<? extends ConditionEvent> value) {
                boolean matches = false;
                for (ConditionEvent event : value) {
                    matches = matches || eventHasCorrectDetails(event);
                }
                return matches;
            }

            private boolean eventHasCorrectDetails(ConditionEvent event) {
                return event.toString().contains(originName)
                        && event.toString().contains(targetName)
                        && event.toString().contains(ideJumpHook);
            }
        }.as(String.format("Event from call with origin %s, target %s and IDE Hook %s", originName, targetName, ideJumpHook));
    }

    private static String ideJumpHookFor(MethodCallToAnalyze callToAnalyze) {
        String simpleCallerName = callToAnalyze.call.getOriginOwner().getSimpleName();
        int lineNumber = callToAnalyze.call.getLineNumber();
        return String.format("(%s.java:%d)", simpleCallerName, lineNumber);
    }

    private static class MethodCallToAnalyze {
        private final JavaCall<?> call;

        private MethodCallToAnalyze(Collection<? extends JavaCall<?>> calls) {
            call = callToTargetIn(calls);
        }

        private JavaCall<?> callToTargetIn(Collection<? extends JavaCall<?>> calls) {
            for (JavaCall<?> call : calls) {
                if (call.getTarget().getOwner().equals(TARGET_CLASS)) {
                    return call;
                }
            }
            throw new RuntimeException("Couldn't find any matching call to " + TARGET_CLASS + " in " + calls);
        }

        @Override
        public String toString() {
            return call.getDescription();
        }
    }

    private static class MethodCallConditionBuilder {
        private Class<?> targetClass;
        private String methodName;
        private List<String> paramTypes;

        private MethodCallConditionBuilder(MethodCallToAnalyze callToAnalyze) {
            targetClass = callToAnalyze.call.getTarget().getOwner().reflect();
            methodName = callToAnalyze.call.getTarget().getName();
            paramTypes = callToAnalyze.call.getTarget().getParameters().getNames();
        }

        private MethodCallConditionBuilder withTarget(Class<?> targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public MethodCallConditionBuilder withName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public MethodCallConditionBuilder withParameters(Class<?>... paramTypes) {
            this.paramTypes = namesOf(ImmutableList.copyOf(paramTypes));
            return this;
        }

        private CodeUnitCallCondition build() {
            DescribedPredicate<JavaCall<?>> ownerAndNameMatch = targetOwner(is(equivalentTo(targetClass)))
                    .and(target(has(name(methodName)))).forSubType();
            return new CodeUnitCallCondition(ownerAndNameMatch.and(target(has(parameterTypes(paramTypes)))));
        }
    }
}