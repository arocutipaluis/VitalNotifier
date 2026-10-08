package pe.vitalstock.vitalnotifier.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import pe.vitalstock.vitalnotifier.data.model.DispatchStatus
import pe.vitalstock.vitalnotifier.data.model.PaymentEntity

class Converters {
    @TypeConverter
    fun fromStatus(status: DispatchStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DispatchStatus = runCatching {
        DispatchStatus.valueOf(value)
    }.getOrDefault(DispatchStatus.PENDING)
}

@Database(entities = [PaymentEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vitalnotifier_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
