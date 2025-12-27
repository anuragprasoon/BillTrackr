package com.example.billtrackr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BillViewData(application: Application) : AndroidViewModel(application) {

    private val dao =
        BillDatabase.getDatabase(application).BillDao()

    val bills = dao.getAllBills()

    fun addBills(bill: Bill) {
        viewModelScope.launch {
            dao.insertBill(bill)
        }
    }
}
