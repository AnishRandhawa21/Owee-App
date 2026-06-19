package com.anish.owee.ui.screen.friend.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.anish.owee.ui.theme.SurfaceVariant
import com.anish.owee.ui.theme.TextSecondary

@Composable
fun FriendSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search by username",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = TextSecondary
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = SurfaceVariant,
            disabledContainerColor = SurfaceVariant,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}
