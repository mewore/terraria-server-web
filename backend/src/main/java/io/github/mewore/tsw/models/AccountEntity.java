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

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.With;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter
@With
@Entity
@Table(name = "account")
@DynamicUpdate
public class AccountEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Column(nullable = false, unique = true)
    private @NonNull @NotBlank String username;

    @JsonIgnore
    @Column(nullable = false)
    private byte[] password;

    @Column(nullable = false)
    private byte[] session;

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull Instant sessionExpiration = Instant.now();

    @Builder.Default
    @ManyToOne
    @Nullable
    private final AccountTypeEntity type = null;
}
