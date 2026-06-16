package com.healthcare.family.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthcare.family.data.remote.api.BpRecordDto
import com.healthcare.family.data.remote.api.BgRecordDto
import com.healthcare.family.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    val bgType: String = "fasting",
    val bgValue: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val recentBpRecords: List<BpRecordDto> = emptyList(),
    val recentBgRecords: List<BgRecordDto> = emptyList(),
    // 语音识别相关状态
    val voiceRecordState: VoiceRecordState = VoiceRecordState.IDLE,
    val recognizedText: String = "",
    val parsedHealthData: ParsedHealthData? = null,
    val isVoiceProcessing: Boolean = false,
    val voiceAudioBase64: String = "",
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        loadRecentRecords()
    }

    fun onSystolicChanged(value: String) {
        _uiState.update { it.copy(systolic = value, errorMessage = null, successMessage = null) }
    }

    fun onDiastolicChanged(value: String) {
        _uiState.update { it.copy(diastolic = value, errorMessage = null, successMessage = null) }
    }

    fun onHeartRateChanged(value: String) {
        _uiState.update { it.copy(heartRate = value, errorMessage = null, successMessage = null) }
    }

    fun onBgTypeChanged(type: String) {
        _uiState.update { it.copy(bgType = type, errorMessage = null, successMessage = null) }
    }

    fun onBgValueChanged(value: String) {
        _uiState.update { it.copy(bgValue = value, errorMessage = null, successMessage = null) }
    }

    fun submitBpRecord() {
        val state = _uiState.value
        val sys = state.systolic.toIntOrNull()
        val dia = state.diastolic.toIntOrNull()
        val hr = state.heartRate.toIntOrNull()

        if (sys == null || dia == null) {
            _uiState.update { it.copy(errorMessage = "请输入有效的血压数值") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recordRepository.addBpRecord(sys, dia, hr).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "血压记录成功",
                            systolic = "",
                            diastolic = "",
                            heartRate = "",
                        )
                    }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun submitBgRecord() {
        val state = _uiState.value
        val value = state.bgValue.toDoubleOrNull()

        if (value == null) {
            _uiState.update { it.copy(errorMessage = "请输入有效的血糖数值") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recordRepository.addBgRecord(state.bgType, value).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "血糖记录成功",
                            bgValue = "",
                        )
                    }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                },
            )
        }
    }

    private fun loadRecentRecords() {
        viewModelScope.launch {
            recordRepository.getBpRecords(5).onSuccess { records ->
                _uiState.update { it.copy(recentBpRecords = records) }
            }
            recordRepository.getBgRecords(5).onSuccess { records ->
                _uiState.update { it.copy(recentBgRecords = records) }
            }
        }
    }

    fun deleteBpRecord(recordId: String) {
        viewModelScope.launch {
            recordRepository.deleteBpRecord(recordId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "已删除") }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }

    fun deleteBgRecord(recordId: String) {
        viewModelScope.launch {
            recordRepository.deleteBgRecord(recordId).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "已删除") }
                    loadRecentRecords()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                },
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ==================== 语音识别相关 ====================

    fun onVoiceRecordStart() {
        _uiState.update { it.copy(voiceRecordState = VoiceRecordState.RECORDING, errorMessage = null) }
    }

    fun onVoiceRecordStop(audioBase64: String) {
        _uiState.update {
            it.copy(
                voiceRecordState = VoiceRecordState.COMPLETED,
                voiceAudioBase64 = audioBase64,
            )
        }
        // 自动进行语音识别
        recognizeVoice(audioBase64)
    }

    fun onVoiceRecordCancel() {
        _uiState.update {
            it.copy(
                voiceRecordState = VoiceRecordState.IDLE,
                recognizedText = "",
                parsedHealthData = null,
                voiceAudioBase64 = "",
            )
        }
    }

    private fun recognizeVoice(audioBase64: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVoiceProcessing = true, errorMessage = null) }
            try {
                // 调用后端语音识别接口
                val result = recordRepository.recognizeVoice(audioBase64)
                result.fold(
                    onSuccess = { response ->
                        val parsedData = parseVoiceText(response.text)
                        _uiState.update {
                            it.copy(
                                isVoiceProcessing = false,
                                recognizedText = response.text,
                                parsedHealthData = parsedData,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isVoiceProcessing = false,
                                errorMessage = "语音识别失败: ${e.message}",
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVoiceProcessing = false,
                        errorMessage = "语音识别失败: ${e.message}",
                    )
                }
            }
        }
    }

    private fun parseVoiceText(text: String): ParsedHealthData {
        // 简单的文本解析逻辑（客户端预解析，实际以服务端为准）
        val cleanedText = text.replace(Regex("[，。！？、]"), " ").trim()

        // 尝试解析血压
        val bpPattern = Regex("(?:血压|收缩压|高压)?\\s*(\\d{2,3})\\s*(?:舒张压|低压)?\\s*(\\d{2,3})")
        val bpMatch = bpPattern.find(cleanedText)
        if (bpMatch != null) {
            val systolic = bpMatch.groupValues[1].toIntOrNull()
            val diastolic = bpMatch.groupValues[2].toIntOrNull()
            if (systolic != null && diastolic != null && systolic > diastolic) {
                return ParsedHealthData(
                    type = ParsedDataType.BLOOD_PRESSURE,
                    rawText = text,
                    systolic = systolic,
                    diastolic = diastolic,
                    parsed = true,
                )
            }
        }

        // 尝试解析血糖
        val bgTypeMap = mapOf(
            "空腹" to "fasting",
            "餐前" to "before_meal",
            "餐后两小时" to "after_meal_2h",
            "餐后" to "after_meal",
            "随机" to "random",
            "睡前" to "bedtime",
        )

        for ((keyword, type) in bgTypeMap) {
            val pattern = Regex("$keyword\\s*(?:血糖)?\\s*(\\d+\\.?\\d*)")
            val match = pattern.find(cleanedText)
            if (match != null) {
                val value = match.groupValues[1].toDoubleOrNull()
                if (value != null && value in 1.0..35.0) {
                    return ParsedHealthData(
                        type = ParsedDataType.BLOOD_SUgar,
                        rawText = text,
                        bgType = type,
                        bgValue = value,
                        parsed = true,
                    )
                }
            }
        }

        // 解析失败
        return ParsedHealthData(
            type = ParsedDataType.UNKNOWN,
            rawText = text,
            parsed = false,
        )
    }

    fun confirmVoiceRecord(data: ParsedHealthData) {
        when (data.type) {
            ParsedDataType.BLOOD_PRESSURE -> {
                if (data.systolic != null && data.diastolic != null) {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        recordRepository.addBpRecord(
                            data.systolic,
                            data.diastolic,
                            data.heartRate,
                            inputMethod = "voice",
                        ).fold(
                            onSuccess = {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        successMessage = "血压记录成功",
                                        voiceRecordState = VoiceRecordState.IDLE,
                                        recognizedText = "",
                                        parsedHealthData = null,
                                        voiceAudioBase64 = "",
                                    )
                                }
                                loadRecentRecords()
                            },
                            onFailure = { e ->
                                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                            },
                        )
                    }
                }
            }
            ParsedDataType.BLOOD_SUgar -> {
                if (data.bgType != null && data.bgValue != null) {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true) }
                        recordRepository.addBgRecord(
                            data.bgType,
                            data.bgValue,
                            inputMethod = "voice",
                        ).fold(
                            onSuccess = {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        successMessage = "血糖记录成功",
                                        voiceRecordState = VoiceRecordState.IDLE,
                                        recognizedText = "",
                                        parsedHealthData = null,
                                        voiceAudioBase64 = "",
                                    )
                                }
                                loadRecentRecords()
                            },
                            onFailure = { e ->
                                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                            },
                        )
                    }
                }
            }
            ParsedDataType.UNKNOWN -> {
                _uiState.update { it.copy(errorMessage = "无法自动识别，请手动输入") }
            }
        }
    }
}
