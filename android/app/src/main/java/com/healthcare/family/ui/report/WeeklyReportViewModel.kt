package com.healthcare.family.ui.report

import androidx.lifecycle.ViewModel
import com.healthcare.family.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    val recordRepository: RecordRepository,
) : ViewModel()
