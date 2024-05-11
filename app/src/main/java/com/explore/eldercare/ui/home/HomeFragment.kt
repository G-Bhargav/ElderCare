package com.explore.eldercare.ui.home

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.explore.eldercare.databinding.FragmentHomeBinding
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.io.encoding.ExperimentalEncodingApi

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var uri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivHome.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                Launcher.launch(it)

            }

        }

        binding.btnSend.setOnClickListener {
            val contentResolver = context?.contentResolver
            val base64Image = contentResolver?.let { it1 -> uri?.let { it2 ->
                imageToBase64(it1,
                    it2
                )
            } }
            if (base64Image != null) {
                Log.i("data","done")
                sendImageToServer(base64Image)
            }
        }

    }


    private val Launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            (
                    if (result.resultCode == AppCompatActivity.RESULT_OK) {
                        result?.data?.data?.let {
                            uri = it
                            context?.let { it1 -> Glide.with(it1).load(uri).into(binding.ivHome) }
                        }
                    }
                    )

        }

    @OptIn(ExperimentalEncodingApi::class)
    fun imageToBase64(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

            base64Image
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //////////////////////////////////////////////////////////////
    data class ImageRequest(val image: String)

    interface ApiService {
        @POST("/")
        fun sendImage(@Body request: ImageRequest): Call<String>
    }

    object ApiClient {
        private const val BASE_URL = "http://127.0.0.1:5003/"

        val apiService: ApiService by lazy {
            val retrofit =Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ApiService::class.java)
        }
    }

    private fun sendImageToServer(base64Image: String) {
        val request = ImageRequest(base64Image)
        val call = ApiClient.apiService.sendImage(request)

        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    // Handle the response here (parse JSON, display result, etc.)
                    println(responseBody)
                } else {
                    // Handle unsuccessful response
                    println("Failed to send image: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                // Handle failure
                println("Failed to send image: ${t.message}")
            }
        })
    }

    /////////////////////////////////


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}