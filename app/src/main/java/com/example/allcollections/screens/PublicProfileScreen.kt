package com.example.allcollections.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.collection.UserCollection
import com.example.allcollections.viewModel.CollectionViewModel
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(userId: String, navController: NavController) {
    val viewModel: CollectionViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    var username by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var userCollections by remember { mutableStateOf(emptyList<UserCollection>()) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isFollowing by remember { mutableStateOf(false) }
    var followerCount by remember { mutableStateOf(0) }

    fun refreshProfile() {
        viewModel.getUsernameById(userId) { username = it }
        viewModel.getProfileImageById(userId) { profileImageUrl = it }
        viewModel.getCollectionsByUserId(userId) { userCollections = it }
        profileViewModel.isFollowing(currentUserId, userId) { result -> isFollowing = result }
        profileViewModel.getFollowerCount(userId) { count ->
            followerCount = count
        }
    }

    LaunchedEffect(userId) {
        refreshProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo utente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            profileImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto profilo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = username, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Follower: $followerCount")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFollowing) {
                        profileViewModel.unfollowUser(currentUserId, userId) {
                            if (it) {
                                refreshProfile()
                                profileViewModel.getFollowerCount(userId) { count ->
                                    followerCount = count
                                }
                            }
                        }
                    } else {
                        profileViewModel.followUser(currentUserId, userId) {
                            if (it) {
                                refreshProfile()
                                profileViewModel.getFollowerCount(userId) { count ->
                                    followerCount = count
                                }
                            }
                        }
                    }
                }
            ){
                Text(if (isFollowing) "Seguito ✓" else "Segui")
            }

            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(userCollections.size) { index ->
                    val collection = userCollections[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                navController.navigate("collectionDetail/${collection.id}")
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = collection.name, style = MaterialTheme.typography.titleSmall)
                            Text(text = collection.category ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}