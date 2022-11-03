package uz.nurlibaydev.ussdapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import uz.nurlibaydev.ussdapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if(granted){
                runUSSDCode()
            }
        }

        binding.btnSendUssdCode.setOnClickListener {
            result.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun runUSSDCode() {
        binding.apply {
            if(!etUssdCode.text.toString().startsWith("*") && !etUssdCode.text.toString().endsWith("#")){
                Toast.makeText(this@MainActivity, "Enter a valid ussd code", Toast.LENGTH_SHORT).show()
                return
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                if(ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                    return
                }
                /** Use Telephony manager */
                val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_status, null, false)
                val progressBar = dialogView.findViewById<CircularProgressIndicator>(R.id.progress_circular)
                val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
                val btnOk = dialogView.findViewById<MaterialButton>(R.id.btn_ok)

                val alertDialog = AlertDialog.Builder(this@MainActivity)
                    .setView(dialogView)
                    .setCancelable(false)

                val dialog = alertDialog.show()

                btnOk.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                btnOk.setOnClickListener {
                    dialog.dismiss()
                }

                /** Run USSD code */
                val callback: UssdResponseCallback = object : UssdResponseCallback() {
                    override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                        super.onReceiveUssdResponse(telephonyManager, request, response)
                        progressBar.visibility = View.GONE
                        tvMessage.text = response.toString()
                        btnOk.visibility = View.VISIBLE
                    }

                    override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                        super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode)
                        progressBar.visibility = View.GONE
                        tvMessage.text = getString(R.string.failed_status)
                        btnOk.visibility = View.VISIBLE
                    }
                }

                telephonyManager.sendUssdRequest(etUssdCode.text.toString(), callback, handler)

            } else {
                var ussdCode = etUssdCode.text.toString()
                ussdCode = ussdCode.substring(0, ussdCode.length - 1)
                ussdCode += Uri.encode("#")
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$ussdCode"))
                startActivity(intent)
            }
        }
    }
}