package com.example.buscaya.data.remote

import com.example.buscaya.data.remote.dto.HybridSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BuscaYaApi {
    @GET("api/hybrid-search-android")
    suspend fun searchBusinesses(
        @Query("q") query: String
    ): HybridSearchResponseDto
}
