package one.yufz.setproxy


object Permission {
    const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

    const val COMMAND = "pm grant ${BuildConfig.APPLICATION_ID} $PERMISSION"

    const val ADB_COMMAND = "adb shell $COMMAND"
    
    const val SU_COMMAND = "su -c $COMMAND"
}