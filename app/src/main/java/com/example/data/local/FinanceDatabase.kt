package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.EpfEntryType
import com.example.data.model.HoldingSource
import com.example.data.model.MutualFundCategory
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundOption
import com.example.data.model.MutualFundPlan
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.MutualFundTxType
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.NetWorthSnapshotEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.PpfTxType
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockEntity
import com.example.data.model.StockExchange
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name
    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)
    @TypeConverter fun fromRecurringFreq(v: RecurringFrequency): String = v.name
    @TypeConverter fun toRecurringFreq(v: String): RecurringFrequency = RecurringFrequency.valueOf(v)
    @TypeConverter fun fromAssetCat(v: AssetLiabilityCategory): String = v.name
    @TypeConverter fun toAssetCat(v: String): AssetLiabilityCategory = AssetLiabilityCategory.valueOf(v)
    @TypeConverter fun fromHoldingSource(v: HoldingSource): String = v.name
    @TypeConverter fun toHoldingSource(v: String): HoldingSource = HoldingSource.valueOf(v)
    @TypeConverter fun fromMfCategory(v: MutualFundCategory): String = v.name
    @TypeConverter fun toMfCategory(v: String): MutualFundCategory = MutualFundCategory.valueOf(v)
    @TypeConverter fun fromMfPlan(v: MutualFundPlan): String = v.name
    @TypeConverter fun toMfPlan(v: String): MutualFundPlan = MutualFundPlan.valueOf(v)
    @TypeConverter fun fromMfOption(v: MutualFundOption): String = v.name
    @TypeConverter fun toMfOption(v: String): MutualFundOption = MutualFundOption.valueOf(v)
    @TypeConverter fun fromMfTxType(v: MutualFundTxType): String = v.name
    @TypeConverter fun toMfTxType(v: String): MutualFundTxType = MutualFundTxType.valueOf(v)
    @TypeConverter fun fromStockExchange(v: StockExchange): String = v.name
    @TypeConverter fun toStockExchange(v: String): StockExchange = StockExchange.valueOf(v)
    @TypeConverter fun fromStockTradeType(v: StockTradeType): String = v.name
    @TypeConverter fun toStockTradeType(v: String): StockTradeType = StockTradeType.valueOf(v)
    @TypeConverter fun fromPpfTxType(v: PpfTxType): String = v.name
    @TypeConverter fun toPpfTxType(v: String): PpfTxType = PpfTxType.valueOf(v)
    @TypeConverter fun fromEpfEntryType(v: EpfEntryType): String = v.name
    @TypeConverter fun toEpfEntryType(v: String): EpfEntryType = EpfEntryType.valueOf(v)
    @TypeConverter fun fromOtherAssetKind(v: OtherAssetKind): String = v.name
    @TypeConverter fun toOtherAssetKind(v: String): OtherAssetKind = OtherAssetKind.valueOf(v)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun observeCreditCards(): Flow<List<CreditCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity): Long

    @Delete
    suspend fun deleteCreditCard(card: CreditCardEntity)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM budgets")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    // Savings Goals
    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    // Net Worth
    @Query("SELECT * FROM net_worth_items ORDER BY id ASC")
    fun observeNetWorthItems(): Flow<List<NetWorthItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetWorthItem(item: NetWorthItemEntity): Long

    @Delete
    suspend fun deleteNetWorthItem(item: NetWorthItemEntity)

    @Query("SELECT * FROM net_worth_snapshots ORDER BY monthYear ASC")
    fun observeNetWorthSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetWorthSnapshot(snapshot: NetWorthSnapshotEntity): Long

    // Categorization Rules
    @Query("SELECT * FROM categorization_rules ORDER BY id DESC")
    fun observeRules(): Flow<List<CategorizationRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: CategorizationRuleEntity)

    // Recurring Bills & Subscriptions
    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC")
    fun observeRecurringBills(): Flow<List<RecurringBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long

    @Delete
    suspend fun deleteRecurringBill(bill: RecurringBillEntity)

    // User Settings
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    // Clear all for reset
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM credit_cards") suspend fun clearCreditCards()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM savings_goals") suspend fun clearSavingsGoals()
    @Query("DELETE FROM net_worth_items") suspend fun clearNetWorthItems()
    @Query("DELETE FROM net_worth_snapshots") suspend fun clearNetWorthSnapshots()
    @Query("DELETE FROM categorization_rules") suspend fun clearRules()
    @Query("DELETE FROM recurring_bills") suspend fun clearRecurringBills()
    @Query("DELETE FROM user_settings") suspend fun clearSettings()
}

@Database(
    entities = [
        AccountEntity::class,
        CreditCardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        NetWorthItemEntity::class,
        NetWorthSnapshotEntity::class,
        CategorizationRuleEntity::class,
        RecurringBillEntity::class,
        UserSettingsEntity::class,
        MutualFundEntity::class,
        MutualFundTxEntity::class,
        StockEntity::class,
        StockTradeEntity::class,
        PpfAccountEntity::class,
        PpfTxEntity::class,
        PpfFyRateEntity::class,
        EpfAccountEntity::class,
        EpfEntryEntity::class,
        OtherAssetEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun portfolioDao(): PortfolioDao

    companion object {
        @Volatile private var INSTANCE: FinanceDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'AMOLED_DARK'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN fontStyle TEXT NOT NULL DEFAULT 'JAKARTA_SANS'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isIgnored INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN repeatCount INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN linkedLoanId INTEGER")
            }
        }

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finflow_finance.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    // Only pre-v5 installs (no migration path) may be reset; v5+ must always migrate to protect user data.
                    .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
