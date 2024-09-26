package cz.aquasoup.barking.activities

import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cz.aquasoup.barking.yamnet.CsvData
import cz.aquasoup.barking.R
import cz.aquasoup.barking.service.BarkService
import cz.aquasoup.barking.service.LISTENER_SENS
import cz.aquasoup.barking.service.LISTENER_URI
import cz.aquasoup.barking.service.LISTENER_WHAT


class MainActivity : AppCompatActivity() {

    private val permissionRequestCode = 100

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var fileButton: Button

    private lateinit var spinner: Spinner
    private lateinit var seekBar: SeekBar
    private lateinit var sharedPref: SharedPreferences

    private val someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                with(sharedPref.edit()) {
                    putString("uri", uri.toString())
                    apply()
                }
                refreshButtons()
            }
        }
    }

    private fun populateSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            CsvData.data.sorted()
        )
        spinner.setAdapter(adapter)

        spinner.setSelection(sharedPref.getInt("position", CsvData.data.sorted().indexOf("Bark")))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                with(sharedPref.edit()) {
                    putInt("position", position)
                    apply()
                }
            }

        }
    }

    private fun populateSeekBar() {
        seekBar.progress = sharedPref.getInt("progress", 100)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                with(sharedPref.edit()) {
                    putInt("progress", progress)
                    apply()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            permissionRequestCode
            -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    Toast.makeText(this, "Everything is OK", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Need every permission", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun refreshButtons() {
        val sel = sharedPref.getString("uri", "")

        resetButton.isEnabled = !sel.isNullOrEmpty()
        fileButton.isEnabled = sel.isNullOrEmpty()
    }

    private fun requestPermissionsForApp() {
        when {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, RECORD_AUDIO

            ) -> {
                Toast.makeText(this, "Need every permission", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val arr = mutableListOf(
                    READ_MEDIA_AUDIO,
                    POST_NOTIFICATIONS,
                    RECORD_AUDIO,
                    FOREGROUND_SERVICE
                )
                if (Build.VERSION.SDK_INT > 33) {
                    arr.add(FOREGROUND_SERVICE_MICROPHONE)
                }
                requestPermissions(
                    arr.toTypedArray(),
                    permissionRequestCode
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> {
                startActivity(Intent(this, AboutActivity::class.java))

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.my_toolbar))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE)

        startButton = findViewById(R.id.start)
        stopButton = findViewById(R.id.stop)
        resetButton = findViewById(R.id.resetAudio)
        fileButton = findViewById(R.id.file)

        seekBar = findViewById(R.id.seekBar)
        spinner = findViewById(R.id.spinner)

        requestPermissionsForApp()

        populateSpinner()
        populateSeekBar()

        refreshButtons()



        fileButton.setOnClickListener {
            val pickMedia = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
            someActivityResultLauncher.launch(pickMedia)
        }

        resetButton.setOnClickListener {
            with(sharedPref.edit()) {
                putString("uri", "")
                apply()
            }
            refreshButtons()
        }

        startButton.setOnClickListener {
            val serviceListen = Intent(this, BarkService::class.java)
            serviceListen.putExtra(LISTENER_WHAT, spinner.selectedItem.toString())
            serviceListen.putExtra(LISTENER_SENS, seekBar.progress)
            serviceListen.putExtra(LISTENER_URI, sharedPref.getString("uri", ""))
            ContextCompat.startForegroundService(this, serviceListen)
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, BarkService::class.java))
        }

    }


}