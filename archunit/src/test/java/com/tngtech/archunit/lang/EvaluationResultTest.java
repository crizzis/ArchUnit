package com.tngtech.archunit.lang;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.core.domain.properties.HasDescription;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {
    @Test
    public void properties_are_passed_to_FailureReport() {
        EvaluationResult result = new EvaluationResult(
                hasDescription("special description"),
                events("first bummer", "second bummer"),
                Priority.HIGH);

        assertThat(result.getFailureReport().getDetails()).containsExactly("first bummer", "second bummer");
        assertThat(result.getFailureReport().toString())
                .containsPattern("Priority.*HIGH")
                .contains("special description")
                .contains("first bummer")
                .contains("second bummer");
    }

    @Test
    public void allows_clients_to_handle_violations() {
        EvaluationResult result = evaluationResultWith(
                new SimpleConditionEvent(ImmutableSet.of("message"), false, "expected"),
                new SimpleConditionEvent(ImmutableSet.of("other message"), true, "not expected"),
                new SimpleConditionEvent(ImmutableList.of("yet another message"), false, "not expected"),
                new SimpleConditionEvent(ImmutableSet.of("second message"), false, "also expected"));

        final Set<String> actual = new HashSet<>();
        result.handleViolations(new ViolationHandler<Set<?>>() {
            @Override
            public void handle(Set<?> violatingObject, String message) {
                actual.add(Iterables.getOnlyElement(violatingObject) + ": " + message);
            }
        });

        assertThat(actual).containsOnly("message: expected", "second message: also expected");
    }


    private EvaluationResult evaluationResultWith(ConditionEvent... events) {
        return new EvaluationResult(hasDescription("unimportant"), events(events), Priority.MEDIUM);
    }

    private ConditionEvents events(String... messages) {
        Set<ConditionEvent> events = new HashSet<>();
        for (String message : messages) {
            events.add(new SimpleConditionEvent(new Object(), false, message));
        }
        return events(events.toArray(new ConditionEvent[events.size()]));
    }

    private ConditionEvents events(ConditionEvent... events) {
        ConditionEvents result = new ConditionEvents();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }

    private HasDescription hasDescription(final String description) {
        return new HasDescription() {
            @Override
            public String getDescription() {
                return description;
            }
        };
    }
}