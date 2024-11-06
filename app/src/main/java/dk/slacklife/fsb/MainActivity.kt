package dk.slacklife.fsb

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import dk.slacklife.fsb.authentication.AuthState
import dk.slacklife.fsb.authentication.AuthenticationError
import dk.slacklife.fsb.authentication.LoginScreenViewmodel
import dk.slacklife.fsb.authentication.appModule
import dk.slacklife.fsb.authentication.networkModule
import dk.slacklife.fsb.ui.theme.FsbTheme
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule, networkModule)
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier, viewmodel: LoginScreenViewmodel = koinViewModel()) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (password, setPassword) = remember { mutableStateOf("") }
        val (email, setEmail) = remember { mutableStateOf("") }
        val state = viewmodel.authState.collectAsState()
        when (state.value) {
            AuthState.Authorized -> Text("Authorized")
            AuthState.Unauthorized -> Text("Not authorized yet")
            is AuthState.Error -> when ((state.value as AuthState.Error).err) {
                AuthenticationError.NotAcceptable -> Text("Not acceptable")
                AuthenticationError.Unauthorized -> Text("unauthorized")
                AuthenticationError.InternalServerError -> Text("internal error")
                AuthenticationError.ConnectionError -> Text("connection error")
                AuthenticationError.UnknownError -> Text("unknown error")
            }
        }
        Text(
            text = "FSB",
            fontSize = TextUnit.Unspecified
        )
        TextField(
            value = email,
            onValueChange = setEmail,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
            )
        )
        TextField(
            value = password,
            onValueChange = setPassword,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
            )
        )
        Button(onClick = { viewmodel.login(email, password) }) {
            Text(text = "Login")
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FsbTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(modifier = Modifier.padding(it))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FsbTheme {
        Greeting("Android")
    }
}