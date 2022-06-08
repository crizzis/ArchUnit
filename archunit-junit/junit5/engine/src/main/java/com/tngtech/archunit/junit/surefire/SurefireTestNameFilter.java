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
package com.tngtech.archunit.junit.surefire;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import com.tngtech.archunit.junit.TestSourceFilter;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class SurefireTestNameFilter implements TestSourceFilter {

    private static final Collection<TestSelectorFactory> SELECTOR_FACTORIES = Arrays.asList(
            new MethodSelectorFactory(),
            new FieldSelectorFactory()
    );

    private final TestListResolver resolver;

    public SurefireTestNameFilter(EngineDiscoveryRequest request) throws NoSuchFieldException, IllegalAccessException {
        this.resolver = getTestListResolver(request);
    }

    private TestListResolver getTestListResolver(EngineDiscoveryRequest request) throws IllegalAccessException, NoSuchFieldException {
        PostDiscoveryFilter filter = ((LauncherDiscoveryRequest) request).getPostDiscoveryFilters().stream()
                .filter(SurefireTestNameFilter::findSurefireFilter)
                .findAny().get();
        Field testListResolver = filter.getClass().getDeclaredField("testListResolver");
        testListResolver.setAccessible(true);
        return (TestListResolver) testListResolver.get(filter);
    }

    public static boolean appliesTo(EngineDiscoveryRequest discoveryRequest) {
        if (!(discoveryRequest instanceof LauncherDiscoveryRequest)) {
            return false;
        }
        LauncherDiscoveryRequest request = (LauncherDiscoveryRequest) discoveryRequest;
        return request.getPostDiscoveryFilters().stream()
                .anyMatch(SurefireTestNameFilter::findSurefireFilter);
    }

    private static boolean findSurefireFilter(PostDiscoveryFilter filter) {
        return "org.apache.maven.surefire.junitplatform.TestMethodFilter".equals(filter.getClass().getName());
    }

    public boolean shouldRun(TestSource source) {
        return resolveFactory(source)
                .map(factory -> factory.createSelector(source))
                .map(selector -> resolver.shouldRun(toClassFileName(selector.getContainerName()), selector.getSelectorName()))
                .orElse(true);
    }

    private Optional<TestSelectorFactory> resolveFactory(TestSource source) {
        return SELECTOR_FACTORIES.stream()
                .filter(factory -> factory.supports(source))
                .findAny();
    }

    static String toClassFileName(String fullyQualifiedTestClass) {
        return fullyQualifiedTestClass.replace('.', '/') + ".class";
    }
}
