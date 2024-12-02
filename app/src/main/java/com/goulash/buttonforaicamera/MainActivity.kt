package com.goulash.buttonforaicamera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goulash.buttonforaicamera.ui.theme.ButtonForAiCameraTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val httpClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                json = Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    useAlternativeNames = false
                    encodeDefaults = true
                },
                contentType = ContentType.Any,
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 20000
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }
}


@Serializable
data class OrderBody(
    @SerialName("order_id")
    val orderId: Int
)

@Serializable
data class Response(
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String
)

class Repository {
    suspend fun sendSignalToRaspberry(host: String): Response {
        val response = try {
            httpClient.post("http://${host}:8080/take_screenshot") {
                setBody(OrderBody(orderId = 666))
            }.body<Response>()
        } catch (e: Exception) {
            Response(status = "error", message = "Произошла ошибка")
        }
        return response
    }
}

private val repository = Repository()

private const val HOST_KEY = "HOST_KEY"
private const val FIRST_LAUNCH_KEY = "FIRST_LAUNCH_KEY"

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val sharedPref = this.getSharedPreferences("BUTTON_AI_SHARED_PREFS", Context.MODE_PRIVATE)
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            var host by remember { mutableStateOf(sharedPref.getString(HOST_KEY, "") ?: "") }
            var firstLaunch by remember {
                mutableStateOf(sharedPref.getBoolean(FIRST_LAUNCH_KEY, true))
            }
            val coroutineScope = rememberCoroutineScope()
            ButtonForAiCameraTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (firstLaunch) {
                            TextField(
                                value = host,
                                onValueChange = { host = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.TopCenter)
                            )
                        }

                        SendButton(
                            text = "",
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .align(Alignment.Center),
                            onClick = {
                                if (firstLaunch) {
                                    with(sharedPref.edit()) {
                                        putString(HOST_KEY, host)
                                        putBoolean(FIRST_LAUNCH_KEY, false)
                                        apply()
                                    }
                                    firstLaunch = false
                                }

                                coroutineScope.launch(Dispatchers.IO) {
                                    val response = repository.sendSignalToRaspberry(host = host)
                                    snackbarHostState.showSnackbar(message = response.message)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SendButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 20.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth()
        )
    }
}