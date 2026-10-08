package com.example.perutours.ui.screens.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationServiceItem
import com.example.perutours.data.repository.QuotationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.data.repository.TravelRequestRepository

class QuotationViewModel(
    private val repository: QuotationRepository = QuotationRepository(),
    private val travelRequestRepository: TravelRequestRepository = TravelRequestRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _formState = MutableStateFlow(QuotationFormState())
    val formState: StateFlow<QuotationFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<QuotationUiState>(QuotationUiState.Idle)
    val uiState: StateFlow<QuotationUiState> = _uiState.asStateFlow()


    fun loadRequestForQuotation(requestId: String) {
        if (requestId.isBlank()) {
            _uiState.value =
                QuotationUiState.Error("No se recibió el ID de la solicitud.")
            return
        }

        viewModelScope.launch {
            _uiState.value = QuotationUiState.Loading

            try {
                // 1. Obtener la solicitud
                val request =
                    travelRequestRepository.getRequestById(requestId)
                        ?: throw IllegalStateException(
                            "No se encontró la solicitud de viaje."
                        )

                // 2. Obtener el perfil del cliente
                val clientProfile =
                    profileRepository.getProfile(request.userId)

                // 3. Cargar los datos reales en el formulario
                _formState.update { current ->
                    current.copy(
                        requestId = request.id,
                        clientId = request.userId,
                        clientName = clientProfile.name,
                        clientEmail = clientProfile.email,
                        destination = request.destination,
                        travelerCount = request.travelerCount,
                        clientFcmToken = ""
                    )
                }

                // Ya cargamos correctamente la solicitud.
                _uiState.value = QuotationUiState.Idle

            } catch (e: Exception) {

                _uiState.value =
                    QuotationUiState.Error(
                        e.localizedMessage
                            ?: "No se pudo cargar la solicitud."
                    )
            }
        }
    }

    fun loadQuotationById(quotationId: String) {
        if (quotationId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = QuotationUiState.Loading
            try {
                val quotation = repository.getQuotationById(quotationId)
                if (quotation != null) {
                    _uiState.value = QuotationUiState.Success(quotation)
                } else {
                    _uiState.value = QuotationUiState.Error("No se encontró la cotización")
                }
            } catch (e: Exception) {
                _uiState.value = QuotationUiState.Error(e.localizedMessage ?: "Error al cargar la cotización")
            }
        }
    }

    fun addServiceFromCatalog(catalog: CatalogService) {
        val newItem = QuotationServiceItem(
            id = catalog.id,
            name = catalog.name,
            category = catalog.category,
            unitCost = catalog.unitCost,
            quantity = _formState.value.travelerCount.coerceAtLeast(1)
        )
        _formState.update { current ->
            val exists = current.selectedServices.any { it.id == catalog.id }
            if (exists) {
                current.copy(selectedServices = current.selectedServices.filter { it.id != catalog.id })
            } else {
                current.copy(selectedServices = current.selectedServices + newItem)
            }
        }
    }

    fun addCustomService(name: String, category: String, cost: Double, quantity: Int = 1) {
        val custom = QuotationServiceItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            category = category,
            unitCost = cost,
            quantity = quantity
        )
        _formState.update { it.copy(selectedServices = it.selectedServices + custom) }
    }

    fun updateServiceQuantity(serviceId: String, delta: Int) {
        _formState.update { current ->
            val updated = current.selectedServices.map { item ->
                if (item.id == serviceId) {
                    val newQty = (item.quantity + delta).coerceAtLeast(1)
                    item.copy(quantity = newQty, subtotal = item.unitCost * newQty)
                } else item
            }
            current.copy(selectedServices = updated)
        }
    }

    fun removeService(serviceId: String) {
        _formState.update { current ->
            current.copy(selectedServices = current.selectedServices.filter { it.id != serviceId })
        }
    }

    fun setMarginPercent(percent: Int) {
        _formState.update { it.copy(marginPercent = percent) }
    }

    fun setValidityDays(days: Int) {
        _formState.update { it.copy(validityDays = days) }
    }

    fun setConditions(text: String) {
        _formState.update { it.copy(conditions = text) }
    }

    fun saveQuotation() {
        val form = _formState.value

        val servicesError = QuotationValidator.validateServices(form.selectedServices)
        if (servicesError != null) {
            _uiState.value = QuotationUiState.Error(servicesError)
            return
        }

        val baseCost = form.selectedServices.sumOf { it.unitCost * it.quantity }
        val total = Math.round(baseCost * (1.0 + form.marginPercent / 100.0) * 100.0) / 100.0

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, form.validityDays)
        val validityStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val quotation = Quotation(
            requestId = form.requestId,
            clientId = form.clientId,
            clientName = form.clientName,
            clientEmail = form.clientEmail,
            destination = form.destination,
            travelerCount = form.travelerCount,
            services = form.selectedServices,
            baseCost = baseCost,
            marginPercent = form.marginPercent,
            totalAmount = total,
            validityDate = validityStr,
            conditions = form.conditions,
            status = Quotation.STATUS_QUOTED, // 'Cotizado'
            clientFcmToken = form.clientFcmToken
        )

        viewModelScope.launch {
            _uiState.value = QuotationUiState.Loading
            try {
                val saved = repository.saveQuotation(quotation)
                _uiState.value = QuotationUiState.Success(saved)
            } catch (e: Exception) {
                _uiState.value = QuotationUiState.Error(e.localizedMessage ?: "Error al guardar en Firestore")
            }
        }
    }
}