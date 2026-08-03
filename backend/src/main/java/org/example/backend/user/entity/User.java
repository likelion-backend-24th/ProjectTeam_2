package org.example.backend.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.auth.entity.AccountStatus;
import org.example.backend.auth.entity.Role;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class) // 이건 자동으로 시간 들어가게 설정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 100)
    private String name;

    //email
    @Column(nullable = false,length = 50, unique = true)
    private String username;

    @Column(length = 255, nullable = true) //소셜 전용 가입자는 null이라 일단 null 허용
    private String password;

    @Column(nullable = false,length = 50,unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'ACTIVE'")
    private AccountStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_subscribed", nullable = false)
    @ColumnDefault("false")
    private boolean subscribed;

    // 나중에 필요시 사용할 예정
//    @LastModifiedDate
//    @Column(name = "updated_at",nullable = false)
//    private LocalDateTime updatedAt;
}