package com.pgdevhouse.dailyrebuild.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_goal_changes")
data class CalorieGoalChange(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val changedDate: String,
    val previousGoal: Int? = null,
    val newGoal: Int,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
