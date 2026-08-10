package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAccent

@Composable
fun LoginScreen(
    onLoginSuccess: (UserProfile) -> Unit,
    onAdminPhoneLogin: (phone: String, otp: String) -> Boolean,
    onShopkeeperLogin: (idOrEmail: String, password: String) -> Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Admin Phone OTP, 1: Shopkeeper ID/Password

    // Admin Phone OTP State
    var adminPhoneInput by remember { mutableStateOf("+91 9876543210") }
    var adminOtpInput by remember { mutableStateOf("123456") }
    var isOtpSent by remember { mutableStateOf(false) }

    // Shopkeeper ID / Pass State
    var shopkeeperIdInput by remember { mutableStateOf("shopkeeper1@store.com") }
    var shopkeeperPasswordInput by remember { mutableStateOf("1234") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalBar,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sales ",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Android 14 Liquor Retail System with Firebase Realtime DB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Role Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldAccent
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = if (selectedTab == 0) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ADMIN (PHONE OTP)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = if (selectedTab == 1) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHOPKEEPER (ID/PASS)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 0) {
                // --- TAB 0: ADMIN PHONE OTP AUTH ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Admin Sign-In (Firebase Phone Auth)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )

                        OutlinedTextField(
                            value = adminPhoneInput,
                            onValueChange = { adminPhoneInput = it; errorMessage = null },
                            label = { Text("Admin Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldAccent) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent)
                        )

                        if (!isOtpSent) {
                            Button(
                                onClick = {
                                    if (adminPhoneInput.isNotBlank()) {
                                        isOtpSent = true
                                    } else {
                                        errorMessage = "Please enter Admin mobile number"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SEND OTP CODE (PHONE AUTH)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = adminOtpInput,
                                onValueChange = { if (it.length <= 6) adminOtpInput = it; errorMessage = null },
                                label = { Text("6-Digit OTP Verification Code") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldSuccess)
                            )

                            Button(
                                onClick = {
                                    val success = onAdminPhoneLogin(adminPhoneInput, adminOtpInput)
                                    if (success) {
                                        onLoginSuccess(UserProfile.ADMIN_USER.copy(phone = adminPhoneInput))
                                    } else {
                                        errorMessage = "Invalid OTP verification code!"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                            ) {
                                Text("VERIFY OTP & LOG IN AS ADMIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // --- TAB 1: SHOPKEEPER ID & PASSWORD AUTH ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Shopkeeper Login (Created by Admin in /users)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )

                        OutlinedTextField(
                            value = shopkeeperIdInput,
                            onValueChange = { shopkeeperIdInput = it; errorMessage = null },
                            label = { Text("Shopkeeper ID / Email / Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldAccent) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent)
                        )

                        OutlinedTextField(
                            value = shopkeeperPasswordInput,
                            onValueChange = { shopkeeperPasswordInput = it; errorMessage = null },
                            label = { Text("Password / PIN") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldAccent) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldAccent)
                        )

                        Button(
                            onClick = {
                                val success = onShopkeeperLogin(shopkeeperIdInput, shopkeeperPasswordInput)
                                if (success) {
                                    onLoginSuccess(
                                        UserProfile(
                                            id = shopkeeperIdInput,
                                            name = "Shopkeeper",
                                            role = UserRole.SHOPKEEPER,
                                            defaultPin = shopkeeperPasswordInput,
                                            email = shopkeeperIdInput
                                        )
                                    )
                                } else {
                                    errorMessage = "Invalid Shopkeeper ID or Password! Default demo password is '1234'"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                        ) {
                            Text("LOG IN AS SHOPKEEPER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick One-Tap Demo Login
            Text(
                text = "⚡ Quick Demo Admin Access (Bypass Auth)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        onLoginSuccess(UserProfile.ADMIN_USER)
                    }
                    .padding(8.dp)
            )
        }
    }
}
