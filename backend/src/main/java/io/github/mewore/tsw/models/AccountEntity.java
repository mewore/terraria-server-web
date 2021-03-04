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

import org.springframework.lang.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.With;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@Getter
@With
@Entity
@Table(name = "account")
public class AccountEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false, unique = true)
    private final @NonNull @NotBlank String username = "";

    @JsonIgnore
    @Builder.Default
    @Column(nullable = false)
    private final byte[] password = new byte[0];

    @Builder.Default
    @Column(nullable = false)
    private final byte[] session = new byte[0];

    @Builder.Default
    @Column(nullable = false)
    private final @NonNull Instant sessionExpiration = Instant.now();

    @Builder.Default
    @ManyToOne
    @Nullable
    private final AccountTypeEntity type = null;
}
