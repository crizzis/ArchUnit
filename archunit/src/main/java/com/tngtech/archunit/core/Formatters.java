package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import com.tngtech.archunit.core.properties.HasName;

public class Formatters {
    private Formatters() {
    }

    public static String formatMethod(String ownerName, String methodName, JavaClassList parameters) {
        return format(ownerName, methodName, formatMethodParameters(parameters));
    }

    private static String format(String ownerName, String methodName, String parameters) {
        return ownerName + "." + methodName + "(" + parameters + ")";
    }

    public static String formatMethodSimple(String ownerName, String methodName, List<String> parameters) {
        List<String> simpleParams = new ArrayList<>();
        for (String parameter : parameters) {
            simpleParams.add(ensureSimpleName(parameter));
        }
        return formatMethod(ensureSimpleName(ownerName), methodName, simpleParams);
    }

    public static String formatMethod(String ownerName, String methodName, List<String> parameters) {
        return format(ownerName, methodName, formatMethodParameterTypeNames(parameters));
    }

    private static String formatMethodParameters(List<? extends HasName> parameters) {
        List<String> names = new ArrayList<>();
        for (HasName type : parameters) {
            names.add(type.getName());
        }
        return formatMethodParameterTypeNames(names);
    }

    public static String formatMethodParameterTypeNames(List<String> typeNames) {
        return Joiner.on(", ").join(typeNames);
    }

    public static String ensureSimpleName(String name) {
        int innerClassStart = name.lastIndexOf('$');
        int classStart = name.lastIndexOf('.');
        if (innerClassStart < 0 && classStart < 0) {
            return name;
        }

        String lastPart = innerClassStart >= 0 ? name.substring(innerClassStart + 1) : name.substring(classStart + 1);
        return isAnonymousRest(lastPart) ? "" : lastPart;
    }

    // NOTE: Anonymous classes (e.g. clazz.getName() == some.Type$1) return an empty clazz.getSimpleName(),
    //       so we mimic this behavior
    private static boolean isAnonymousRest(String lastPart) {
        return Ints.tryParse(lastPart) != null;
    }
}
