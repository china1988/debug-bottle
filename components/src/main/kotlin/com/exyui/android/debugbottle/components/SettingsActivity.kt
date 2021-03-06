package com.exyui.android.debugbottle.components

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.SwitchCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.SeekBar
import com.exyui.android.debugbottle.components.floating.FloatingService
import com.exyui.android.debugbottle.ui.BlockCanary

internal class SettingsActivity : DTBaseActivity() {

    private val MIN: Int = 15
    private val MAX: Int = 5000
    private val DEFAULT: Int = 500

    override val actionbarTitle: String by lazy { getString(R.string.__dt_setting_title) }

    private val seekBar: SeekBar by lazy {
        val result = findViewById(R.id.__dt_seek_bar) as SeekBar
        result.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(v: SeekBar?) {

            }

            override fun onStartTrackingTouch(v: SeekBar?) {

            }

            override fun onProgressChanged(v: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    valueText.setText((progress + MIN).toString())
                }
            }
        })
        result.max = MAX - MIN
        result.progress = DTSettings.getBlockThreshold().toInt() - MIN
        result
    }

    private val valueText: EditText by lazy {
        val result = findViewById(R.id.__dt_seek_value) as EditText
        result.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    var num: Int = s.toString().toInt()
                    if (num < MIN || num > MAX) {
                        num = DEFAULT
                    }
                    seekBar.progress = num
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
        result.setText((seekBar.progress + MIN).toString())
        result
    }

    private val networkSwitcher by lazy {
        val result = findViewById(R.id.__dt_network_switcher) as SwitchCompat
        result.isChecked = DTSettings.getNetworkSniff()
        result.setOnCheckedChangeListener { view, isChecked ->
            DTSettings.setNetworkSniff(isChecked)
        }
        result
    }

    private val strictSwitcher by lazy {
        val result = findViewById(R.id.__dt_strict_switcher) as SwitchCompat
        result.isChecked = DTSettings.getStrictMode()
        result.setOnCheckedChangeListener { view, isChecked ->
            DTSettings.setStrictMode(isChecked)
        }
        result
    }

    private val view3DSwitcher by lazy {
        val result = findViewById(R.id.__dt_3d_switcher) as SwitchCompat
        result.isChecked = isFloatingWindowRunning()
        result.setOnCheckedChangeListener { view, isChecked ->
            val intent = Intent(this, FloatingService::class.java)
            if (isChecked) {
                startService(intent)
            } else {
                stopService(intent)
            }
        }
        result
    }

    private val leakCanarySwitcher by lazy {
        val result = findViewById(R.id.__dt_leak_canary_switcher) as SwitchCompat
        result.isChecked = DTSettings.getLeakCanaryEnable()
        result.setOnCheckedChangeListener { view, isChecked ->
            DTSettings.setLeakCanaryEnable(isChecked)
        }
        result
    }

    private val blockCanarySwitcher by lazy {
        val result = findViewById(R.id.__dt_block_canary_switcher) as SwitchCompat
        result.isChecked = DTSettings.getBlockCanaryEnable()
        result.setOnCheckedChangeListener { view, isChecked ->
            DTSettings.setBlockCanaryEnable(isChecked)
            try {
                if (isChecked) {
                    BlockCanary.get().start()
                } else {
                    BlockCanary.get().stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.__activity_settings)
        seekBar; valueText; networkSwitcher; strictSwitcher; view3DSwitcher
        leakCanarySwitcher; blockCanarySwitcher
    }

    override fun onStop() {
        super.onStop()
        save()
    }

    private fun save() {
        DTSettings.setBlockThreshold(seekBar.progress.toLong())
    }

    private fun isFloatingWindowRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val serviceList = activityManager.getRunningServices(30)

        if (serviceList.size <= 0) {
            return false
        }

        for (i in 0..serviceList.size - 1) {
            if (serviceList[i].service.className.equals(FloatingService::class.java.name) == true) {
                return true
            }
        }
        return false
    }
}
