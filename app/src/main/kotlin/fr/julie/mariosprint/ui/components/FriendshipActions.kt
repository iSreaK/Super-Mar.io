package fr.julie.mariosprint.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.julie.mariosprint.R
import fr.julie.mariosprint.domain.model.FriendCommand
import fr.julie.mariosprint.domain.model.FriendshipStatus
import fr.julie.mariosprint.ui.model.FriendUi

@Composable
internal fun FriendshipActions(playerUid: String, relation: FriendUi?, enabled: Boolean,
    onCommand: (FriendCommand) -> Unit) {
    when (relation?.status) {
        null -> PrimaryAction(stringResource(R.string.friends_add), { onCommand(FriendCommand.SendToPlayer(playerUid)) }, enabled = enabled)
        FriendshipStatus.INCOMING -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCommand(FriendCommand.Accept(relation.id)) }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.friends_accept)) }
            OutlinedButton(onClick = { onCommand(FriendCommand.Decline(relation.id)) }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.friends_decline)) }
        }
        FriendshipStatus.OUTGOING -> SecondaryAction(stringResource(R.string.friends_cancel),
            { onCommand(FriendCommand.Cancel(relation.id)) }, enabled = enabled)
        FriendshipStatus.FRIEND -> TextButton(onClick = { onCommand(FriendCommand.Remove(relation.id)) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.friends_remove)) }
    }
}

@Composable
internal fun RemoveFriendDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.friends_remove_title)) },
        text = { Text(stringResource(R.string.friends_remove_description, name)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.friends_remove)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.friends_keep)) } })
}
