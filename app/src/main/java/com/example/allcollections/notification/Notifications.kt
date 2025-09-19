package com.example.allcollections.notification

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.allcollections.profile.UserData
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun Notifications(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }

    LaunchedEffect(userId) {
        userId?.let {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            viewModel.getNotifications(it) { result ->
                notifications = result
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notifiche", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        if (notifications.isEmpty()) {
            Text("Nessuna notifica ricevuta.")
        } else {
            notifications.forEach { item ->
                val user = item.user
                val timestamp = item.timestamp
                val read = item.read

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.markNotificationAsRead(item.notificationId)
                            notifications = notifications.map {
                                if (it.notificationId == item.notificationId) it.copy(read = true) else it
                            }
                        }
                        .padding(vertical = 8.dp)
                        .background(
                            if (!read) Color(0xFFE3F2FD) else Color.Transparent
                        ),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    val painter = rememberAsyncImagePainter(user.profileImageUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Foto profilo",
                        modifier = Modifier.size(40.dp)
                    )

                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${user.username}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    viewModel.markNotificationAsRead(item.notificationId)
                                    notifications = notifications.map {
                                        if (it.notificationId == item.notificationId) it.copy(read = true) else it
                                    }
                                    navController.navigate("publicProfile/${user.userId}")
                                }
                            )
                        }

                        Text(text = "Ti ha seguito il $timestamp", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}