package com.tngtech.archunit.library.dependencies;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.library.dependencies.syntax.GivenSlices;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class SlicesRuleDefinition {
    private SlicesRuleDefinition() {
    }

    @PublicAPI(usage = ACCESS)
    public static Creator slices() {
        return new Creator();
    }

    public static class Creator {
        private Creator() {
        }

        @PublicAPI(usage = ACCESS)
        public GivenSlices matching(String packageIdentifier) {
            return new GivenSlicesInternal(Priority.MEDIUM, Slices.matching(packageIdentifier));
        }
    }
}
