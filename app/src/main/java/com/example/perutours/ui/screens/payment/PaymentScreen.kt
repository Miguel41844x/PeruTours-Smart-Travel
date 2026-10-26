package com.example.perutours.ui.screens.payment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.perutours.data.repository.PaymentUploadState
import com.example.perutours.ui.screens.quotation.formatQuotationAmount
import com.example.perutours.ui.theme.PeruGold40
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    quotationId: String,
    reservationId: String,
    totalAmount: Double,
    currency: String,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PaymentViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCameraX by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // HU07: la reserva se consulta en Firestore para validar el estado
    // y mostrar el importe real del pago.
    LaunchedEffect(reservationId) {
        viewModel.loadReservation(reservationId)
    }

    // Selector de Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onVoucherCaptured(it) }
    }

    // Permiso de Cámara para CameraX
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraX = true
        } else {
            // HU07: el cliente debe saber que sin permiso puede usar la
            // galería en su lugar; antes el error quedaba silencioso.
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Permiso de cámara denegado. Puedes elegir el comprobante desde la galería."
                )
            }
        }
    }

    if (showCameraX) {
        CameraXPaymentDialog(
            onDismiss = { showCameraX = false },
            onPhotoCaptured = { uri ->
                showCameraX = false
                viewModel.onVoucherCaptured(uri)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Comprobante de Pago") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total a Pagar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // El importe autoritativo viene de la reserva en Firestore;
                    // el recibido por navegación solo se muestra como respaldo.
                    Text(
                        text = uiState.reservationAmount
                            ?.let { formatQuotationAmount(it, uiState.reservationCurrency) }
                            ?: formatQuotationAmount(totalAmount, currency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PeruGold40
                    )
                    if (uiState.isLoadingReservation) {
                        Text(
                            text = "Verificando el importe de la reserva...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Adjunta tu comprobante físico o voucher digital para validar tu reserva.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // HU07: selector real del método de pago (Yape, Plin o
            // Transferencia BCP); sin selección no se puede enviar.
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Método de pago",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.paymentMethods.forEach { method ->
                        FilterChip(
                            selected = uiState.paymentMethod == method,
                            onClick = { viewModel.onPaymentMethodChange(method) },
                            label = { Text(method, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de Cámara (CameraX) y Galería
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            showCameraX = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cámara")
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vista previa del comprobante
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.voucherUri != null) {
                    AsyncImage(
                        model = uiState.voucherUri,
                        contentDescription = "Comprobante de pago",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Ningún comprobante seleccionado aún",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val uploadState = uiState.uploadState) {
                // Estado inicial: botón de envío con validación completa.
                PaymentUploadState.Idle -> Button(
                    onClick = { viewModel.submitPayment(quotationId, reservationId) },
                    enabled = uiState.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                ) {
                    if (uiState.isPreparing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Confirmar y Registrar Pago", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // HU07: seguimiento en tiempo real del Worker.
                PaymentUploadState.Enqueued, PaymentUploadState.Uploading -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PeruGold40)
                        Text(
                            text = if (uploadState is PaymentUploadState.Enqueued) {
                                "En cola para subir el comprobante..."
                            } else {
                                "Subiendo comprobante..."
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            text = "Puedes cerrar la app; la subida continúa en segundo plano.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Comprobante enviado: queda a la espera de la revisión
                // del agente (Confirmada → Pago enviado).
                PaymentUploadState.Uploaded -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                        Text(
                            text = "Comprobante enviado",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Se ha emviado correctamente el comprobante. El agente verificará el pago y te notificaremos la decisión.",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = onPaymentSuccess,
                            modifier = Modifier.padding(top = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Text("Aceptar y Volver")
                        }
                    }
                }

                // Error definitivo (tras los reintentos del Worker): se
                // puede reintentar desde la misma pantalla.
                is PaymentUploadState.Error -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No se pudo enviar el comprobante",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                        Text(
                            text = uploadState.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = { viewModel.retryUpload(quotationId, reservationId) },
                            enabled = uiState.canSubmit,
                            modifier = Modifier.padding(top = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                        ) {
                            Text("Reintentar envío")
                        }
                    }
                }
            }
        }
    }
}

// Diálogo con CameraX nativo de Jetpack
@Composable
private fun CameraXPaymentDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView
        if (view == null) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            var cameraProvider: ProcessCameraProvider? = null
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0)
                        .build()

                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    cameraProvider = provider
                    imageCapture = capture
                } catch (_: Exception) {
                    // HU07: si la cámara no está disponible, el cliente
                    // ve el motivo en lugar de un diálogo mudo.
                    cameraErrorMessage =
                        "No se pudo iniciar la cámara. Puedes elegir el comprobante desde la galería."
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                cameraProvider?.unbindAll()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enfoca tu Comprobante", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                cameraErrorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            PreviewView(viewContext).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }.also { previewView = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            val tempFile = File(context.cacheDir, "temp_voucher_${UUID.randomUUID()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            isCapturing = true
                            cameraErrorMessage = null
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        onPhotoCaptured(Uri.fromFile(tempFile))
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        // HU07: el fallo de captura se comunica
                                        // en lugar de descartarse en silencio.
                                        cameraErrorMessage =
                                            "No se pudo capturar la foto. Intenta nuevamente."
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing,
                        colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                    ) {
                        Text(if (isCapturing) "Capturando..." else "Tomar Foto")
                    }
                }
            }
        }
    }
}
