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
package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;
import com.tngtech.archunit.library.dependencies.syntax.GivenNamedSlices;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlicesConjunction;
import com.tngtech.archunit.library.dependencies.syntax.SlicesShould;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.library.dependencies.DependencyRules.slicesShouldNotDependOnEachOtherIn;

class GivenSlicesInternal implements GivenSlices, SlicesShould, GivenSlicesConjunction {
    private final Priority priority;
    private final Slices.Transformer classesTransformer;
    private final PredicateAggregator<Slice> chosenSlices;

    GivenSlicesInternal(Priority priority, Slices.Transformer classesTransformer) {
        this(priority, classesTransformer, new PredicateAggregator<Slice>());
    }

    private GivenSlicesInternal(Priority priority,
                                Slices.Transformer classesTransformer,
                                PredicateAggregator<Slice> chosenSlices) {
        this.classesTransformer = classesTransformer;
        this.priority = priority;
        this.chosenSlices = chosenSlices;
    }

    @Override
    public ArchRule should(ArchCondition<Slice> condition) {
        return ArchRule.Factory.create(finishClassesTransformer(), condition, priority);
    }

    @Override
    public GivenSlicesInternal that(DescribedPredicate<? super Slice> predicate) {
        return givenWith(chosenSlices.add(predicate));
    }

    private GivenSlicesInternal givenWith(PredicateAggregator<Slice> predicate) {
        return new GivenSlicesInternal(priority, classesTransformer, predicate);
    }

    @Override
    public GivenSlicesInternal and(DescribedPredicate<? super Slice> predicate) {
        return givenWith(chosenSlices.thatANDs().add(predicate));
    }

    @Override
    public GivenSlicesInternal or(DescribedPredicate<? super Slice> predicate) {
        return givenWith(chosenSlices.thatORs().add(predicate));
    }

    private Slices.Transformer finishClassesTransformer() {
        return chosenSlices.isPresent() ?
                classesTransformer.that(chosenSlices.get()) :
                classesTransformer;
    }

    @Override
    public GivenSlices as(String newDescription) {
        return new GivenSlicesInternal(priority, classesTransformer.as(newDescription), chosenSlices);
    }

    /**
     * @see Slices#namingSlices(String)
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public GivenNamedSlices namingSlices(String pattern) {
        return new GivenSlicesInternal(priority, classesTransformer.namingSlices(pattern), chosenSlices);
    }

    @Override
    public SlicesShould should() {
        return this;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ArchRule beFreeOfCycles() {
        return should(DependencyRules.beFreeOfCycles());
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public ArchRule notDependOnEachOther() {
        return slicesShouldNotDependOnEachOtherIn(finishClassesTransformer());
    }
}
