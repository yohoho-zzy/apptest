package com.example.quotepicker.util

import android.os.Environment
import java.io.File

object StoragePaths {
    private const val ROOT_DIR = "111rensheng"
    private const val SYS_SUBDIR = "zy/sys"
    private const val FILE_SUBDIR = "zy/file"

    private fun externalRoot(): File {
        return Environment.getExternalStorageDirectory()
    }

    fun sysDir(): File = File(externalRoot(), "$ROOT_DIR/$SYS_SUBDIR")

    fun fileDir(): File = File(externalRoot(), "$ROOT_DIR/$FILE_SUBDIR")
}
