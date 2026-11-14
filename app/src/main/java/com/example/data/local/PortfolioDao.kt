package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.StockEntity
import com.example.data.model.StockTradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    // Mutual Funds
    @Query("SELECT * FROM mutual_funds ORDER BY id ASC")
    fun observeMutualFunds(): Flow<List<MutualFundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutualFund(fund: MutualFundEntity): Long

    @Delete
    suspend fun deleteMutualFund(fund: MutualFundEntity)

    @Query("SELECT * FROM mutual_fund_transactions ORDER BY dateMillis DESC, id DESC")
    fun observeMutualFundTxs(): Flow<List<MutualFundTxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutualFundTx(tx: MutualFundTxEntity): Long

    @Delete
    suspend fun deleteMutualFundTx(tx: MutualFundTxEntity)

    @Query("DELETE FROM mutual_fund_transactions WHERE fundId = :fundId")
    suspend fun deleteTxsForFund(fundId: Long)

    // Stocks
    @Query("SELECT * FROM stocks ORDER BY id ASC")
    fun observeStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity): Long

    @Delete
    suspend fun deleteStock(stock: StockEntity)

    @Query("SELECT * FROM stock_trades ORDER BY dateMillis DESC, id DESC")
    fun observeStockTrades(): Flow<List<StockTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTrade(trade: StockTradeEntity): Long

    @Delete
    suspend fun deleteStockTrade(trade: StockTradeEntity)

    @Query("DELETE FROM stock_trades WHERE stockId = :stockId")
    suspend fun deleteTradesForStock(stockId: Long)

    // PPF
    @Query("SELECT * FROM ppf_accounts ORDER BY id ASC")
    fun observePpfAccounts(): Flow<List<PpfAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPpfAccount(acc: PpfAccountEntity): Long

    @Delete
    suspend fun deletePpfAccount(acc: PpfAccountEntity)

    @Query("SELECT * FROM ppf_transactions ORDER BY dateMillis ASC, id ASC")
    fun observePpfTxs(): Flow<List<PpfTxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPpfTx(tx: PpfTxEntity): Long

    @Delete
    suspend fun deletePpfTx(tx: PpfTxEntity)

    @Query("DELETE FROM ppf_transactions WHERE ppfAccountId = :ppfId")
    suspend fun deleteTxsForPpf(ppfId: Long)

    @Query("SELECT * FROM ppf_fy_rates ORDER BY financialYearStart DESC")
    fun observePpfRates(): Flow<List<PpfFyRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPpfRate(rate: PpfFyRateEntity)

    // EPF
    @Query("SELECT * FROM epf_accounts ORDER BY id ASC")
    fun observeEpfAccounts(): Flow<List<EpfAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpfAccount(acc: EpfAccountEntity): Long

    @Delete
    suspend fun deleteEpfAccount(acc: EpfAccountEntity)

    @Query("SELECT * FROM epf_entries ORDER BY dateMillis DESC, id DESC")
    fun observeEpfEntries(): Flow<List<EpfEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpfEntry(entry: EpfEntryEntity): Long

    @Delete
    suspend fun deleteEpfEntry(entry: EpfEntryEntity)

    @Query("DELETE FROM epf_entries WHERE epfAccountId = :epfId")
    suspend fun deleteEntriesForEpf(epfId: Long)

    // Other Assets
    @Query("SELECT * FROM other_assets ORDER BY id ASC")
    fun observeOtherAssets(): Flow<List<OtherAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtherAsset(asset: OtherAssetEntity): Long

    @Delete
    suspend fun deleteOtherAsset(asset: OtherAssetEntity)

    // Clear all
    @Query("DELETE FROM mutual_fund_transactions") suspend fun clearMutualFundTxs()
    @Query("DELETE FROM mutual_funds") suspend fun clearMutualFunds()
    @Query("DELETE FROM stock_trades") suspend fun clearStockTrades()
    @Query("DELETE FROM stocks") suspend fun clearStocks()
    @Query("DELETE FROM ppf_transactions") suspend fun clearPpfTxs()
    @Query("DELETE FROM ppf_accounts") suspend fun clearPpfAccounts()
    @Query("DELETE FROM ppf_fy_rates") suspend fun clearPpfRates()
    @Query("DELETE FROM epf_entries") suspend fun clearEpfEntries()
    @Query("DELETE FROM epf_accounts") suspend fun clearEpfAccounts()
    @Query("DELETE FROM other_assets") suspend fun clearOtherAssets()
}
