package com.vitorpamplona.amethyst.ui.note

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.LnZapEvent
import com.vitorpamplona.amethyst.ui.actions.CloseButton
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.TextSpinner
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZapOptionstViewModel : ViewModel() {
    private var account: Account? = null

    var customAmount by mutableStateOf(TextFieldValue("21"))
    var customMessage by mutableStateOf(TextFieldValue(""))

    fun load(account: Account) {
        this.account = account
    }

    fun canSend(): Boolean {
        return value() != null
    }

    fun value(): Long? {
        return try {
            customAmount.text.trim().toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun cancel() {
    }
}

@Composable
fun ZapCustomDialog(onClose: () -> Unit, accountViewModel: AccountViewModel, baseNote: Note) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postViewModel: ZapOptionstViewModel = viewModel()

    LaunchedEffect(accountViewModel) {
        postViewModel.load(accountViewModel.account)
    }

    var zappingProgress by remember { mutableStateOf(0f) }

    val zapTypes = listOf(
        Triple(LnZapEvent.ZapType.PUBLIC, stringResource(id = R.string.zap_type_public), stringResource(id = R.string.zap_type_public_explainer)),
        Triple(LnZapEvent.ZapType.PRIVATE, stringResource(id = R.string.zap_type_private), stringResource(id = R.string.zap_type_private_explainer)),
        Triple(LnZapEvent.ZapType.ANONYMOUS, stringResource(id = R.string.zap_type_anonymous), stringResource(id = R.string.zap_type_anonymous_explainer)),
        Triple(LnZapEvent.ZapType.NONZAP, stringResource(id = R.string.zap_type_nonzap), stringResource(id = R.string.zap_type_nonzap_explainer))
    )

    val zapOptions = remember { zapTypes.map { it.second }.toImmutableList() }
    val zapOptionExplainers = remember { zapTypes.map { it.third }.toImmutableList() }
    var selectedZapType by remember(accountViewModel) { mutableStateOf(accountViewModel.account.defaultZapType) }

    Dialog(
        onDismissRequest = { onClose() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface() {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CloseButton(onCancel = {
                        postViewModel.cancel()
                        onClose()
                    })

                    ZapButton(
                        isActive = postViewModel.canSend()
                    ) {
                        scope.launch(Dispatchers.IO) {
                            accountViewModel.zap(
                                baseNote,
                                postViewModel.value()!! * 1000L,
                                null,
                                postViewModel.customMessage.text,
                                context,
                                onError = {
                                    zappingProgress = 0f
                                    scope.launch {
                                        Toast
                                            .makeText(context, it, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onProgress = {
                                    scope.launch(Dispatchers.Main) {
                                        zappingProgress = it
                                    }
                                },
                                zapType = selectedZapType
                            )
                        }
                        onClose()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        // stringResource(R.string.new_amount_in_sats
                        label = { Text(text = stringResource(id = R.string.amount_in_sats)) },
                        value = postViewModel.customAmount,
                        onValueChange = {
                            postViewModel.customAmount = it
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Number
                        ),
                        placeholder = {
                            Text(
                                text = "100, 1000, 5000",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .weight(1f)
                    )

                    TextSpinner(
                        label = stringResource(id = R.string.zap_type),
                        placeholder = zapTypes.filter { it.first == accountViewModel.account.defaultZapType }.first().second,
                        options = zapOptions,
                        explainers = zapOptionExplainers,
                        onSelect = {
                            selectedZapType = zapTypes[it].first
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 5.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        // stringResource(R.string.new_amount_in_sats
                        label = {
                            if (selectedZapType == LnZapEvent.ZapType.PUBLIC || selectedZapType == LnZapEvent.ZapType.ANONYMOUS) {
                                Text(text = stringResource(id = R.string.custom_zaps_add_a_message))
                            } else if (selectedZapType == LnZapEvent.ZapType.PRIVATE) {
                                Text(text = stringResource(id = R.string.custom_zaps_add_a_message_private))
                            } else if (selectedZapType == LnZapEvent.ZapType.NONZAP) {
                                Text(text = stringResource(id = R.string.custom_zaps_add_a_message_nonzap))
                            }
                        },
                        value = postViewModel.customMessage,
                        onValueChange = {
                            postViewModel.customMessage = it
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Text
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.custom_zaps_add_a_message_example),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ZapButton(isActive: Boolean, onPost: () -> Unit) {
    Button(
        onClick = { onPost() },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = if (isActive) MaterialTheme.colors.primary else Color.Gray
            )
    ) {
        Text(text = "⚡Zap ", color = Color.White)
    }
}
