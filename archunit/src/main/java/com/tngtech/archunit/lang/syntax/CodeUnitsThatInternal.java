/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.syntax;

import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.syntax.elements.CodeUnitsThat;

import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Predicates.rawReturnType;
import static com.tngtech.archunit.core.domain.properties.HasThrowsClause.Predicates.throwsClauseContainingType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;

class CodeUnitsThatInternal<
        CODE_UNIT extends JavaCodeUnit,
        CONJUNCTION extends AbstractGivenCodeUnitsInternal<CODE_UNIT, CONJUNCTION>
        >
        extends MembersThatInternal<CODE_UNIT, CONJUNCTION>
        implements CodeUnitsThat<CONJUNCTION> {

    CodeUnitsThatInternal(CONJUNCTION givenCodeUnits, PredicateAggregator<CODE_UNIT> currentPredicate) {
        super(givenCodeUnits, currentPredicate);
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(Class<?>... parameterTypes) {
        return withPredicate(have(rawParameterTypes(parameterTypes)));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(String... parameterTypeNames) {
        return withPredicate(have(rawParameterTypes(parameterTypeNames)));
    }

    @Override
    public CONJUNCTION haveRawParameterTypes(DescribedPredicate<List<JavaClass>> predicate) {
        return withPredicate(have(rawParameterTypes(predicate)));
    }

    @Override
    public CONJUNCTION haveRawReturnType(Class<?> type) {
        return withPredicate(have(rawReturnType(type)));

    }

    @Override
    public CONJUNCTION haveRawReturnType(String typeName) {
        return withPredicate(have(rawReturnType(typeName)));

    }

    @Override
    public CONJUNCTION haveRawReturnType(DescribedPredicate<JavaClass> predicate) {
        return withPredicate(have(rawReturnType(predicate)));

    }

    @Override
    public CONJUNCTION declareThrowableOfType(Class<? extends Throwable> type) {
        return withPredicate(throwsClauseContainingType(type).as("declare throwable of type " + type.getName()));
    }

    @Override
    public CONJUNCTION declareThrowableOfType(String typeName) {
        return withPredicate(throwsClauseContainingType(typeName).as("declare throwable of type " + typeName));
    }

    @Override
    public CONJUNCTION declareThrowableOfType(DescribedPredicate<JavaClass> predicate) {
        return withPredicate(throwsClauseContainingType(predicate).as("declare throwable of type " + predicate.getDescription()));
    }

    private CONJUNCTION withPredicate(DescribedPredicate<? super CODE_UNIT> predicate) {
        return givenMembers.with(currentPredicate.add(predicate));
    }
}
