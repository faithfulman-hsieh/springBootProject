package com.taskmanager.account.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★★★ [Fix] 統一使用 username，解決 JPA 找不到 name 屬性的錯誤 ★★★
    @Column(unique = true, nullable = false)
    private String username;

    private String email;
    private String password;

    // ★★★ [FCM] 確保此欄位存在 ★★★
    @Column(name = "fcm_token")
    private String fcmToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "map_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}