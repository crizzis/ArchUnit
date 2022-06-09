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
package com.tngtech.archunit.junit.filtering.surefire;

import java.lang.reflect.Field;
import java.util.Objects;

import com.tngtech.archunit.junit.filtering.AbstractTestNameFilter;
import com.tngtech.archunit.junit.filtering.TestSelectorFactory;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;

public class SurefireTestNameFilter extends AbstractTestNameFilter {

    private static final String SUREFIRE_DISCOVERY_FILTER_NAME = "org.apache.maven.surefire.junitplatform.TestMethodFilter";

    private TestListResolver resolver;

    public SurefireTestNameFilter(EngineDiscoveryRequest request) throws Exception {
        super(request, SUREFIRE_DISCOVERY_FILTER_NAME);
    }

    private TestListResolver getTestListResolver(PostDiscoveryFilter filter) throws IllegalAccessException, NoSuchFieldException {
        Field testListResolver = filter.getClass().getDeclaredField("testListResolver");
        testListResolver.setAccessible(true);
        return (TestListResolver) testListResolver.get(filter);
    }

    public static boolean appliesTo(EngineDiscoveryRequest discoveryRequest) {
        return AbstractTestNameFilter.checkApplicability(discoveryRequest, SUREFIRE_DISCOVERY_FILTER_NAME);
    }

    @Override
    protected PostDiscoveryFilter initialize(PostDiscoveryFilter filter) throws Exception {
        this.resolver = Objects.requireNonNull(getTestListResolver(filter));
        return filter;
    }

    @Override
    protected boolean shouldRunAccordingToTestingTool(TestSelectorFactory.TestSelector selector) {
        return resolver.shouldRun(toClassFileName(selector.getContainerName()), selector.getSelectorName());
    }

    static String toClassFileName(String fullyQualifiedTestClass) {
        return fullyQualifiedTestClass.replace('.', '/') + ".class";
    }
}
