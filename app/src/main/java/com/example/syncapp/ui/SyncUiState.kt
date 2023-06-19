package com.example.syncapp.ui

import androidx.compose.ui.graphics.Color
import java.io.DataInputStream
import java.io.DataOutputStream

data class SyncUiState(
    var socketReader: DataInputStream? = null,
    var socketWriter: DataOutputStream? = null,
    var isConnected: Boolean = false,
    var isRequesting: Boolean = false,
    var connectionColor: Color  = red,
    var statusString : String = "",
    var isWorking: Boolean = false,
    var localFilesCount: Int = 0,
    var globalFilesCount: Int = 0,
    var globalDirsCount: Int = 0,
    var globalSize: Int = 0,
    var localDirsCount: Int = 0,
    var localSize: Int = 0,
    var isUpToDate: Boolean = false,
    var syncDate: String = "---",
)
