package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.DynamicUpdate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Setter
@Entity
@Table(name = "account")
@DynamicUpdate
public class AccountEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private @NonNull @NotBlank String username;

    @JsonIgnore
    @Column(nullable = false)
    private byte[] password;

    @Column(nullable = false)
    private byte[] session;

    @Builder.Default
    @Column(nullable = false)
    private @NonNull Instant sessionExpiration = Instant.now();

    @ManyToOne
    private @Nullable AccountTypeEntity type;
}
