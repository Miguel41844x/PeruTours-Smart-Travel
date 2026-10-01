package com.example.perutours.data.remote

import retrofit2.http.GET

interface DestinationsApiService {
    // Consume la API externa pública de destinos de PeruTours
    @GET("perutours-destinations.json")
    suspend fun getDestinations(): List<DestinationDto>
}
