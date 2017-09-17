/*
 * Copyright 2017 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.util.LinkedList;

import com.google.common.base.Splitter;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedCall;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedFieldAccess;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.junit.MessageAssertionChain.containsConsecutiveLines;
import static com.tngtech.archunit.junit.MessageAssertionChain.containsLine;
import static com.tngtech.archunit.junit.MessageAssertionChain.matchesLine;
import static java.lang.System.lineSeparator;
import static java.util.regex.Pattern.quote;

public class ExpectedViolation implements TestRule, ExpectsViolations {
    private final MessageAssertionChain assertionChain = new MessageAssertionChain();

    private ExpectedViolation() {
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new ExpectedViolationStatement(base);
    }

    public static ExpectedViolation none() {
        return new ExpectedViolation();
    }

    @Override
    public ExpectedViolation ofRule(String ruleText) {
        LinkedList<String> ruleLines = new LinkedList<>(Splitter.on(lineSeparator()).splitToList(ruleText));
        checkArgument(!ruleLines.isEmpty(), "Rule text may not be empty");
        if (ruleLines.size() == 1) {
            addSingleLineRuleAssertion(getOnlyElement(ruleLines));
        } else {
            addMultiLineRuleAssertion(ruleLines);
        }
        return this;
    }

    private void addSingleLineRuleAssertion(String ruleText) {
        assertionChain.add(matchesLine(String.format(
                "Architecture Violation .* Rule '%s' was violated.*", quote(ruleText))));
    }

    private void addMultiLineRuleAssertion(LinkedList<String> ruleLines) {
        assertionChain.add(matchesLine(String.format(
                "Architecture Violation .* Rule '%s", quote(ruleLines.pollFirst()))));
        assertionChain.add(matchesLine(String.format("%s' was violated.*", quote(ruleLines.pollLast()))));
        assertionChain.add(containsConsecutiveLines(ruleLines));
    }

    @Override
    public ExpectedViolation by(ExpectedFieldAccess access) {
        access.associateLines(toAssertionChain());
        return this;
    }

    @Override
    public ExpectedViolation by(ExpectedCall call) {
        call.associateLines(toAssertionChain());
        return this;
    }

    @Override
    public ExpectsViolations by(ExpectedDependency dependency) {
        dependency.associateLines(toAssertionChain());
        return this;
    }

    private ExpectedRelation.LineAssociation toAssertionChain() {
        return new ExpectedRelation.LineAssociation() {
            @Override
            public void associateIfPatternMatches(String pattern) {
                assertionChain.add(matchesLine(pattern));
            }

            @Override
            public void associateIfStringIsContained(String string) {
                assertionChain.add(containsLine(string));
            }
        };
    }

    @Override
    public ExpectedViolation by(MessageAssertionChain.Link assertion) {
        assertionChain.add(assertion);
        return this;
    }

    public static PackageAssertionCreator javaPackageOf(Class<?> clazz) {
        return new PackageAssertionCreator(clazz);
    }

    @Internal
    public static class PackageAssertionCreator {
        private final Class<?> clazz;

        private PackageAssertionCreator(Class<?> clazz) {
            this.clazz = clazz;
        }

        public MessageAssertionChain.Link notMatching(String packageIdentifier) {
            return containsLine("Class %s doesn't reside in a package '%s'", clazz.getName(), packageIdentifier);
        }
    }

    private class ExpectedViolationStatement extends Statement {
        private final Statement base;

        private ExpectedViolationStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
                throw new NoExpectedViolationException(assertionChain);
            } catch (AssertionError assertionError) {
                assertionChain.evaluate(assertionError);
            }
        }
    }

    private static class NoExpectedViolationException extends RuntimeException {
        private NoExpectedViolationException(MessageAssertionChain assertionChain) {
            super("Rule was not violated in the expected way: Expected " + assertionChain);
        }
    }
}
