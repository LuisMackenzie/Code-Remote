package dev.mackenzie.coderemote.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLEncoder

/**
 * Navigation routes for the app
 */
sealed class Screen(
    internal val baseRoute: String,
    private val navArgs: List<ScreenArg> = emptyList()
) {
    data object Home : Screen("home")
    
    data object WebView : Screen(
        baseRoute = "webview",
        navArgs = listOf(
            ScreenArg.ServerUrl,
            ScreenArg.Username,
            ScreenArg.Password,
            ScreenArg.ServerName,
            ScreenArg.InitialPath
        )
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            initialPath: String = ""
        ): String = createQueryRoute(
            ScreenArg.ServerUrl to serverUrl,
            ScreenArg.Username to username,
            ScreenArg.Password to password,
            ScreenArg.ServerName to serverName,
            ScreenArg.InitialPath to initialPath
        )
    }
    
    data object SessionList : Screen(
        baseRoute = "sessions",
        navArgs = serverConnectionArgs
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            serverId: String
        ): String = createServerConnectionRoute(
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            serverId = serverId
        )
    }
    
    data object Chat : Screen(
        baseRoute = "chat",
        navArgs = serverConnectionArgs + listOf(ScreenArg.SessionId, ScreenArg.OpenTerminal)
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            serverId: String,
            sessionId: String,
            openTerminal: Boolean = false,
        ): String = createServerConnectionRoute(
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            serverId = serverId,
            ScreenArg.SessionId to sessionId,
            ScreenArg.OpenTerminal to openTerminal.toString()
        )
    }

    data object ServerSettings : Screen(
        baseRoute = "server_settings",
        navArgs = serverConnectionArgs
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            serverId: String
        ): String = createServerConnectionRoute(
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            serverId = serverId
        )
    }

    data object ServerProviders : Screen(
        baseRoute = "server_providers",
        navArgs = serverConnectionArgs
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            serverId: String
        ): String = createServerConnectionRoute(
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            serverId = serverId
        )
    }

    data object ServerModelFilter : Screen(
        baseRoute = "server_model_filter",
        navArgs = serverConnectionArgs
    ) {
        fun createRoute(
            serverUrl: String,
            username: String,
            password: String,
            serverName: String,
            serverId: String
        ): String = createServerConnectionRoute(
            serverUrl = serverUrl,
            username = username,
            password = password,
            serverName = serverName,
            serverId = serverId
        )
    }
    
    data object Settings : Screen("settings")
    data object About : Screen("about")

    val route = run {
        if (navArgs.isEmpty()) {
            baseRoute
        } else {
            navArgs.joinToString(
                prefix = "$baseRoute?",
                separator = "&"
            ) { arg -> "${arg.key}={${arg.key}}" }
        }
    }

    val args = navArgs.map { it.toNavArgument() }

    protected fun createServerConnectionRoute(
        serverUrl: String,
        username: String,
        password: String,
        serverName: String,
        serverId: String,
        vararg extraArgs: Pair<ScreenArg, String>
    ): String = createQueryRoute(
        ScreenArg.ServerUrl to serverUrl,
        ScreenArg.Username to username,
        ScreenArg.Password to password,
        ScreenArg.ServerName to serverName,
        ScreenArg.ServerId to serverId,
        *extraArgs
    )

    protected fun createQueryRoute(vararg args: Pair<ScreenArg, String>): String {
        if (args.isEmpty()) return baseRoute

        return args.joinToString(
            prefix = "$baseRoute?",
            separator = "&"
        ) { (arg, value) -> "${arg.key}=${value.encodeForRoute()}" }
    }

    private fun String.encodeForRoute(): String = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    companion object {
        private val serverConnectionArgs = listOf(
            ScreenArg.ServerUrl,
            ScreenArg.Username,
            ScreenArg.Password,
            ScreenArg.ServerName,
            ScreenArg.ServerId
        )
    }
}

enum class ScreenArg(
    val key: String,
    val navType: NavType<*>,
    private val argumentDefaultValue: Any? = null
) {
    ServerUrl("serverUrl", NavType.StringType),
    Username("username", NavType.StringType),
    Password("password", NavType.StringType),
    ServerName("serverName", NavType.StringType),
    ServerId("serverId", NavType.StringType),
    InitialPath("initialPath", NavType.StringType, argumentDefaultValue = ""),
    SessionId("sessionId", NavType.StringType),
    OpenTerminal("openTerminal", NavType.BoolType, argumentDefaultValue = false);

    fun toNavArgument() = navArgument(key) {
        type = navType
        if (argumentDefaultValue != null) {
            defaultValue = argumentDefaultValue
        }
    }
}
