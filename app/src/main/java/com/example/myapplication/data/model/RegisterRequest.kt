
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val avatar: String,
    val hobbies: List<String> = emptyList(), // Added hobbies field
    val about: String = "",
    val gender: String,
    val genderInterest: String,
    val occupation: String = "",
)
