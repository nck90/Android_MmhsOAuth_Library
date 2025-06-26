package kr.mmhs.oauth

import com.google.gson.annotations.SerializedName

data class MirimUser(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("nickname")
    val nickname: String? = null,
    
    @SerializedName("major")
    val major: String? = null,
    
    @SerializedName("isGraduated")
    val isGraduated: Boolean? = null,
    
    @SerializedName("admission")
    val admission: String? = null,
    
    @SerializedName("role")
    val role: String? = null,
    
    @SerializedName("generation")
    val generation: Int? = null
) 