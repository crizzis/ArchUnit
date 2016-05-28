package com.tngtech.archunit.lang.conditions;

import java.io.Serializable;
import java.util.List;

import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containsOnly;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.Arrays.asList;

public class ContainsOnlyConditionTest {
    static final List<SerializableObject> TWO_SERIALIZABLE_OBJECTS = asList(new SerializableObject(), new SerializableObject());
    static final List<Object> ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT = asList(new SerializableObject(), new Object());

    static final AbstractArchCondition<Object> IS_SERIALIZABLE = new AbstractArchCondition<Object>() {
        @Override
        public void check(Object item, ConditionEvents events) {
            boolean satisfied = item instanceof Serializable;
            events.add(new ConditionEvent(satisfied, isSerializableMessageFor(item.getClass())));
        }
    };

    static String isSerializableMessageFor(Class<?> clazz) {
        return String.format("%s is%s serializable", clazz.getSimpleName(), Serializable.class.isAssignableFrom(clazz) ? "" : " not");
    }

    @Test
    public void satisfied_works_and_description_contains_mismatches() {
        ConditionEvents events = new ConditionEvents();
        containsOnly(IS_SERIALIZABLE).check(ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT, events);

        assertThat(events).containViolations(isSerializableMessageFor(Object.class));

        events = new ConditionEvents();
        containsOnly(IS_SERIALIZABLE).check(TWO_SERIALIZABLE_OBJECTS, events);

        assertThat(events).containNoViolation();
    }

    @Test
    public void inverting_works() throws Exception {
        ConditionEvents events = new ConditionEvents();
        containsOnly(IS_SERIALIZABLE).check(TWO_SERIALIZABLE_OBJECTS, events);

        assertThat(events).containNoViolation();
        assertThat(events.getAllowed()).as("Exactly one allowed event occurred").hasSize(1);

        assertThat(getInverted(events)).containViolations(messageForTwoTimes(isSerializableMessageFor(SerializableObject.class)));
    }

    static ConditionEvents getInverted(ConditionEvents events) {
        ConditionEvents inverted = new ConditionEvents();
        for (ConditionEvent event : events) {
            event.addInvertedTo(inverted);
        }
        return inverted;
    }

    static String messageForTwoTimes(String message) {
        return String.format("%s%n%s", message, message);
    }

    static class SerializableObject implements Serializable {
    }
}