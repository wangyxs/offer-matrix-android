package com.example.offermatrix.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun DigitalHumanPlayer(modifier: Modifier = Modifier, isSpeaking: Boolean) {
    Box(modifier = modifier.background(Color.DarkGray)) {
        Text("Digital Human", modifier = Modifier.align(Alignment.Center), color = Color.White)
    }
}