package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.DescribedIterable;
import com.tngtech.archunit.core.JavaClasses;

/**
 * A specification of {@link ArchRule} where the set of classes is not known at the time the
 * rule is defined, or where a rule is supposed to be applied to multiple sets of objects.<br><p>
 * <b>Example</b>
 * <pre><code>
 * all(services).should("not access the ui").assertedBy(conditionThatDetectsThis)
 * </code></pre></p>
 *
 * @param <T> The type of objects the rule applies to (e.g. {@link com.tngtech.archunit.core.JavaClass})
 * @see ArchRule
 * @see ClosedArchRule
 */
public final class OpenArchRule<T> extends ArchRule<T> {
    private final Priority priority;
    private final InputTransformer<T> inputTransformer;

    OpenArchRule(OpenDescribable<T> describable, ArchCondition<T> condition) {
        super(condition.getDescription(), condition);
        this.priority = describable.priority;
        this.inputTransformer = describable.inputTransformer;
    }

    public void check(JavaClasses classes) {
        DescribedIterable<T> describedCollection = inputTransformer.transform(classes);
        String completeRuleText = String.format("%s should %s", describedCollection.getDescription(), condition.getDescription());
        ClosedArchRule<?> rule = new ClosedArchRule<>(describedCollection, completeRuleText, condition);
        ArchRuleAssertion.from(rule).assertNoViolations(priority);
    }
}