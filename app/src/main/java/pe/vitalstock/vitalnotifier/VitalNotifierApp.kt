package pe.vitalstock.vitalnotifier

import android.app.Application
import pe.vitalstock.vitalnotifier.data.local.AppDatabase
import pe.vitalstock.vitalnotifier.data.local.PreferencesManager
import pe.vitalstock.vitalnotifier.data.remote.VitalStockClient
import pe.vitalstock.vitalnotifier.service.PaymentDispatcher

class VitalNotifierApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var vitalStockClient: VitalStockClient
        private set

    lateinit var paymentDispatcher: PaymentDispatcher
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        database = AppDatabase.getInstance(this)
        vitalStockClient = VitalStockClient()
        paymentDispatcher = PaymentDispatcher(
            dao = database.paymentDao(),
            client = vitalStockClient,
            preferences = preferencesManager
        )
    }

    companion object {
        lateinit var instance: VitalNotifierApp
            private set
    }
}
