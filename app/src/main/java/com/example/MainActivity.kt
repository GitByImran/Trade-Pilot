package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TradePilotApp
import com.example.ui.TradePilotViewModel
import com.example.ui.theme.TradePilotTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TradePilotTheme {
        val viewModel: TradePilotViewModel = viewModel()
        TradePilotApp(viewModel = viewModel)
      }
    }
  }
}
