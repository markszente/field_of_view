package com.example.field_of_view

import kotlin.test.Test
import kotlin.test.assertNotNull

/*
 * This demonstrates a simple unit test of the Kotlin portion of this plugin's implementation.
 *
 * Once you have built the plugin's example app, you can run these tests from the command
 * line by running `./gradlew testDebugUnitTest` in the `example/android/` directory, or
 * you can run them directly from IDEs that support JUnit such as Android Studio.
 */

internal class FieldOfViewPluginTest {
    @Test
    fun fieldOfViewResponse_createsCorrectly() {
        val response = FieldOfViewResponse(
            horizontalFov = 60.0,
            verticalFov = 45.0
        )

        assertNotNull(response)
        assert(response.horizontalFov == 60.0)
        assert(response.verticalFov == 45.0)
    }

    @Test
    fun fieldOfViewResponse_toList_returnsCorrectValues() {
        val response = FieldOfViewResponse(
            horizontalFov = 70.5,
            verticalFov = 52.3
        )

        val list = response.toList()

        assert(list.size == 2)
        assert(list[0] == 70.5)
        assert(list[1] == 52.3)
    }

    @Test
    fun fieldOfViewResponse_fromList_createsCorrectly() {
        val list = listOf<Any?>(65.0, 48.5)

        val response = FieldOfViewResponse.fromList(list)

        assert(response.horizontalFov == 65.0)
        assert(response.verticalFov == 48.5)
    }

    @Test
    fun fieldOfViewResponse_equality_worksCorrectly() {
        val response1 = FieldOfViewResponse(horizontalFov = 60.0, verticalFov = 45.0)
        val response2 = FieldOfViewResponse(horizontalFov = 60.0, verticalFov = 45.0)
        val response3 = FieldOfViewResponse(horizontalFov = 70.0, verticalFov = 50.0)

        assert(response1 == response2)
        assert(response1 != response3)
    }
}
