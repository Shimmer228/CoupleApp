package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.presentation.viewmodel.PairViewModel

@Composable
fun PairScreen(
    onPairComplete: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: PairViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pair Setup",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Create a pair to receive a join code, or enter your partner's code to connect.",
            style = MaterialTheme.typography.bodyLarge
        )

        viewModel.createdJoinCode.value?.let { joinCode ->
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your pair code",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = joinCode,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Share this code with your partner, then continue to Home.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::createPair,
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading.value && viewModel.currentPairId.value == null
        ) {
            Text(if (viewModel.isLoading.value) "Loading..." else "Create Pair")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = viewModel.joinCode.value,
            onValueChange = viewModel::onJoinCodeChange,
            label = { Text("Join code") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading.value && viewModel.currentPairId.value == null
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.joinPair(onPairComplete) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading.value && viewModel.currentPairId.value == null
        ) {
            Text(if (viewModel.isLoading.value) "Loading..." else "Join Pair")
        }

        viewModel.error.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        viewModel.successMessage.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.currentPairId.value != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.clearMessage()
                    onPairComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Home")
            }
        }
    }
}
