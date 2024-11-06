package dk.slacklife.fsb.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.flatMap
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

interface AuthenticationRepository {
    suspend fun login(username: String, password: String): Either<AuthenticationError, Unit>
}

enum class AuthenticationError {
    NotAcceptable,
    Unauthorized,
    InternalServerError,
    ConnectionError,
    UnknownError,
}

sealed class AuthState {
    data object Unauthorized : AuthState()
    data object Authorized : AuthState()
    data class Error(val err: AuthenticationError) : AuthState()
}

class LoginScreenViewmodel(private val authService: AuthService) : ViewModel() {
    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Unauthorized)
    val authState = _authState.asStateFlow()

    fun login(username: String, password: String) = viewModelScope.launch {
        authService.login(username, password).fold(
            { err ->
                _authState.value = AuthState.Error(err)
            }, {
                _authState.value = AuthState.Authorized
            }
        )
    }
}

@Serializable
data class User(val username: String, val password: String)

class AuthenticationRepositoryImpl(private val client: HttpClient) : AuthenticationRepository {
    override suspend fun login(
        username: String,
        password: String
    ): Either<AuthenticationError, Unit> = Either.catch {
        client.post {
            contentType(ContentType.Application.Json)
            setBody(User(username = username, password = password))
            url {
                protocol = URLProtocol.HTTPS
                host = "fsb.slacklife.dk"
                path("login")
            }
        }
    }.mapLeft {
        println(it.message)
        AuthenticationError.ConnectionError
    }.flatMap { r ->
        when (r.status) {
            OK -> Either.Right(Unit)
            NotAcceptable -> Either.Left(AuthenticationError.NotAcceptable)
            InternalServerError -> Either.Left(AuthenticationError.InternalServerError)
            Unauthorized -> Either.Left(AuthenticationError.Unauthorized)
            else -> Either.Left(AuthenticationError.UnknownError)
        }
    }
}

val appModule = module {
    singleOf(::AuthenticationRepositoryImpl) { bind<AuthenticationRepository>() }
    singleOf(::AuthService)
    viewModel { LoginScreenViewmodel(get()) }
}

class AuthService(private val repo: AuthenticationRepository) {
    suspend fun login(username: String, password: String): Either<AuthenticationError, Unit> =
        repo.login(username, password)
}

val networkModule = module {
    single {
        HttpClient(Android) {
            engine {
                connectTimeout = 100_000
                socketTimeout = 100_000
            }
            install(HttpCookies)
            install(ContentNegotiation) {
                json()
            }
        }
    }
}