package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TutorViewModelFactory
import com.example.viewmodel.TutorViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as TutorApp
    val factory = TutorViewModelFactory(app.repository)
    
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            val viewModel: TutorViewModel = viewModel(factory = factory)
            AppNavigation(viewModel)
        }
      }
    }
  }
}
