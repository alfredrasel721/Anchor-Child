package com.example.service

/**
 * Deterministic child-device restriction policy. Only explicit, parent-synced,
 * acknowledged restrictions are applied; never infer restrictions from content.
 *
 * This pure function is also used by unit tests, separately from Android services.
 */
internal object AppRestrictionPolicy {
    enum class Reason { NONE, PARENT_BLOCK, APP_TIME_LIMIT }

    fun decide(
        status: String,
        blockedByParent: Boolean,
        whitelisted: Boolean,
        scheduleOnly: Boolean,
        dailyLimitMinutes: Int,
        measuredMinutesToday: Int?
    ): Reason {
        if (whitelisted || status == "WHITELISTED") return Reason.NONE
        if (status == "PENDING" || status == "SCHEDULE_ONLY" || scheduleOnly) return Reason.NONE
        if (status != "ENFORCED" && status != "BLOCKED") return Reason.NONE

        if (blockedByParent || status == "BLOCKED") return Reason.PARENT_BLOCK
        if (dailyLimitMinutes > 0 &&
            measuredMinutesToday != null &&
            measuredMinutesToday >= dailyLimitMinutes
        ) return Reason.APP_TIME_LIMIT
        return Reason.NONE
    }
}
