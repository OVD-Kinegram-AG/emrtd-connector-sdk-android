package com.kinegram.android.emrtdconnectorapp

import com.kinegram.emrtd.DataGroupSet

/**
 * The predefined selections of [DataGroupSet], to pick one before a read.
 */
enum class DataGroupProfile(val filesToRead: DataGroupSet) {
    MINIMAL(DataGroupSet.MINIMAL),
    MINIMAL_KYC(DataGroupSet.MINIMAL_KYC),
    STANDARD(DataGroupSet.STANDARD),
    ALL(DataGroupSet.ALL);

    /**
     * What the SDK itself calls this selection, e.g. `minimalKYC: DG1, DG2`,
     * without the brackets of [DataGroupSet.toString].
     */
    val label: String get() = filesToRead.toString().removeSurrounding("[", "]")

    /**
     * The selection for the `int[]` extra
     * [EmrtdConnectorActivity.DATA_GROUPS][com.kinegram.android.emrtdconnector.EmrtdConnectorActivity.DATA_GROUPS].
     */
    val dataGroupNumbers: IntArray get() = filesToRead.dataGroupNumbers().toIntArray()

    companion object {
        /** The selection a read without a selection uses. */
        val DEFAULT = STANDARD

        /**
         * Resolves a name from preferences, falling back to [DEFAULT] for
         * anything unknown.
         */
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
