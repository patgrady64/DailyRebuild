package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MeetingDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeeting(
        meeting: SavedMeeting
    ): Long

    @Update
    suspend fun updateMeeting(
        meeting: SavedMeeting
    )

    @Query(
        "SELECT * FROM saved_meetings " +
            "WHERE active = 1 " +
            "ORDER BY favorite DESC, name COLLATE NOCASE ASC"
    )
    suspend fun getActiveMeetings(): List<SavedMeeting>

    @Query(
        "SELECT * FROM saved_meetings " +
            "WHERE id = :id LIMIT 1"
    )
    suspend fun getMeetingById(
        id: Long
    ): SavedMeeting?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttendance(
        attendance: MeetingAttendance
    ): Long

    @Update
    suspend fun updateAttendance(
        attendance: MeetingAttendance
    )

    @Query(
        "SELECT * FROM meeting_attendance " +
            "ORDER BY startedAt DESC"
    )
    suspend fun getAllAttendance(): List<MeetingAttendance>

    @Query(
        "SELECT * FROM meeting_attendance " +
            "WHERE date BETWEEN :startDate AND :endDate " +
            "ORDER BY startedAt DESC"
    )
    suspend fun getAttendanceBetween(
        startDate: String,
        endDate: String
    ): List<MeetingAttendance>

    @Query(
        "SELECT * FROM meeting_attendance " +
            "WHERE date = :date " +
            "ORDER BY startedAt ASC"
    )
    suspend fun getAttendanceForDate(
        date: String
    ): List<MeetingAttendance>

    @Query(
        "SELECT * FROM meeting_attendance " +
            "WHERE date = :date " +
            "AND meetingName = :meetingName " +
            "AND ABS(startedAt - :startedAt) < 1800000 " +
            "AND id != :excludedId " +
            "LIMIT 1"
    )
    suspend fun findPotentialDuplicate(
        date: String,
        meetingName: String,
        startedAt: Long,
        excludedId: Long = 0L
    ): MeetingAttendance?

    @Query(
        "DELETE FROM meeting_attendance " +
            "WHERE id = :id"
    )
    suspend fun deleteAttendanceById(
        id: Long
    )

    @Query(
        "DELETE FROM meeting_attendance " +
            "WHERE date = :date"
    )
    suspend fun deleteAttendanceByDate(
        date: String
    )
}
