package com.pgdevhouse.dailyrebuild.data.repository

import androidx.room.withTransaction
import com.pgdevhouse.dailyrebuild.data.local.CalorieGoalChange
import com.pgdevhouse.dailyrebuild.data.local.CareAppointment
import com.pgdevhouse.dailyrebuild.data.local.CarePlace
import com.pgdevhouse.dailyrebuild.data.local.CareProvider
import com.pgdevhouse.dailyrebuild.data.local.CareVisit
import com.pgdevhouse.dailyrebuild.data.local.DailyActivitySnapshot
import com.pgdevhouse.dailyrebuild.data.local.DailyRebuildDatabase
import com.pgdevhouse.dailyrebuild.data.local.DailyRecord
import com.pgdevhouse.dailyrebuild.data.local.FoodLogEntry
import com.pgdevhouse.dailyrebuild.data.local.FoodProduct
import com.pgdevhouse.dailyrebuild.data.local.HealthMeasurement
import com.pgdevhouse.dailyrebuild.data.local.HealthProfile
import com.pgdevhouse.dailyrebuild.data.local.IopGroup
import com.pgdevhouse.dailyrebuild.data.local.MeetingAttendance
import com.pgdevhouse.dailyrebuild.data.local.LifeMaintenanceLog
import com.pgdevhouse.dailyrebuild.data.local.MedicationEntry
import com.pgdevhouse.dailyrebuild.data.local.MigraineLog
import com.pgdevhouse.dailyrebuild.data.local.MobilitySession
import com.pgdevhouse.dailyrebuild.data.local.PantryEssential
import com.pgdevhouse.dailyrebuild.data.local.SavedMeal
import com.pgdevhouse.dailyrebuild.data.local.SavedMealIngredient
import com.pgdevhouse.dailyrebuild.data.local.SavedMealWithIngredients
import com.pgdevhouse.dailyrebuild.data.local.SavedMeeting
import com.pgdevhouse.dailyrebuild.data.local.ShowerLog

/**
 * Dependency boundary between feature code and Room.
 *
 * The methods intentionally resemble the old DAO calls so Phase 2 can move
 * database access out of the app shell without changing user-visible behavior.
 */
class DailyRebuildRepositories private constructor(
    val dailyRecords: DailyRecordRepository,
    val food: FoodRepository,
    val meals: MealRepository,
    val activity: DailyActivityRepository,
    val mobility: MobilityRepository,
    val showers: ShowerRepository,
    val migraines: MigraineRepository,
    val meetings: MeetingRepository,
    val careVisits: CareVisitRepository,
    val appointments: CareAppointmentRepository,
    val pantry: PantryRepository,
    val healthProfile: HealthProfileRepository,
    val lifeMaintenance: LifeMaintenanceRepository,
    val iopGroups: IopGroupRepository,
    val history: HistoryRepository
) {
    companion object {
        fun create(database: DailyRebuildDatabase): DailyRebuildRepositories {
            return DailyRebuildRepositories(
                dailyRecords = DailyRecordRepository(database),
                food = FoodRepository(database),
                meals = MealRepository(database),
                activity = DailyActivityRepository(database),
                mobility = MobilityRepository(database),
                showers = ShowerRepository(database),
                migraines = MigraineRepository(database),
                meetings = MeetingRepository(database),
                careVisits = CareVisitRepository(database),
                appointments = CareAppointmentRepository(database),
                pantry = PantryRepository(database),
                healthProfile = HealthProfileRepository(database),
                lifeMaintenance = LifeMaintenanceRepository(database),
                iopGroups = IopGroupRepository(database),
                history = HistoryRepository(database)
            )
        }
    }
}

class DailyRecordRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.dailyRecordDao()
    suspend fun getRecordByDate(date: String): DailyRecord? = dao.getRecordByDate(date)
    suspend fun getAllRecords(): List<DailyRecord> = dao.getAllRecords()
    suspend fun saveRecord(record: DailyRecord) = dao.saveRecord(record)
    suspend fun deleteRecord(record: DailyRecord) = dao.deleteRecord(record)
}

class FoodRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.foodDao()
    suspend fun saveProduct(product: FoodProduct) = dao.saveProduct(product)
    suspend fun addProduct(product: FoodProduct): Long = dao.addProduct(product)
    suspend fun updateProduct(product: FoodProduct) = dao.updateProduct(product)
    suspend fun getProductById(productId: Long): FoodProduct? = dao.getProductById(productId)
    suspend fun getProductByBarcode(barcode: String): FoodProduct? = dao.getProductByBarcode(barcode)
    suspend fun getAllProducts(): List<FoodProduct> = dao.getAllProducts()
    suspend fun getFavoriteProducts(): List<FoodProduct> = dao.getFavoriteProducts()
    suspend fun addFoodEntry(entry: FoodLogEntry): Long = dao.addFoodEntry(entry)
    suspend fun updateFoodEntry(entry: FoodLogEntry): Int = dao.updateFoodEntry(entry)

    suspend fun findMergeableIndividualEntry(
        date: String,
        productId: Long,
        productNameSnapshot: String,
        unit: String,
        mealName: String?
    ): FoodLogEntry? = dao.findMergeableIndividualEntry(
        date = date,
        productId = productId,
        productNameSnapshot = productNameSnapshot,
        unit = unit,
        mealName = mealName
    )

    suspend fun getEntriesForDate(date: String): List<FoodLogEntry> = dao.getEntriesForDate(date)
    suspend fun getEntriesBetween(startDate: String, endDate: String): List<FoodLogEntry> =
        dao.getEntriesBetween(startDate, endDate)
    suspend fun getAllEntries(): List<FoodLogEntry> = dao.getAllEntries()
    suspend fun deleteFoodEntry(entry: FoodLogEntry) = dao.deleteFoodEntry(entry)
    suspend fun deleteFoodEntryById(entryId: Long) = dao.deleteFoodEntryById(entryId)
    suspend fun deleteProductById(productId: Long) = dao.deleteProductById(productId)
    suspend fun deleteFoodEntriesByMealLogId(mealLogId: String) = dao.deleteFoodEntriesByMealLogId(mealLogId)
    suspend fun deleteEntriesForDate(date: String) = dao.deleteEntriesForDate(date)
}

class MealRepository internal constructor(database: DailyRebuildDatabase) {
    private val databaseRef = database
    private val dao = database.mealDao()
    suspend fun addMeal(meal: SavedMeal): Long = dao.addMeal(meal)
    suspend fun updateMeal(meal: SavedMeal) = dao.updateMeal(meal)
    suspend fun deleteMeal(mealId: Long) = dao.deleteMeal(mealId)
    suspend fun addIngredients(ingredients: List<SavedMealIngredient>) = dao.addIngredients(ingredients)
    suspend fun deleteIngredientsForMeal(mealId: Long) = dao.deleteIngredientsForMeal(mealId)
    suspend fun getAllMealsWithIngredients(): List<SavedMealWithIngredients> = dao.getAllMealsWithIngredients()

    suspend fun saveMealWithIngredients(
        meal: SavedMeal,
        ingredients: List<SavedMealIngredient>
    ): Long = databaseRef.withTransaction {
        val mealId = if (meal.id == 0L) {
            dao.addMeal(meal)
        } else {
            dao.updateMeal(meal)
            dao.deleteIngredientsForMeal(meal.id)
            meal.id
        }
        dao.addIngredients(ingredients.map { it.copy(mealId = mealId) })
        mealId
    }
}

class DailyActivityRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.dailyActivityDao()
    suspend fun saveSnapshot(snapshot: DailyActivitySnapshot) = dao.saveSnapshot(snapshot)
    suspend fun getSnapshotByDate(date: String): DailyActivitySnapshot? = dao.getSnapshotByDate(date)
    suspend fun getSnapshotsBetween(startDate: String, endDate: String): List<DailyActivitySnapshot> =
        dao.getSnapshotsBetween(startDate, endDate)
    suspend fun getAllSnapshots(): List<DailyActivitySnapshot> = dao.getAllSnapshots()
    suspend fun deleteSnapshot(snapshot: DailyActivitySnapshot) = dao.deleteSnapshot(snapshot)
}

class MobilityRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.mobilitySessionDao()
    suspend fun addSession(session: MobilitySession): Long = dao.addSession(session)
    suspend fun getSessionsForDate(date: String): List<MobilitySession> = dao.getSessionsForDate(date)
    suspend fun getSessionsBetween(startDate: String, endDate: String): List<MobilitySession> =
        dao.getSessionsBetween(startDate, endDate)
    suspend fun getAllSessions(): List<MobilitySession> = dao.getAllSessions()
    suspend fun deleteSession(session: MobilitySession) = dao.deleteSession(session)
    suspend fun deleteSessionsForDate(date: String) = dao.deleteSessionsForDate(date)
}

class ShowerRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.showerLogDao()
    suspend fun save(log: ShowerLog) = dao.save(log)
    suspend fun getLogByDate(date: String): ShowerLog? = dao.getLogByDate(date)
    suspend fun getLogsBetween(startDate: String, endDate: String): List<ShowerLog> =
        dao.getLogsBetween(startDate, endDate)
    suspend fun getAllLogs(): List<ShowerLog> = dao.getAllLogs()
    suspend fun deleteByDate(date: String) = dao.deleteByDate(date)
}

class MigraineRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.migraineLogDao()
    suspend fun save(log: MigraineLog): Long = dao.save(log)
    suspend fun getAllLogs(): List<MigraineLog> = dao.getAllLogs()
    suspend fun getLogsForDate(date: String): List<MigraineLog> = dao.getLogsForDate(date)
    suspend fun getLogsBetween(startDate: String, endDate: String): List<MigraineLog> =
        dao.getLogsBetween(startDate, endDate)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun deleteByDate(date: String) = dao.deleteByDate(date)
}

class MeetingRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.meetingDao()
    suspend fun insertMeeting(meeting: SavedMeeting): Long = dao.insertMeeting(meeting)
    suspend fun updateMeeting(meeting: SavedMeeting) = dao.updateMeeting(meeting)
    suspend fun getActiveMeetings(): List<SavedMeeting> = dao.getActiveMeetings()
    suspend fun getMeetingById(id: Long): SavedMeeting? = dao.getMeetingById(id)
    suspend fun insertAttendance(attendance: MeetingAttendance): Long = dao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: MeetingAttendance) = dao.updateAttendance(attendance)
    suspend fun getAllAttendance(): List<MeetingAttendance> = dao.getAllAttendance()
    suspend fun getAttendanceBetween(startDate: String, endDate: String): List<MeetingAttendance> =
        dao.getAttendanceBetween(startDate, endDate)
    suspend fun getAttendanceForDate(date: String): List<MeetingAttendance> = dao.getAttendanceForDate(date)
    suspend fun findPotentialDuplicate(
        date: String,
        meetingName: String,
        startedAt: Long,
        excludedId: Long = 0L
    ): MeetingAttendance? = dao.findPotentialDuplicate(date, meetingName, startedAt, excludedId)
    suspend fun deleteAttendanceById(id: Long) = dao.deleteAttendanceById(id)
    suspend fun deleteAttendanceByDate(date: String) = dao.deleteAttendanceByDate(date)
}

class CareVisitRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.careVisitDao()
    suspend fun insertPlace(place: CarePlace): Long = dao.insertPlace(place)
    suspend fun updatePlace(place: CarePlace) = dao.updatePlace(place)
    suspend fun getActivePlaces(): List<CarePlace> = dao.getActivePlaces()
    suspend fun getPlaceById(id: Long): CarePlace? = dao.getPlaceById(id)
    suspend fun insertProvider(provider: CareProvider): Long = dao.insertProvider(provider)
    suspend fun updateProvider(provider: CareProvider) = dao.updateProvider(provider)
    suspend fun getActiveProviders(): List<CareProvider> = dao.getActiveProviders()
    suspend fun getProvidersForPlace(placeId: Long): List<CareProvider> = dao.getProvidersForPlace(placeId)
    suspend fun getProviderById(id: Long): CareProvider? = dao.getProviderById(id)
    suspend fun insertVisit(visit: CareVisit): Long = dao.insertVisit(visit)
    suspend fun updateVisit(visit: CareVisit) = dao.updateVisit(visit)
    suspend fun getAllVisits(): List<CareVisit> = dao.getAllVisits()
    suspend fun getVisitsBetween(startDate: String, endDate: String): List<CareVisit> =
        dao.getVisitsBetween(startDate, endDate)
    suspend fun getVisitsForDate(date: String): List<CareVisit> = dao.getVisitsForDate(date)
    suspend fun findPotentialDuplicate(
        date: String,
        placeName: String,
        providerName: String,
        startedAt: Long,
        excludedId: Long = 0L
    ): CareVisit? = dao.findPotentialDuplicate(date, placeName, providerName, startedAt, excludedId)
    suspend fun deleteVisitById(id: Long) = dao.deleteVisitById(id)
    suspend fun deleteVisitsByDate(date: String) = dao.deleteVisitsByDate(date)
}

class CareAppointmentRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.careAppointmentDao()
    suspend fun insertAppointment(appointment: CareAppointment): Long = dao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: CareAppointment) = dao.updateAppointment(appointment)
    suspend fun getAllAppointments(): List<CareAppointment> = dao.getAllAppointments()
    suspend fun getUpcomingAppointments(now: Long): List<CareAppointment> = dao.getUpcomingAppointments(now)
    suspend fun getAppointmentById(id: Long): CareAppointment? = dao.getAppointmentById(id)
    suspend fun getAppointmentsForDate(date: String): List<CareAppointment> = dao.getAppointmentsForDate(date)
    suspend fun findPotentialDuplicate(
        date: String,
        placeName: String,
        providerName: String,
        scheduledAt: Long,
        excludedId: Long = 0L
    ): CareAppointment? = dao.findPotentialDuplicate(date, placeName, providerName, scheduledAt, excludedId)
    suspend fun clearConvertedVisitLink(
        visitId: Long,
        updatedAt: Long
    ) = dao.clearConvertedVisitLink(visitId, updatedAt)
    suspend fun deleteAppointmentById(id: Long) = dao.deleteAppointmentById(id)
    suspend fun deleteAppointmentsByDate(date: String) = dao.deleteAppointmentsByDate(date)
}

class PantryRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.pantryEssentialDao()
    suspend fun getAll(): List<PantryEssential> = dao.getAll()
    suspend fun getNeeded(): List<PantryEssential> = dao.getNeeded()
    suspend fun insert(item: PantryEssential): Long = dao.insert(item)
    suspend fun update(item: PantryEssential) = dao.update(item)
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long) =
        dao.updateStatus(id, status, updatedAt)
    suspend fun markAllNeededAsHave(updatedAt: Long) = dao.markAllNeededAsHave(updatedAt)
    suspend fun delete(item: PantryEssential) = dao.delete(item)
}

class LifeMaintenanceRepository internal constructor(
    private val database: DailyRebuildDatabase
) {
    private val dao = database.lifeMaintenanceDao()

    suspend fun save(taskKey: String, date: String) {
        dao.save(
            LifeMaintenanceLog(
                taskKey = taskKey,
                date = date
            )
        )
    }

    suspend fun getAllLogs(): List<LifeMaintenanceLog> = dao.getAllLogs()

    suspend fun getLogsForDate(date: String): List<LifeMaintenanceLog> =
        dao.getLogsForDate(date)

    suspend fun move(log: LifeMaintenanceLog, newDate: String) =
        database.withTransaction {
            dao.delete(log.taskKey, log.date)
            dao.save(log.copy(date = newDate))
        }

    suspend fun delete(log: LifeMaintenanceLog) =
        dao.delete(log.taskKey, log.date)

    suspend fun deleteByDate(date: String) = dao.deleteByDate(date)
}

class IopGroupRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.iopGroupDao()
    suspend fun insert(group: IopGroup): Long = dao.insert(group)
    suspend fun insertAll(groups: List<IopGroup>): List<Long> = dao.insertAll(groups)
    suspend fun update(group: IopGroup) = dao.update(group)
    suspend fun delete(group: IopGroup) = dao.delete(group)
    suspend fun getAll(): List<IopGroup> = dao.getAll()
    suspend fun getActive(): List<IopGroup> = dao.getActive()
}

class HealthProfileRepository internal constructor(database: DailyRebuildDatabase) {
    private val dao = database.healthProfileDao()
    suspend fun getProfile(): HealthProfile? = dao.getProfile()
    suspend fun saveProfile(profile: HealthProfile) = dao.saveProfile(profile)
    suspend fun getAllMeasurements(): List<HealthMeasurement> = dao.getAllMeasurements()
    suspend fun getMeasurementsByType(type: String): List<HealthMeasurement> = dao.getMeasurementsByType(type)
    suspend fun addMeasurement(measurement: HealthMeasurement): Long = dao.addMeasurement(measurement)
    suspend fun deleteMeasurement(measurement: HealthMeasurement) = dao.deleteMeasurement(measurement)
    suspend fun getMedications(): List<MedicationEntry> = dao.getMedications()
    suspend fun countMedications(): Int = dao.countMedications()
    suspend fun saveMedication(medication: MedicationEntry): Long = dao.saveMedication(medication)
    suspend fun saveMedications(medications: List<MedicationEntry>) = dao.saveMedications(medications)
    suspend fun deleteMedication(medication: MedicationEntry) = dao.deleteMedication(medication)
    suspend fun getCalorieGoalChanges(): List<CalorieGoalChange> = dao.getCalorieGoalChanges()
    suspend fun addCalorieGoalChange(change: CalorieGoalChange): Long = dao.addCalorieGoalChange(change)
}

/** Cross-feature, transaction-safe operations used by global History. */
class HistoryRepository internal constructor(
    private val database: DailyRebuildDatabase
) {
    suspend fun deleteDate(date: String) = database.withTransaction {
        database.dailyRecordDao().getRecordByDate(date)?.let { record ->
            database.dailyRecordDao().deleteRecord(record)
        }
        database.foodDao().deleteEntriesForDate(date)
        database.dailyActivityDao().getSnapshotByDate(date)?.let { snapshot ->
            database.dailyActivityDao().deleteSnapshot(snapshot)
        }
        database.mobilitySessionDao().deleteSessionsForDate(date)
        database.showerLogDao().deleteByDate(date)
        database.migraineLogDao().deleteByDate(date)
        database.meetingDao().deleteAttendanceByDate(date)
        database.careVisitDao().getVisitsForDate(date).forEach { visit ->
            database.careAppointmentDao().clearConvertedVisitLink(
                visitId = visit.id,
                updatedAt = System.currentTimeMillis()
            )
        }
        database.careVisitDao().deleteVisitsByDate(date)
        database.careAppointmentDao().deleteAppointmentsByDate(date)
        database.lifeMaintenanceDao().deleteByDate(date)
    }
}
