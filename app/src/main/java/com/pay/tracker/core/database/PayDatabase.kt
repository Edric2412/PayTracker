package com.pay.tracker.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PayDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: PayDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN direction TEXT NOT NULL DEFAULT 'DEBIT'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN classification TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN originalExpenseId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_direction ON transactions(direction)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_classification ON transactions(classification)")
            }
        }

        fun getInstance(context: Context): PayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PayDatabase::class.java,
                    "pay_expenses.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
