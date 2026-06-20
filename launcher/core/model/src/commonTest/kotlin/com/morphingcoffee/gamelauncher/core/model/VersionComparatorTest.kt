package com.morphingcoffee.gamelauncher.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {
    @Test
    fun sameMarketing_higherBuildIsNewer() {
        assertTrue(VersionComparator.isLessThan("0.0.1-build48", "0.0.1-build51"))
        assertFalse(VersionComparator.isLessThan("0.0.1-build51", "0.0.1-build51"))
    }

    @Test
    fun higherMarketingWinsRegardlessOfBuild() {
        assertTrue(VersionComparator.isLessThan("0.0.1-build999", "0.0.2-build1"))
    }

    @Test
    fun missingBuildSuffixTreatedAsBuildZero() {
        assertTrue(VersionComparator.isLessThan("0.0.1", "0.0.1-build1"))
    }

    @Test
    fun minimumVersionWithoutBuildSuffix() {
        assertFalse(VersionComparator.isLessThan("0.0.1-build51", "0.0.1"))
    }
}
