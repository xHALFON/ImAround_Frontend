import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("id")
    val userId: String,
    val username: String,
    val email: String,

    @SerializedName("accessToken")
    val token: String,
    val refreshToken: String
)
