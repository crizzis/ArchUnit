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
package com.tngtech.archunit.junit.filtering;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import com.tngtech.archunit.junit.ReflectionUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

@SuppressWarnings("unchecked")
public abstract class DuckTypedProxy {

    public static <T> T proxying(final Object target, Class<T> targetInterface, Object... constructorParams)
            throws ReflectiveOperationException {
        InvocationHandler delegatingHandler = getDelegatingHandler(target);
        if (targetInterface.isInterface()) {
            return jdkProxy(targetInterface, delegatingHandler);
        } else {
            return bytecodeProxy(targetInterface, delegatingHandler, constructorParams);
        }
    }

    private static InvocationHandler getDelegatingHandler(final Object target) {
        return (proxy, method, args) -> ReflectionUtils.invokeMethod(target, method.getName(), args);
    }

    private static <T> T bytecodeProxy(Class<T> targetInterface, InvocationHandler delegatingHandler, Object... constructorParams)
            throws ReflectiveOperationException {
        return (T) new ByteBuddy()
                .subclass(targetInterface)
                .method(ElementMatchers.any())
                .intercept(InvocationHandlerAdapter.of(delegatingHandler))
                .make()
                .load(targetInterface.getClassLoader())
                .getLoaded()
                .getConstructors()[0]
                .newInstance(constructorParams);
    }

    private static <T> T jdkProxy(Class<T> targetInterface, InvocationHandler delegatingHandler) {
        return (T) Proxy.newProxyInstance(
                targetInterface.getClassLoader(),
                new Class<?>[]{targetInterface},
                delegatingHandler);
    }
}
