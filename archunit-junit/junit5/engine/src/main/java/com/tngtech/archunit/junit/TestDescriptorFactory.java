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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;

public interface TestDescriptorFactory<
        CONTEXT_TYPE extends EngineExecutionContext,
        PRODUCED_DESCRIPTOR_SUPERTYPE extends TestDescriptor & Node<CONTEXT_TYPE> & CreatesChildren> {

    PRODUCED_DESCRIPTOR_SUPERTYPE forTestSuite(
            ElementResolver resolver,
            ArchUnitTestDescriptor.DeclaredArchTests archTests,
            Supplier<JavaClasses> classes,
            Field field);

    PRODUCED_DESCRIPTOR_SUPERTYPE forTestClass(
            ElementResolver resolver,
            Class<?> testClass,
            ClassCache classCache
    );

    PRODUCED_DESCRIPTOR_SUPERTYPE forTestMethod(
            UniqueId uniqueId,
            Method method,
            Supplier<JavaClasses> classes
    );

    PRODUCED_DESCRIPTOR_SUPERTYPE forTestField(
            UniqueId uniqueId,
            ArchRule rule,
            Supplier<JavaClasses> classes,
            Field field
    );

}
