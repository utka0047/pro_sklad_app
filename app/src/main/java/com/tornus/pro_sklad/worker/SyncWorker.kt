package com.tornus.pro_sklad.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.Result
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val productRepository: ProductRepository,
    private val movementRepository: MovementRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val productsResult = productRepository.syncProducts()
        if (productsResult is com.tornus.pro_sklad.util.Result.Error) return Result.retry()
        val movementsResult = movementRepository.syncMovements()
        if (movementsResult is com.tornus.pro_sklad.util.Result.Error) return Result.retry()
        return Result.success()
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONE_TIME = "sync_one_time"
    }
}
