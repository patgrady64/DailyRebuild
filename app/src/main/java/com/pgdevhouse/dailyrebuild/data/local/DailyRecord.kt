package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(

    /*
     * ISO date such as 2026-07-24.
     * Each date can have only one daily record.
     */
    @PrimaryKey
    val date: String,

    /*
     * Daily checklist
     */
    val foodRecorded: Boolean = false,
    val walkCompleted: Boolean = false,
    val painRecorded: Boolean = false,
    val mobilityCompleted: Boolean = false,

    /*
     * Pain levels
     */
    val backPain: Float = 0f,
    val shinPain: Float = 0f,

    /*
     * Water entries
     */
    val plainReusableBottleCount: Int = 0,
    val mioReusableBottleCount: Int = 0,
    val plainDisposableBottleCount: Int = 0,
    val mioDisposableBottleCount: Int = 0,

    /*
     * Morning pain relievers
     */
    val morningAspirinTaken: Boolean = true,
    val morningIbuprofenTaken: Boolean = true,
    val morningNaproxenTaken: Boolean = true,
    val morningAcetaminophenTaken: Boolean = true,

    /*
     * Night pain relievers
     */
    val nightIbuprofenTaken: Boolean = true,
    val nightNaproxenTaken: Boolean = true,
    val nightAcetaminophenTaken: Boolean = true,

    /*
     * Editable daily journal
     */
    val journalText: String = "",

    /*
     * Time at which the record was most recently saved.
     */
    val updatedAt: Long = System.currentTimeMillis()
)