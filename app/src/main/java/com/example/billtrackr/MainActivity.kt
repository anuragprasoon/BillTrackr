package com.example.billtrackr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.ui.Alignment
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.rememberAsyncImagePainter









class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("API_KEY_TEST", BuildConfig.GEMINI_API_KEY)
        setContent {
            val navController = rememberNavController()
            val billViewModel: BillViewData = viewModel()
            var imagePath by remember { mutableStateOf<String?>(null) }

            NavHost(navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    HomeScreen(navController=navController,viewModel = billViewModel)

                }
                composable(Screen.Addbill.route) {
                    AddBillScreen(navController=navController,viewModel = billViewModel, imagePath = imagePath)
                }
                composable(Screen.Camera.route) {
                    CameraScreen(navController=navController, onImageCaptured = { path ->
                        imagePath = path
                    })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController,viewModel: BillViewData) {

    val bills by viewModel.bills.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "My Bills",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 8.dp)
        ) {
            items(bills) { bill ->
                BillItem(bill)
            }
        }

        Button(
            onClick = {
                navController.navigate(Screen.Addbill.route)
                      },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Bill")
        }
    }
}

@Composable
fun BillItem(bill: Bill) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    text = bill.merchant,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = bill.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "₹${bill.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddBillScreen(
    navController: NavController,
    viewModel: BillViewData,
    imagePath: String?
) {
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(imagePath) {
        if (imagePath == null) return@LaunchedEffect

        isExtracting = true
        Log.d("OCR", "Started")

        try {
            val base64 = imageToBase64(File(imagePath))
            val response = GeminiApi.extractBillTextSuspend(base64)

            val aiText = extractGeminiText(response)
            val parsed = parseBillFields(aiText)

            merchant = parsed.merchant
            amount = parsed.amount
            date = parsed.date

            Log.d("OCR", "Success")
        } catch (e: Exception) {
            Log.e("OCR", "Failed", e)
        } finally {
            isExtracting = false
            Log.d("OCR", "Ended")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            imagePath?.let { path ->
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = rememberAsyncImagePainter(File(path)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Text("Add Bill", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(Screen.Camera.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Bill")
            }

            Button(
                enabled = !isExtracting,
                onClick = {
                    if (merchant.isNotBlank() && amount.isNotBlank() && date.isNotBlank()) {
                        viewModel.addBills(
                            Bill(
                                merchant = merchant,
                                amount = amount.toInt(),
                                date = date
                            )
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Bill")
            }
        }

        if (isExtracting) {
            LoadingOverlay()
        }
    }
}


@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Reading bill using AI…",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun CameraScreen(
    navController: NavController,
    onImageCaptured: (String) -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isCameraReady by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    isCameraReady = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )

    // 📸 Capture button
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = {
                if (!isCameraReady) return@Button

                val photoFile = File(
                    context.cacheDir,
                    "bill_${System.currentTimeMillis()}.jpg"
                )

                val outputOptions =
                    ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(
                            outputFileResults: ImageCapture.OutputFileResults
                        ) {
                            Log.i("TAG", "File saved: ${photoFile.absolutePath}")
                            onImageCaptured(photoFile.absolutePath)
                            navController.popBackStack()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("Image capture failed", exception.toString())
                            exception.printStackTrace()
                        }
                    }
                )
            },
            enabled = isCameraReady,   // 👈 IMPORTANT
            modifier = Modifier.padding(24.dp)
        ) {
            Text(if (isCameraReady) "Capture" else "Opening Camera…")
        }

    }
}
