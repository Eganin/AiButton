package com.goulash.buttonforaicamera

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
            requestTimeoutMillis = 45000
            socketTimeoutMillis = 45000
            connectTimeoutMillis = 45000
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
    suspend fun sendSignalToRaspberry(): Response {
        val response = try {
            httpClient.post("http://192.168.1.198:8080/take_screenshot") {
                setBody(OrderBody(orderId = 1000))
            }.body<Response>()
        } catch (e: Exception) {
            Response(status = "error", message = "Произошла ошибка")
        }
        return response
    }
}

private val repository = Repository()

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            ButtonForAiCameraTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        SendButton(
                            text = "",
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SendButton(text: String, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    Button(
        onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                val response = repository.sendSignalToRaspberry()
                snackbarHostState.showSnackbar(message = response.message)
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 20.dp, pressedElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth()
        )
    }
}