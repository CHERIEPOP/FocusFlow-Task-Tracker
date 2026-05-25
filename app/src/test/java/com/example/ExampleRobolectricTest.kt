package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TaskViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daily Task Tracker", appName)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `initialize TaskViewModel and app database`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TaskViewModel(app)
    assertNotNull(viewModel)
  }
}
