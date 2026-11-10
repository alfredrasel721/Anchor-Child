package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRestrictionPolicyTest {
    @Test fun parentBlockIsAppliedOnlyWhenAcknowledged() {
        assertEquals(AppRestrictionPolicy.Reason.PARENT_BLOCK,
            AppRestrictionPolicy.decide("ENFORCED", true, false, false, 60, null))
        assertEquals(AppRestrictionPolicy.Reason.NONE,
            AppRestrictionPolicy.decide("PENDING", true, false, false, 60, 100))
    }

    @Test fun allowedAppsAndScheduleOnlyRulesAreNotBlocked() {
        assertEquals(AppRestrictionPolicy.Reason.NONE,
            AppRestrictionPolicy.decide("BLOCKED", true, true, false, 5, 60))
        assertEquals(AppRestrictionPolicy.Reason.NONE,
            AppRestrictionPolicy.decide("SCHEDULE_ONLY", true, false, true, 5, 60))
    }

    @Test fun perAppLimitRequiresActualUsagePermissionEvidence() {
        assertEquals(AppRestrictionPolicy.Reason.NONE,
            AppRestrictionPolicy.decide("ENFORCED", false, false, false, 30, null))
        assertEquals(AppRestrictionPolicy.Reason.NONE,
            AppRestrictionPolicy.decide("ENFORCED", false, false, false, 30, 29))
        assertEquals(AppRestrictionPolicy.Reason.APP_TIME_LIMIT,
            AppRestrictionPolicy.decide("ENFORCED", false, false, false, 30, 30))
    }
}
