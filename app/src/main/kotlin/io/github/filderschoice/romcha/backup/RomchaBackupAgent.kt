package io.github.filderschoice.romcha.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput

/**
 * 設定のバックアップを利用者のスイッチで止められるようにするバックアップエージェント（BL-097）。
 *
 * スイッチがオフの間は何も書き出さない。対象のファイルは従来どおり `data_extraction_rules.xml` が決める。
 * 復元側は規則どおりに処理するため、ここでは何もしない。
 */
class RomchaBackupAgent : BackupAgentHelper() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        if (BackupSettings.isEnabledNow(this)) super.onFullBackup(data)
    }
}
