/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.filtering.gradle;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Objects;

import com.tngtech.archunit.junit.FieldSource;
import com.tngtech.archunit.junit.filtering.AbstractTestNameFilter;
import com.tngtech.archunit.junit.filtering.DuckTypedProxy;
import com.tngtech.archunit.junit.filtering.TestSelectorFactory.TestSelector;
import org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class GradleTestNameFilter extends AbstractTestNameFilter {
    private static final String GRADLE_DISCOVERY_FILTER_NAME =
            "org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$ClassMethodNameFilter";

    private TestSelectionMatcher matcher;

    public GradleTestNameFilter(EngineDiscoveryRequest request) throws Exception {
        super(request, GRADLE_DISCOVERY_FILTER_NAME);
    }

    public static boolean appliesTo(EngineDiscoveryRequest discoveryRequest) {
        return AbstractTestNameFilter.checkApplicability(discoveryRequest, GRADLE_DISCOVERY_FILTER_NAME);
    }

    @Override
    protected PostDiscoveryFilter initialize(PostDiscoveryFilter filter) throws Exception {
        this.matcher = Objects.requireNonNull(getTestSelectionMatcher(filter));
        /*
        Needed because Gradle's JUnitPlatformTestClassProcessor$ClassMethodNameFilter.shouldRun returns false
        for unrecognized test sources.

        This hack could be avoided if Gradle changed the return value for unrecognized sources to true.
         */
        return object -> object.getSource()
                .filter(source -> source.getClass().getName().equals(FieldSource.class.getName()))
                .map(source -> FilterResult.included("Pre-filtered by ArchUnitTestEngine"))
                .orElseGet(() -> filter.apply(object));
    }

    private TestSelectionMatcher getTestSelectionMatcher(PostDiscoveryFilter filter)
            throws ReflectiveOperationException {
        Field matcher = filter.getClass().getDeclaredField("matcher");
        matcher.setAccessible(true);
        // proxying to resolve classloader conflict
        return DuckTypedProxy.proxying(matcher.get(filter), TestSelectionMatcher.class,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Override
    protected boolean shouldRunAccordingToTestingTool(TestSelector selector) {
        return matcher.matchesTest(selector.getContainerName(), selector.getSelectorName());
    }
}
