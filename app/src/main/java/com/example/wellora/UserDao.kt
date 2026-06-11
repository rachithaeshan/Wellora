import androidx.room.*

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val password: String
)

@Dao
interface UserDao {

    @Insert
    suspend fun insertUserProfile(user: User) // insert user

    @Query("SELECT * FROM User WHERE email = :email AND password = :password")
    suspend fun getUserByEmailAndPassword(email: String, password: String): User? // login check
}
