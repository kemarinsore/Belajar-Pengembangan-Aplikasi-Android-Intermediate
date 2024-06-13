package com.dicoding.picodiploma.loginwithanimation.response

import com.google.gson.annotations.SerializedName


data class AddStoryResponse(
    @field:SerializedName("error")
    val error: Boolean? = null,

    @field:SerializedName("massage")
    val message: String? = null
)
