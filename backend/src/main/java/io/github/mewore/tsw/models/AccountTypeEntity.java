package io.github.mewore.tsw.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@With
@Entity
@Table(name = "account_type")
public class AccountTypeEntity {

    @Builder.Default
    @Id
    @GeneratedValue
    private final Long id = null;

    @Builder.Default
    @Column(nullable = false)
    private final boolean ableToManageAccounts = false;

    @Builder.Default
    @Column(nullable = false)
    private final boolean ableToManageHosts = false;

    @Builder.Default
    @Column(nullable = false)
    private final boolean ableToManageTerraria = false;
}
