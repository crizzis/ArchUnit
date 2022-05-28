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
package com.tngtech.archunit.junit.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AbstractArchUnitTestDescriptor;
import com.tngtech.archunit.junit.AnnotationConfigFactory;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchUnitEngineExecutionContext;
import com.tngtech.archunit.junit.ArchUnitTestDescriptor;
import com.tngtech.archunit.junit.ClassCache;
import com.tngtech.archunit.junit.ElementResolver;
import com.tngtech.archunit.junit.ExecutionContextFactory;
import com.tngtech.archunit.junit.TestDescriptorFactory;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.UniqueId;

import static java.util.Arrays.asList;

class ExtensibleTestConfigurationFactory
        implements TestDescriptorFactory<
        ExtensibleArchUnitEngineExecutionContext,
        ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>
        >,
        AnnotationConfigFactory,
        ExecutionContextFactory {

    @Override
    public Collection<Class<? extends Annotation>> getIgnoredAnnotations() {
        return asList(ArchIgnore.class, Disabled.class);
    }

    @Override
    public Collection<Class<? extends Annotation>> getTagAnnotations() {
        return asList(ArchTag.class, Tag.class);
    }

    @Override
    public ArchUnitEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new ExtensibleArchUnitEngineExecutionContext(request.getEngineExecutionListener(), request.getConfigurationParameters());
    }

    @Override
    public ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>> forTestSuite(
            ElementResolver resolver, ArchUnitTestDescriptor.DeclaredArchTests archTests, Supplier<JavaClasses> classes, Field field) {
        return new ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>(
                new ArchUnitTestDescriptor.ArchUnitArchTestsDescriptor(this, this, resolver, archTests, classes, field));
    }

    @Override
    public ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>> forTestClass(
            ElementResolver resolver, Class<?> testClass, ClassCache classCache) {
        return new ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>(
                new ArchUnitTestDescriptor(this, this, resolver, testClass, classCache));
    }

    @Override
    public ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>> forTestMethod(
            UniqueId uniqueId, Method method, Supplier<JavaClasses> classes) {
        return new ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>(
                new ArchUnitTestDescriptor.ArchUnitMethodDescriptor(this, this, uniqueId, method, classes));
    }

    @Override
    public ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>> forTestField(
            UniqueId uniqueId, ArchRule rule, Supplier<JavaClasses> classes, Field field) {
        return new ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>(
                new ArchUnitTestDescriptor.ArchUnitRuleDescriptor(this, this, uniqueId, rule, classes, field));
    }
}
