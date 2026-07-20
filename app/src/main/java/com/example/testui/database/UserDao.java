package com.example.testui.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertUser(UserEntity user);

    @Update
    int updateUser(UserEntity user);

    @Query("DELETE FROM users WHERE email = :email")
    void deleteUserByEmail(String email);
}
