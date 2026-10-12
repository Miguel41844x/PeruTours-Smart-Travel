package com.example.perutours.ui.screens.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationServiceItem
import com.example.perutours.data.model.QuotationValidity
import com.example.perutours.data.repository.QuotationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.data.repository.ServiceCatalogRepository
import com.example.perutours.data.repository.TravelRequestDataSource

class QuotationViewModel(
    private val repository: QuotationRepository,
    private val catalogRepository: ServiceCatalogRepository,
    private val travelRequestRepository: TravelRequestDataSource,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(QuotationFormState())
    val formState: StateFlow<QuotationFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<QuotationUiState>(QuotationUiState.Idle)
    val uiState: StateFlow<QuotationUiState> = _uiState.asStateFlow()

    private val _catalogServices =
        MutableStateFlow<List<CatalogService>>(emptyList())

    val catalogServices: StateFlow<List<CatalogService>> =
        _catalogServices.asStateFlow()

    private val _catalogLoading =
        MutableStateFlow(false)

    val catalogLoading: StateFlow<Boolean> =
        _catalogLoading.asStateFlow()


    fun loadCatalogServices() {
        viewModelScope.launch {
            _catalogLoading.value = true
            try {
                // Para actualizar/subir los servicios a Firestore
                catalogRepository.syncCatalog()

                _catalogServices.value = catalogRepository.getServices()
            } catch (_: Exception) {
                _uiState.value =
                    QuotationUiState.Error(
                        "No pudimos cargar el catálogo de servicios."
                    )
            } finally {
                _catalogLoading.value = false
            }
        }
    }

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
                        travelerCount = request.travelerCount
                    )
                }

                // Ya cargamos correctamente la solicitud.
                _uiState.value = QuotationUiState.Idle

            } catch (_: Exception) {

                _uiState.value =
                    QuotationUiState.Error("No pudimos cargar los datos de la solicitud.")
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
            } catch (_: Exception) {
                _uiState.value = QuotationUiState.Error("No pudimos cargar la cotización.")
            }
        }
    }

    fun loadQuotationForRevision(quotationId: String) {
        if (quotationId.isBlank()) {
            _uiState.value = QuotationUiState.Error("No se recibió el ID de la cotización.")
            return
        }

        viewModelScope.launch {
            _uiState.value = QuotationUiState.Loading
            try {
                val quotation = repository.getQuotationById(quotationId)
                    ?: throw IllegalStateException("No se encontró la cotización.")
                if (quotation.status != Quotation.STATUS_OBSERVED) {
                    throw IllegalStateException("La cotización no tiene cambios pendientes.")
                }

                _formState.value = QuotationFormState(
                    quotationId = quotation.id,
                    requestId = quotation.requestId,
                    clientId = quotation.clientId,
                    clientName = quotation.clientName,
                    clientEmail = quotation.clientEmail,
                    destination = quotation.destination,
                    travelerCount = quotation.travelerCount,
                    selectedServices = quotation.services,
                    marginPercent = quotation.marginPercent,
                    validityDays = DEFAULT_REVISION_VALIDITY_DAYS,
                    conditions = quotation.conditions
                )
                _uiState.value = QuotationUiState.Idle
            } catch (exception: IllegalStateException) {
                _uiState.value = QuotationUiState.Error(
                    exception.message ?: "No pudimos cargar la cotización."
                )
            } catch (_: Exception) {
                _uiState.value = QuotationUiState.Error(
                    "No pudimos cargar la cotización para revisión."
                )
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
        _formState.update {
            it.copy(conditions = text.take(QuotationValidator.MAX_CONDITIONS_LENGTH))
        }
    }

    fun saveQuotation() {
        val form = _formState.value

        val validationError = listOfNotNull(
            QuotationValidator.validateDestination(form.destination),
            QuotationValidator.validateClientName(form.clientName),
            QuotationValidator.validateServices(form.selectedServices),
            QuotationValidator.validateMargin(form.marginPercent),
            QuotationValidator.validateValidityDays(form.validityDays),
            QuotationValidator.validateConditions(form.conditions)
        ).firstOrNull()
        if (validationError != null) {
            _uiState.value = QuotationUiState.Error(validationError)
            return
        }

        val baseCost = form.selectedServices.sumOf { it.unitCost * it.quantity }
        val total = Math.round(baseCost * (1.0 + form.marginPercent / 100.0) * 100.0) / 100.0

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, form.validityDays)
        val validityStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val quotation = Quotation(
            id = form.quotationId,
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
            validUntilMillis = QuotationValidity
                .validUntilExclusiveMillis(validityStr)
                ?: 0L,
            conditions = form.conditions,
            status = Quotation.STATUS_QUOTED // 'Cotizado'
        )

        viewModelScope.launch {
            _uiState.value = QuotationUiState.Loading
            try {
                val saved = if (form.quotationId.isBlank()) {
                    repository.saveQuotation(quotation)
                } else {
                    repository.reviseObservedQuotation(quotation)
                }
                _uiState.value = QuotationUiState.Success(saved)
            } catch (_: Exception) {
                _uiState.value = QuotationUiState.Error(
                    "No pudimos guardar la cotización. Inténtalo nuevamente."
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_REVISION_VALIDITY_DAYS = 5
    }
}
