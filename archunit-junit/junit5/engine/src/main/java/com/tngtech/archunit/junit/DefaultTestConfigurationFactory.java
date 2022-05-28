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
package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitTestDescriptor.ArchUnitArchTestsDescriptor;
import com.tngtech.archunit.junit.ArchUnitTestDescriptor.ArchUnitMethodDescriptor;
import com.tngtech.archunit.junit.ArchUnitTestDescriptor.ArchUnitRuleDescriptor;
import com.tngtech.archunit.junit.ArchUnitTestDescriptor.DeclaredArchTests;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.UniqueId;

import static java.util.Collections.singleton;

class DefaultTestConfigurationFactory
        implements TestDescriptorFactory<ArchUnitEngineExecutionContext, AbstractArchUnitTestDescriptor<ArchUnitEngineExecutionContext, ?>>,
        AnnotationConfigFactory,
        ExecutionContextFactory {

    @Override
    public Collection<Class<? extends Annotation>> getIgnoredAnnotations() {
        return singleton(ArchIgnore.class);
    }

    @Override
    public Collection<Class<? extends Annotation>> getTagAnnotations() {
        return singleton(ArchTag.class);
    }

    @Override
    public ArchUnitEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new ArchUnitEngineExecutionContext();
    }

    @Override
    public ArchUnitArchTestsDescriptor<ArchUnitEngineExecutionContext, AbstractArchUnitTestDescriptor<ArchUnitEngineExecutionContext, ?>> forTestSuite(
            ElementResolver resolver, DeclaredArchTests archTests, Supplier<JavaClasses> classes, Field field) {
        return new ArchUnitArchTestsDescriptor<>(this, this, resolver, archTests, classes, field);
    }

    @Override
    public ArchUnitTestDescriptor<ArchUnitEngineExecutionContext, AbstractArchUnitTestDescriptor<ArchUnitEngineExecutionContext, ?>> forTestClass(ElementResolver resolver, Class<?> testClass, ClassCache classCache) {
        return new ArchUnitTestDescriptor<>(this, this, resolver, testClass, classCache);
    }

    @Override
    public ArchUnitMethodDescriptor<ArchUnitEngineExecutionContext, AbstractArchUnitTestDescriptor<ArchUnitEngineExecutionContext, ?>> forTestMethod(UniqueId uniqueId, Method method, Supplier<JavaClasses> classes) {
        return new ArchUnitMethodDescriptor<>(this, this, uniqueId, method, classes);
    }

    @Override
    public ArchUnitRuleDescriptor<ArchUnitEngineExecutionContext, AbstractArchUnitTestDescriptor<ArchUnitEngineExecutionContext, ?>> forTestField(
            UniqueId uniqueId, ArchRule rule, Supplier<JavaClasses> classes, Field field) {
        return new ArchUnitRuleDescriptor<>(this, this, uniqueId, rule, classes, field);
    }
}
