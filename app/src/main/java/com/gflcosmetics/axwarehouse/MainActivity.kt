package com.gflcosmetics.axwarehouse

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var imageViewBarcode: ImageView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // get reference to button
        val btnDisponibilita = findViewById<Button>(R.id.btnDisponibilita)
        // set on-click listener
        btnDisponibilita.setOnClickListener {
            findViewById<TextView>(R.id.textView).text = "Cliccato"
            // your code to perform when the user clicks on the button
            run("https://traffix.gfl.eu:410/api/Pallet?token=104aac6d-1037-407d-8615-adf76949bd07&companyId=GCH&search=")
        }
    }

    private fun run(urlbase: String) {
        val pallet = findViewById<EditText>(R.id.editTextPalletUbica).text.trim()
        if(pallet.equals("")) {
            updateUI("Inserire un parametro di ricerca")
            return
        }
        findViewById<TextView>(R.id.textView).text = "Aggiornamento in corso..."
        val url = urlbase + pallet
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateUI(e.message)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    updateUI(response.body!!.string())
                }
            }
        })
    }

    public fun updateUI(s: String?) {
        var txt = "Non trovato"
        runOnUiThread {
            kotlin.run {
                if (s != null) {
                    if(s.isNotEmpty())
                    {
                        txt = "Completato"
                        imageViewBarcode = findViewById<ImageView>(R.id.imageViewBarcode)
                        // val inputStreamReader = InputStreamReader(s, charset("UTF-8"))
                        val typeToken = object : TypeToken<List<Availability>>(){}.type
                        val availList : List<Availability> =
                            Gson().fromJson(s, typeToken)
                        if(availList.isNotEmpty()) {
                            findViewById<TextView>(R.id.tvItem).text = availList[0].ItemId
                            findViewById<TextView>(R.id.tvPallet).text = availList[0].Id
                            findViewById<TextView>(R.id.tvDescription).text = availList[0].Item
                            findViewById<TextView>(R.id.tvQty).text = availList[0].Invent.toInt().toString()
                            findViewById<TextView>(R.id.tvBatch).text = availList[0].Batch
                            try {
                                val multi = MultiFormatWriter()
                                val bitMatrix : BitMatrix = multi.encode(availList[0].Id,
                                    BarcodeFormat.CODE_39, imageViewBarcode.width, imageViewBarcode.height)
                                val bitMap: Bitmap = Bitmap.createBitmap(imageViewBarcode.width, imageViewBarcode.height, Bitmap.Config.RGB_565)
                                var i = 0
                                var j = 0
                                val w = imageViewBarcode.width
                                val h = imageViewBarcode.height
                                while (i < w) {
                                    while (j < h) {
                                        if(bitMatrix.get(i,j))
                                        {
                                            bitMap.setPixel(i,j,Color.Black.value.toInt())
                                        } else {
                                            bitMap.setPixel(i,j,Color.White.value.toInt())
                                        }
                                        j++
                                    }
                                    i++
                                }

                                // imageViewBarcode.setImageBitmap(bitMap)

                                val b = ByteArrayOutputStream()
                                bitMap.compress(Bitmap.CompressFormat.PNG, 60, b)
                                val byteArray = b.toByteArray()
                                val compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                imageViewBarcode.setImageBitmap(compressedBitmap)
                                //var d = applicationContext.resources.getDrawable(R.id.imageViewBarcode.toInt(), this.theme)
                                imageViewBarcode.setImageBitmap(compressedBitmap)

                            }
                            catch (e: Exception)
                            {
                                txt = e.message.toString()
                            }
                        }
                    }
                }
                findViewById<TextView>(R.id.textView).text = txt
            }
        }
    }

}