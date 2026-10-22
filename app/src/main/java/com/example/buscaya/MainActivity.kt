package com.example.buscaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.buscaya.data.models.BusinessResult
import androidx.activity.viewModels
import com.example.buscaya.ui.screens.DetailScreen
import com.example.buscaya.ui.screens.HomeScreenContent
import com.example.buscaya.ui.theme.BuscayaTheme
import com.example.buscaya.ui.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuscayaTheme {
                var selectedBusiness by remember { mutableStateOf<BusinessResult?>(null) }

                // Intercept back gesture if a business is selected
                BackHandler(enabled = selectedBusiness != null) {
                    selectedBusiness = null
                }

                AnimatedContent(
                    targetState = selectedBusiness,
                    transitionSpec = {
                        if (targetState != null) {
                            // Slide in from right when going to detail screen
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            // Slide in from left when returning to search screen
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "navigationTransition"
                ) { business ->
                    if (business != null) {
                        DetailScreen(
                            business = business,
                            onBack = { selectedBusiness = null }
                        )
                    } else {
                        HomeScreenContent(
                            viewModel = viewModel,
                            onBusinessSelected = { selectedBusiness = it }
                        )
                    }
                }
            }
        }
    }
}
