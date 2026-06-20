package dev.mackenzie.coderemote.ui.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ScreenTest {

    @Test
    fun `route values preserve navigation contracts`() {
        assertEquals("home", Screen.Home.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("about", Screen.About.route)
        assertEquals(
            "server_settings?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&serverId={serverId}",
            Screen.ServerSettings.route
        )
        assertEquals(
            "server_providers?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&serverId={serverId}",
            Screen.ServerProviders.route
        )
        assertEquals(
            "server_model_filter?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&serverId={serverId}",
            Screen.ServerModelFilter.route
        )
        assertEquals(
            "sessions?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&serverId={serverId}",
            Screen.SessionList.route
        )
        assertEquals(
            "webview?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&initialPath={initialPath}",
            Screen.WebView.route
        )
        assertEquals(
            "chat?serverUrl={serverUrl}&username={username}&password={password}&serverName={serverName}&serverId={serverId}&sessionId={sessionId}&openTerminal={openTerminal}",
            Screen.Chat.route
        )
    }

    @Test
    fun `webview args preserve names order types and defaults`() {
        assertArguments(
            actual = Screen.WebView.args,
            expected = listOf(
                ExpectedArgument("serverUrl", NavType.StringType),
                ExpectedArgument("username", NavType.StringType),
                ExpectedArgument("password", NavType.StringType),
                ExpectedArgument("serverName", NavType.StringType),
                ExpectedArgument("initialPath", NavType.StringType, defaultValue = "")
            )
        )
    }

    @Test
    fun `chat args preserve names order types and defaults`() {
        assertArguments(
            actual = Screen.Chat.args,
            expected = listOf(
                ExpectedArgument("serverUrl", NavType.StringType),
                ExpectedArgument("username", NavType.StringType),
                ExpectedArgument("password", NavType.StringType),
                ExpectedArgument("serverName", NavType.StringType),
                ExpectedArgument("serverId", NavType.StringType),
                ExpectedArgument("sessionId", NavType.StringType),
                ExpectedArgument("openTerminal", NavType.BoolType, defaultValue = false)
            )
        )
    }

    @Test
    fun `createRoute preserves utf8 url encoding and boolean argument values`() {
        val serverUrl = "https://example.com/a path?q=hello&lang=es"
        val username = "mac+kenzie"
        val password = "p@ss word/ñ"
        val serverName = "Server & Café"
        val serverId = "srv/id 42"
        val initialPath = "/workspace/Code Remote/session/abc?tab=chat&emoji=☕"
        val sessionId = "session/abc 123?mode=terminal"

        assertEquals(
            "webview?serverUrl=https%3A%2F%2Fexample.com%2Fa%20path%3Fq%3Dhello%26lang%3Des" +
                "&username=mac%2Bkenzie" +
                "&password=p%40ss%20word%2F%C3%B1" +
                "&serverName=Server%20%26%20Caf%C3%A9" +
                "&initialPath=%2Fworkspace%2FCode%20Remote%2Fsession%2Fabc%3Ftab%3Dchat%26emoji%3D%E2%98%95",
            Screen.WebView.createRoute(
                serverUrl = serverUrl,
                username = username,
                password = password,
                serverName = serverName,
                initialPath = initialPath
            )
        )

        assertEquals(
            "chat?serverUrl=https%3A%2F%2Fexample.com%2Fa%20path%3Fq%3Dhello%26lang%3Des" +
                "&username=mac%2Bkenzie" +
                "&password=p%40ss%20word%2F%C3%B1" +
                "&serverName=Server%20%26%20Caf%C3%A9" +
                "&serverId=srv%2Fid%2042" +
                "&sessionId=session%2Fabc%20123%3Fmode%3Dterminal" +
                "&openTerminal=true",
            Screen.Chat.createRoute(
                serverUrl = serverUrl,
                username = username,
                password = password,
                serverName = serverName,
                serverId = serverId,
                sessionId = sessionId,
                openTerminal = true
            )
        )
    }

    @Test
    fun `createRoute encodes plus and spaces for navigation query decoding`() {
        val route = Screen.Chat.createRoute(
            serverUrl = "https://example.com/a path",
            username = "mac+kenzie",
            password = "p ss+word",
            serverName = "Server Name+Prod",
            serverId = "srv 42+prod",
            sessionId = "session + one",
            openTerminal = false
        )

        assertEquals(
            "chat?serverUrl=https%3A%2F%2Fexample.com%2Fa%20path" +
                "&username=mac%2Bkenzie" +
                "&password=p%20ss%2Bword" +
                "&serverName=Server%20Name%2BProd" +
                "&serverId=srv%2042%2Bprod" +
                "&sessionId=session%20%2B%20one" +
                "&openTerminal=false",
            route
        )
    }

    private fun assertArguments(
        actual: List<NamedNavArgument>,
        expected: List<ExpectedArgument>
    ) {
        assertEquals(expected.map { it.name }, actual.map { it.name })

        expected.zip(actual).forEach { (expectedArgument, actualArgument) ->
            assertSame(expectedArgument.name, expectedArgument.type, actualArgument.argument.type)
            assertEquals(
                expectedArgument.name,
                expectedArgument.defaultValue != null,
                actualArgument.argument.isDefaultValuePresent
            )
            if (expectedArgument.defaultValue != null) {
                assertEquals(
                    expectedArgument.name,
                    expectedArgument.defaultValue,
                    actualArgument.argument.defaultValue
                )
            }
        }
    }

    private data class ExpectedArgument(
        val name: String,
        val type: NavType<*>,
        val defaultValue: Any? = null
    )
}
