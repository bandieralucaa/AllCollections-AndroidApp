package com.example.allcollections.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.allcollections.login.RequireLogin
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.allcollections.profile.FollowType

@Composable
fun Profile(navController: NavController) {
    RequireLogin(navController) {
        ProfileContent(navController = navController)
    }
}

@Composable
fun ProfileContent(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel()
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val uid = currentUser?.uid
    var username by remember { mutableStateOf("") }
    val profileImageUrl by remember { viewModel.profileImageUrl }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var showFollowDialog by remember { mutableStateOf(false) }
    var followDialogType by remember { mutableStateOf(FollowType.FOLLOWERS) }
    var followList by remember { mutableStateOf<List<UserData>>(emptyList()) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screens.Login.name) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            username = withContext(Dispatchers.IO) { viewModel.getUsername() }
            viewModel.getProfileImage()
            uid?.let {
                viewModel.getFollowerCount(it) { count -> followerCount = count }
                viewModel.getFollowingCount(it) { count -> followingCount = count }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                navController.navigate(Screens.Settings.name)
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Icona Impostazioni"
                )
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        profileImageUrl?.let { imageUrl ->
            val painter = rememberImagePainter(imageUrl)
            Image(
                painter = painter,
                contentDescription = "Immagine del profilo",
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = username,
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Follower: $followerCount",
                modifier = Modifier.clickable {
                    uid?.let {
                        followDialogType = FollowType.FOLLOWERS
                        viewModel.getFollowList(it, FollowType.FOLLOWERS) { list ->
                            followList = list
                            showFollowDialog = true
                        }
                    }
                }
            )

            Text(
                text = "Seguiti: $followingCount",
                modifier = Modifier.clickable {
                    uid?.let {
                        followDialogType = FollowType.FOLLOWING
                        viewModel.getFollowList(it, FollowType.FOLLOWING) { list ->
                            followList = list
                            showFollowDialog = true
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navController.navigate(Screens.MyCollections.name)
        }) {
            Text(text = "Vedi le tue collezioni")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate(Screens.AddCollection.name)
        }) {
            Text(text = "Crea una nuova collezione")
        }
    }

    if (showFollowDialog) {
        AlertDialog(
            onDismissRequest = { showFollowDialog = false },
            title = {
                Text(text = if (followDialogType == FollowType.FOLLOWERS) "Follower" else "Seguiti")
            },
            text = {
                if (followList.isEmpty()) {
                    Text("Nessun utente trovato.")
                } else {
                    Column {
                        followList.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val painter = rememberAsyncImagePainter(user.profileImageUrl)
                                Image(
                                    painter = painter,
                                    contentDescription = "Foto profilo di ${user.username}",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(end = 8.dp)
                                )
                                Column {
                                    Text(text = "${user.name} ${user.surname}", fontSize = 16.sp)
                                    Text(
                                        text = "@${user.username}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable{
                                            navController.navigate("publicProfile/${user.userId}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFollowDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

}