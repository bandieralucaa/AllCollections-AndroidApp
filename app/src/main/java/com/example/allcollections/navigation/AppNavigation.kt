package com.example.allcollections.navigation

import androidx.compose.runtime.Composable
import com.example.allcollections.ui.theme.ThemeMode
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.allcollections.collection.AddCollection
import com.example.allcollections.collection.AddImageCollection
import com.example.allcollections.collection.AddObjectCollection
import com.example.allcollections.collection.CollectionDetail
import com.example.allcollections.collection.MyCollections
import com.example.allcollections.login.Login
import com.example.allcollections.login.Register
import com.example.allcollections.profile.EditPhotoProfile
import com.example.allcollections.profile.EditProfile
import com.example.allcollections.profile.PhotoProfile
import com.example.allcollections.profile.Profile
import com.example.allcollections.screens.Chat
import com.example.allcollections.screens.ChooseTheme
import com.example.allcollections.screens.Home
import com.example.allcollections.screens.Search
import com.example.allcollections.screens.Settings
import com.example.allcollections.viewModel.ProfileViewModel
import com.example.allcollections.viewModel.ThemeState
import com.example.allcollections.viewModel.ViewModelContainer


@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModelContainer: ViewModelContainer,
    startDestination: String,
    state: ThemeState,
    onThemeSelected: (ThemeMode) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val profileViewModel: ProfileViewModel = viewModel()


    Scaffold(
        bottomBar = {
            if (currentDestination?.route !in listOf(Screens.Login.name, Screens.Register.name)) {
                NavigationBar(
                    modifier = Modifier.height(55.dp)
                ) {
                    listOfNavItems.forEach { navItem ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == navItem.route } == true,
                            onClick = {
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = navItem.icon,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    ) {
            paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route = Screens.Login.name) {
                Login(navController, viewModelContainer.profileViewModel)
            }
            composable(route = Screens.Home.name) {
                Home()
            }
            composable(route = Screens.Profile.name) {
                Profile(navController)
            }
            composable(route = Screens.Chat.name) {
                Chat()
            }
            composable(route = Screens.SearchPage.name) {
                Search()
            }
            composable(route = Screens.Settings.name) {
                Settings(navController, viewModelContainer.profileViewModel)
            }
            composable(route = Screens.Register.name) {
                Register(navController, profileViewModel)
            }
            composable(route = Screens.AddCollection.name) {
                AddCollection(navController)
            }
            composable(route = Screens.MyCollections.name) {
                MyCollections(navController, viewModelContainer.collectionViewModel)
            }
            composable(route = "${Screens.CollectionDetail.name}/{collectionId}") { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                CollectionDetail(navController, collectionId, viewModelContainer.collectionViewModel)
            }
            composable(route = "${Screens.PhotoProfile.name}/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                PhotoProfile(navController, userId, profileViewModel)
            }
            composable(route = "${Screens.AddObjectCollection.name}/{collectionId}") { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                AddObjectCollection(navController = navController, collectionId = collectionId, viewModelContainer.collectionViewModel)
            }
            composable(route = Screens.ChooseTheme.name) {
                ChooseTheme(state, onThemeSelected, navController)
            }
            composable(route = Screens.EditProfile.name) {
                EditProfile(navController)
            }
            composable(route = "${Screens.AddImageCollection.name}/{collectionId}") { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                AddImageCollection(collectionId, navController, viewModelContainer.collectionViewModel)
            }
            composable(route = Screens.EditPhotoProfile.name) {
                EditPhotoProfile(navController, viewModelContainer.profileViewModel)
            }

        }
    }
}