package com.quran.halaqareport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quran.halaqareport.ui.*
import com.quran.halaqareport.viewmodel.HalaqaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val halaqaViewModel: HalaqaViewModel = viewModel()
            val profile by halaqaViewModel.profile.collectAsState()
            val statusMessage by halaqaViewModel.statusMessage.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(statusMessage) {
                statusMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    halaqaViewModel.clearStatusMessage()
                }
            }

            HalaqaReportTheme(darkTheme = profile.darkThemeEnabled) {
                var currentTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("${profile.appCustomTitle} - ${profile.halaqaName}") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("الرئيسية") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.People, contentDescription = null) },
                                label = { Text("الطلاب") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                                label = { Text("جلسة اليوم") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                                label = { Text("التقارير") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> DashboardScreen(viewModel = halaqaViewModel, onNavigateToQuickSession = { currentTab = 2 })
                            1 -> StudentsScreen(viewModel = halaqaViewModel)
                            2 -> QuickSessionScreen(viewModel = halaqaViewModel)
                            3 -> ReportsAndSettingsScreen(viewModel = halaqaViewModel)
                        }
                    }
                }
            }
        }
    }
}
